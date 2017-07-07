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
package org.sleepydragon.util.android.log

import android.util.Log
import org.sleepydragon.util.log.LogEmitter
import org.sleepydragon.util.log.LogLevel

/**
 * An implementation of [LogEmitter] that writes log messages to Android's logcat using [Log]
 */
class LogcatLogEmitter(val tag: String) : LogEmitter {

    override fun emit(level: LogLevel, message: String?, exception: Throwable?) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, exception)
            LogLevel.DEBUG -> Log.d(tag, message, exception)
            LogLevel.INFO -> Log.i(tag, message, exception)
            LogLevel.WARNING -> Log.w(tag, message, exception)
            LogLevel.ERROR -> Log.e(tag, message, exception)
        }
    }

}
