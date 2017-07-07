/*
 * Copyright (C) 2017 Denver Coneybeare <denver@sleepydragon.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleepydragon.util.log

import java.util.Formatter
import java.util.Locale

/**
 * A class to assist with logging application events.
 *
 * The log messages are formatted using the [Formatter] syntax and the formatted messages are ultimately emitted to
 * all [LogEmitter] objects in the [LoggerConfig] specified to this object's constructor.
 *
 * @param name the name of this logger; this string will be added to the beginning of every log message sent to
 * the log emitters
 * @param config the configuration to use for this logger; the values in this object may be changed over time and
 * the logger will retrieve the values from the config every time they are needed.  For example, the level can be
 * changed in the config at any time, affecting all future messages logged by this object.
 */
class Logger(val name: String, private val config: LoggerConfig) {

    /**
     * Get the log level threshold that is currently set in this objects [LoggerConfig].
     *
     * Any log messages whose level is lower than this level will not be logged.
     *
     * Note that this value may change over time by modifying the [LoggerConfig] object that was specified to the
     * constructor.
     */
    val level: LogLevel
        get() = config.level

    /**
     * Get the list of log emitters that are currently set in this objects [LoggerConfig]
     *
     * Each time that a message is logged it will be formatter and passed to each of these emitters.
     *
     * Note that the list of log emitters may change over time by modifying the [LoggerConfig] object that was
     * specified to the constructor.
     */
    val emitters: List<LogEmitter>
        get() = config.emitters

    /**
     * Log a message with a given [LogLevel]
     */
    fun log(level: LogLevel, message: String, vararg args: Any?) {
        _log(level, null, message, args)
    }

    /**
     * Log a message with a given [LogLevel] and an optional exception.
     */
    fun log(level: LogLevel, exception: Throwable?, message: String, vararg args: Any?) {
        _log(level, exception, message, args)
    }

    /**
     * Log a message with [LogLevel.VERBOSE] log level.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.VERBOSE].
     */
    fun v(message: String, vararg args: Any?) {
        _log(LogLevel.VERBOSE, null, message, args)
    }

    /**
     * Log a message with [LogLevel.VERBOSE] log level and an optional exception.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.VERBOSE].
     */
    fun v(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.VERBOSE, exception, message, args)
    }

    /**
     * Log a message with [LogLevel.DEBUG] log level.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.DEBUG].
     */
    fun d(message: String, vararg args: Any?) {
        _log(LogLevel.DEBUG, null, message, args)
    }

    /**
     * Log a message with [LogLevel.DEBUG] log level and an optional exception.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.DEBUG].
     */
    fun d(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.DEBUG, exception, message, args)
    }

    /**
     * Log a message with [LogLevel.INFO] log level.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.INFO].
     */
    fun i(message: String, vararg args: Any?) {
        _log(LogLevel.INFO, null, message, args)
    }

    /**
     * Log a message with [LogLevel.INFO] log level and an optional exception.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.INFO].
     */
    fun i(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.INFO, exception, message, args)
    }

    /**
     * Log a message with [LogLevel.WARNING] log level.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.WARNING].
     */
    fun w(message: String, vararg args: Any?) {
        _log(LogLevel.WARNING, null, message, args)
    }

    /**
     * Log a message with [LogLevel.WARNING] log level and an optional exception.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.WARNING].
     */
    fun w(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.WARNING, exception, message, args)
    }

    /**
     * Log a message with [LogLevel.ERROR] log level.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.ERROR].
     */
    fun e(message: String, vararg args: Any?) {
        _log(LogLevel.ERROR, null, message, args)
    }

    /**
     * Log a message with [LogLevel.ERROR] log level and an optional exception.
     *
     * This is merely a shorthand for calling [log] with level=[LogLevel.ERROR].
     */
    fun e(exception: Throwable?, message: String, vararg args: Any?) {
        _log(LogLevel.ERROR, exception, message, args)
    }

    private fun _log(level: LogLevel, exception: Throwable?, message: String, args: Array<out Any?>?) {
        if (level.ordinal < this.level.ordinal) {
            return
        }
        val formattedMessage = assembleMessage(level, message, args)
        for (emitter in config.emitters) {
            emitter.emit(level, formattedMessage, exception)
        }
    }

    private fun assembleMessage(level: LogLevel, message: String, args: Array<out Any?>?): String {
        val messageFormatter = THREAD_LOCAL_MESSAGE_FORMATTER.get()
        val sb = messageFormatter.sb
        sb.setLength(0)

        sb.append(name).append(": ")

        prefixFor(level)?.let { prefix ->
            sb.append(prefix).append(": ")
        }

        formatMessage(message, (args ?: EMPTY_ARGS), sb, messageFormatter.formatter)

        val assembledMessage = sb.toString()

        if (sb.length > 2000) {
            THREAD_LOCAL_MESSAGE_FORMATTER.remove()
        } else {
            sb.setLength(0)
        }

        return assembledMessage
    }

    private fun formatMessage(message: String, args: Array<out Any?>, sb: StringBuilder, formatter: Formatter) {
        when (config.formatErrorAction) {
            LoggerConfig.FormatErrorAction.THROW_EXCEPTION -> {
                formatter.format(message, *args)
            }
            LoggerConfig.FormatErrorAction.APPEND_AS_STRING -> {
                val lengthBefore = sb.length
                try {
                    formatter.format(message, *args)
                } catch (e: Exception) {
                    sb.setLength(lengthBefore)
                    sb.append(message)
                    if (args.isNotEmpty()) {
                        sb.append(" (")
                        for (i in 0..args.size - 1) {
                            if (i > 0) {
                                sb.append(", ")
                            }
                            val arg = args[i]
                            try {
                                sb.append(arg)
                            } catch (e: Exception) {
                                sb.append(e)
                            }
                        }
                        sb.append(')')
                    }
                }
            }
        }
    }

    private fun prefixFor(level: LogLevel) = when (level) {
        LogLevel.WARNING -> "WARNING"
        LogLevel.ERROR -> "ERROR"
        else -> null
    }

}

private val EMPTY_ARGS = emptyArray<Any?>()

private class MessageFormatter {
    val sb = StringBuilder(100)
    val formatter = Formatter(sb, Locale.US)
}

private val THREAD_LOCAL_MESSAGE_FORMATTER = object : ThreadLocal<MessageFormatter>() {

    override fun initialValue(): MessageFormatter {
        return MessageFormatter()
    }

}
