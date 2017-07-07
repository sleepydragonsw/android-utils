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

import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito
import org.sleepydragon.util.log.LoggerConfig.FormatErrorAction
import java.util.Date
import java.util.IllegalFormatException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LoggerTest {

    @Test
    fun test_name() {
        run {
            val config = LoggerConfig(LogLevel.INFO, listOf(NullLogEmitter()), FormatErrorAction.THROW_EXCEPTION)
            val logger = Logger("abc", config)
            assertEquals("abc", logger.name)
        }
    }

    @Test
    fun test_level() {
        run {
            val config = LoggerConfig(LogLevel.INFO, listOf(NullLogEmitter()), FormatErrorAction.THROW_EXCEPTION)
            val logger = Logger("abc", config)
            assertEquals(LogLevel.INFO, logger.level)
            config.level = LogLevel.DEBUG
            assertEquals(LogLevel.DEBUG, logger.level)
        }
    }

    @Test
    fun test_emitters() {
        run {
            val emitter1 = NullLogEmitter()
            val emitter2 = NullLogEmitter()
            val config = LoggerConfig(LogLevel.INFO, listOf(emitter1), FormatErrorAction.THROW_EXCEPTION)
            val logger = Logger("abc", config)
            assertEquals(1, logger.emitters.size)
            assertSame(emitter1, logger.emitters[0])

            config.emitters = listOf(emitter2)
            assertEquals(1, logger.emitters.size)
            assertSame(emitter2, logger.emitters[0])

            config.emitters = listOf(emitter1, emitter2)
            assertEquals(2, logger.emitters.size)
            assertSame(emitter1, logger.emitters[0])
            assertSame(emitter2, logger.emitters[1])
        }
    }

    @Test
    fun test_log_LogLevelRespected() {
        fun logMessages(loggerLevel: LogLevel): LogEmitter {
            val emitter = Mockito.mock(LogEmitter::class.java)
            val config = LoggerConfig(loggerLevel, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
            Logger("abc", config).apply {
                log(LogLevel.VERBOSE, "111")
                log(LogLevel.DEBUG, "222")
                log(LogLevel.INFO, "333")
                log(LogLevel.WARNING, "444")
                log(LogLevel.ERROR, "555")
            }
            return emitter
        }

        logMessages(LogLevel.VERBOSE).let { emitter ->
            Mockito.verify(emitter).emit(LogLevel.VERBOSE, "abc: 111", null)
            Mockito.verify(emitter).emit(LogLevel.DEBUG, "abc: 222", null)
            Mockito.verify(emitter).emit(LogLevel.INFO, "abc: 333", null)
            Mockito.verify(emitter).emit(LogLevel.WARNING, "abc: WARNING: 444", null)
            Mockito.verify(emitter).emit(LogLevel.ERROR, "abc: ERROR: 555", null)
            Mockito.verifyNoMoreInteractions(emitter)
        }
        logMessages(LogLevel.DEBUG).let { emitter ->
            Mockito.verify(emitter).emit(LogLevel.DEBUG, "abc: 222", null)
            Mockito.verify(emitter).emit(LogLevel.INFO, "abc: 333", null)
            Mockito.verify(emitter).emit(LogLevel.WARNING, "abc: WARNING: 444", null)
            Mockito.verify(emitter).emit(LogLevel.ERROR, "abc: ERROR: 555", null)
            Mockito.verifyNoMoreInteractions(emitter)
        }
        logMessages(LogLevel.INFO).let { emitter ->
            Mockito.verify(emitter).emit(LogLevel.INFO, "abc: 333", null)
            Mockito.verify(emitter).emit(LogLevel.WARNING, "abc: WARNING: 444", null)
            Mockito.verify(emitter).emit(LogLevel.ERROR, "abc: ERROR: 555", null)
            Mockito.verifyNoMoreInteractions(emitter)
        }
        logMessages(LogLevel.WARNING).let { emitter ->
            Mockito.verify(emitter).emit(LogLevel.WARNING, "abc: WARNING: 444", null)
            Mockito.verify(emitter).emit(LogLevel.ERROR, "abc: ERROR: 555", null)
            Mockito.verifyNoMoreInteractions(emitter)
        }
        logMessages(LogLevel.ERROR).let { emitter ->
            Mockito.verify(emitter).emit(LogLevel.ERROR, "abc: ERROR: 555", null)
            Mockito.verifyNoMoreInteractions(emitter)
        }
    }

    @Test
    fun test_log_WithException() {
        val emitter = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(LogLevel.VERBOSE, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
        val exception = RuntimeException("forced exception")
        Logger("exc", config).apply {
            log(LogLevel.INFO, null, "hello1")
            log(LogLevel.DEBUG, exception, "hello2")
        }
        Mockito.verify(emitter).emit(LogLevel.INFO, "exc: hello1", null)
        Mockito.verify(emitter).emit(LogLevel.DEBUG, "exc: hello2", exception)
        Mockito.verifyNoMoreInteractions(emitter)
    }

    @Test
    fun test_log_FormatStrings() {
        val emitter = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(LogLevel.VERBOSE, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
        val exception = RuntimeException("forced exception")
        val date = Date(0)
        Logger("fmt", config).apply {
            log(LogLevel.VERBOSE, "hello %s", "world")
            log(LogLevel.DEBUG, "hello %d", 123)
            log(LogLevel.INFO, "%s %d", "hello", null)
            log(LogLevel.WARNING, "%tB %s %d", date, "hello", 123)
            log(LogLevel.ERROR, exception, "%tA", date, 1, 2, 3)
        }
        Mockito.verify(emitter).emit(LogLevel.VERBOSE, "fmt: hello world", null)
        Mockito.verify(emitter).emit(LogLevel.DEBUG, "fmt: hello 123", null)
        Mockito.verify(emitter).emit(LogLevel.INFO, "fmt: hello null", null)
        Mockito.verify(emitter).emit(LogLevel.WARNING, "fmt: WARNING: December hello 123", null)
        Mockito.verify(emitter).emit(LogLevel.ERROR, "fmt: ERROR: Wednesday", exception)
        Mockito.verifyNoMoreInteractions(emitter)
    }

    @Test
    fun test_log_FormatStrings_FormatErrorAction_ThrowException() {
        val emitter = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(LogLevel.VERBOSE, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
        val logger = Logger("fte", config)
        assertFailsWith(IllegalFormatException::class) {
            logger.log(LogLevel.INFO, "%s")
        }
        assertFailsWith(IllegalFormatException::class) {
            logger.log(LogLevel.INFO, "%d", "abc")
        }
    }

    @Test
    fun test_log_FormatStrings_FormatErrorAction_AppendAsString() {
        fun test(expected: String, message: String, vararg args: Any?) {
            val emitter = Mockito.mock(LogEmitter::class.java)
            val config = LoggerConfig(LogLevel.VERBOSE, listOf(emitter), FormatErrorAction.APPEND_AS_STRING)
            val logger = Logger("fas", config)
            logger.log(LogLevel.INFO, message, *args)
            Mockito.verify(emitter, Mockito.only()).emit(LogLevel.INFO, "fas: $expected", null)
        }

        val toStringThrows = ToStringThrows()

        test("%s", "%s")
        test("%d (abc)", "%d", "abc")
        test("%d %d (abc, def)", "%d %d", "abc", "def")
        test("Hello %s World (${toStringThrows.exception})", "Hello %s World", toStringThrows)
    }

    @Test
    fun test_log_LogsToEachEmitter() {
        val emitter1 = Mockito.mock(LogEmitter::class.java)
        val emitter2 = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(LogLevel.VERBOSE, listOf(emitter1, emitter2), FormatErrorAction.THROW_EXCEPTION)
        val logger = Logger("lte", config)
        val exception = RuntimeException()

        logger.log(LogLevel.INFO, "info")
        logger.log(LogLevel.WARNING, exception, "warn")
        Mockito.verify(emitter1).emit(LogLevel.INFO, "lte: info", null)
        Mockito.verify(emitter1).emit(LogLevel.WARNING, "lte: WARNING: warn", exception)
        Mockito.verifyNoMoreInteractions(emitter1)
        Mockito.verify(emitter2).emit(LogLevel.INFO, "lte: info", null)
        Mockito.verify(emitter2).emit(LogLevel.WARNING, "lte: WARNING: warn", exception)
        Mockito.verifyNoMoreInteractions(emitter2)
    }

    private fun testLogMethodNoException(level: LogLevel, prefix: String, f: Logger.(String, Array<out Any?>) -> Unit) {
        val emitter = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(level, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
        val logger = Logger("a", config)
        logger.f("hello %d %d %d", arrayOf(1, 2, 3))
        Mockito.verify(emitter).emit(level, "a: ${prefix}hello 1 2 3", null)
        Mockito.verifyNoMoreInteractions(emitter)
    }

    private fun testLogMethodWithException(level: LogLevel, prefix: String,
            f: Logger.(Throwable, String, Array<out Any?>) -> Unit) {
        val emitter = Mockito.mock(LogEmitter::class.java)
        val config = LoggerConfig(level, listOf(emitter), FormatErrorAction.THROW_EXCEPTION)
        val logger = Logger("a", config)
        val exception = RuntimeException()
        logger.f(exception, "hello %d %d %d", arrayOf(1, 2, 3))
        Mockito.verify(emitter).emit(level, "a: ${prefix}hello 1 2 3", exception)
        Mockito.verifyNoMoreInteractions(emitter)
    }

    @Test
    fun test_v() {
        testLogMethodNoException(LogLevel.VERBOSE, "", Logger::v)
        testLogMethodWithException(LogLevel.VERBOSE, "", Logger::v)
    }

    @Test
    fun test_d() {
        testLogMethodNoException(LogLevel.DEBUG, "", Logger::d)
        testLogMethodWithException(LogLevel.DEBUG, "", Logger::d)
    }

    @Test
    fun test_i() {
        testLogMethodNoException(LogLevel.INFO, "", Logger::i)
        testLogMethodWithException(LogLevel.INFO, "", Logger::i)
    }

    @Test
    fun test_w() {
        testLogMethodNoException(LogLevel.WARNING, "WARNING: ", Logger::w)
        testLogMethodWithException(LogLevel.WARNING, "WARNING: ", Logger::w)
    }

    @Test
    fun test_e() {
        testLogMethodNoException(LogLevel.ERROR, "ERROR: ", Logger::e)
        testLogMethodWithException(LogLevel.ERROR, "ERROR: ", Logger::e)
    }

}

private class NullLogEmitter : LogEmitter {

    override fun emit(level: LogLevel, message: String?, exception: Throwable?) {
    }

}

private class ToStringThrows {

    val exception = RuntimeException("forced exception")

    override fun toString(): String {
        throw exception
    }

}
