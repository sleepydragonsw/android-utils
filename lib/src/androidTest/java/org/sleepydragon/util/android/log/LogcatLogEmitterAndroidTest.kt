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

import android.support.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.sleepydragon.util.log.LogLevel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LogcatLogEmitterAndroidTest {

    @Test
    fun testSanity() {
        val lines = withLogcatCapture {
            LogcatLogEmitter("abc").apply {
                emit(LogLevel.ERROR, "def", null)
            }
        }
        var found = false
        for (line in lines) {
            if (line.startsWith("E/abc")) {
                found = true
                assertTrue { line.contains("def") }
            }
        }
        assertTrue(found)
    }

    @Test
    fun testLevels() {
        val vMessage = "ababab${System.nanoTime()}"
        val dMessage = "cdcdcd${System.nanoTime()}"
        val iMessage = "efefef${System.nanoTime()}"
        val wMessage = "ghghgh${System.nanoTime()}"
        val eMessage = "ijijij${System.nanoTime()}"
        val lines = withLogcatCapture {
            LogcatLogEmitter("zzyzx").apply {
                emit(LogLevel.VERBOSE, vMessage, null)
                emit(LogLevel.DEBUG, dMessage, null)
                emit(LogLevel.INFO, iMessage, null)
                emit(LogLevel.WARNING, wMessage, null)
                emit(LogLevel.ERROR, eMessage, null)
            }
        }
        val found = mutableSetOf<LogLevel>()
        for (line in lines) {
            if (line.startsWith("V/zzyzx")) {
                assertFalse { found.contains(LogLevel.VERBOSE) }
                found.add(LogLevel.VERBOSE)
                assertTrue { line.contains(vMessage) }
            } else if (line.startsWith("D/zzyzx")) {
                assertFalse { found.contains(LogLevel.DEBUG) }
                found.add(LogLevel.DEBUG)
                assertTrue { line.contains(dMessage) }
            } else if (line.startsWith("I/zzyzx")) {
                assertFalse { found.contains(LogLevel.INFO) }
                found.add(LogLevel.INFO)
                assertTrue { line.contains(iMessage) }
            } else if (line.startsWith("W/zzyzx")) {
                assertFalse { found.contains(LogLevel.WARNING) }
                found.add(LogLevel.WARNING)
                assertTrue { line.contains(wMessage) }
            } else if (line.startsWith("E/zzyzx")) {
                assertFalse { found.contains(LogLevel.ERROR) }
                found.add(LogLevel.ERROR)
                assertTrue { line.contains(eMessage) }
            }
        }
        assertTrue(found.contains(LogLevel.VERBOSE))
        assertTrue(found.contains(LogLevel.DEBUG))
        assertTrue(found.contains(LogLevel.INFO))
        assertTrue(found.contains(LogLevel.WARNING))
        assertTrue(found.contains(LogLevel.ERROR))
    }

    @Test
    fun testExceptions() {
        val vMessage = "ababab${System.nanoTime()}"
        val dMessage = "cdcdcd${System.nanoTime()}"
        val iMessage = "efefef${System.nanoTime()}"
        val wMessage = "ghghgh${System.nanoTime()}"
        val eMessage = "ijijij${System.nanoTime()}"
        val exception = try {
            throw IOException("forced exception")
        } catch (e: IOException) {
            e
        }
        val lines = withLogcatCapture {
            LogcatLogEmitter("zzyzx").apply {
                emit(LogLevel.VERBOSE, vMessage, exception)
                emit(LogLevel.DEBUG, dMessage, exception)
                emit(LogLevel.INFO, iMessage, exception)
                emit(LogLevel.WARNING, wMessage, exception)
                emit(LogLevel.ERROR, eMessage, exception)
            }
        }

        val foundMessage = mutableSetOf<LogLevel>()
        for (line in lines) {
            if (line.startsWith("V/zzyzx") && line.contains(vMessage)) {
                foundMessage.add(LogLevel.VERBOSE)
            } else if (line.startsWith("D/zzyzx") && line.contains(dMessage)) {
                foundMessage.add(LogLevel.DEBUG)
            } else if (line.startsWith("I/zzyzx") && line.contains(iMessage)) {
                foundMessage.add(LogLevel.INFO)
            } else if (line.startsWith("W/zzyzx") && line.contains(wMessage)) {
                foundMessage.add(LogLevel.WARNING)
            } else if (line.startsWith("E/zzyzx") && line.contains(eMessage)) {
                foundMessage.add(LogLevel.ERROR)
            }
        }
        assertTrue(foundMessage.contains(LogLevel.VERBOSE))
        assertTrue(foundMessage.contains(LogLevel.DEBUG))
        assertTrue(foundMessage.contains(LogLevel.INFO))
        assertTrue(foundMessage.contains(LogLevel.WARNING))
        assertTrue(foundMessage.contains(LogLevel.ERROR))

        val foundException = mutableSetOf<LogLevel>()
        for (line in lines) {
            if (line.startsWith("V/zzyzx") && line.contains(exception.toString())) {
                foundException.add(LogLevel.VERBOSE)
            } else if (line.startsWith("D/zzyzx") && line.contains(exception.toString())) {
                foundException.add(LogLevel.DEBUG)
            } else if (line.startsWith("I/zzyzx") && line.contains(exception.toString())) {
                foundException.add(LogLevel.INFO)
            } else if (line.startsWith("W/zzyzx") && line.contains(exception.toString())) {
                foundException.add(LogLevel.WARNING)
            } else if (line.startsWith("E/zzyzx") && line.contains(exception.toString())) {
                foundException.add(LogLevel.ERROR)
            }
        }
        assertTrue(foundException.contains(LogLevel.VERBOSE))
        assertTrue(foundException.contains(LogLevel.DEBUG))
        assertTrue(foundException.contains(LogLevel.INFO))
        assertTrue(foundException.contains(LogLevel.WARNING))
        assertTrue(foundException.contains(LogLevel.ERROR))
    }

    private fun withLogcatCapture(block: () -> Unit): List<String> {
        val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        block()
        val output = logcat("-v", "brief", "-b", "main", "-t", startTime)
        return output.split("\n")
    }

    private fun logcat(vararg args: String): String {
        val argList = mutableListOf("logcat").apply {
            addAll(args)
        }
        val process = ProcessBuilder(argList).run {
            redirectErrorStream(true)
            start()
        }
        val output = process.inputStream.let { src ->
            val dest = ByteArrayOutputStream()
            val buf = ByteArray(1024)
            while (true) {
                val readCount = src.read(buf)
                if (readCount < 0) {
                    break
                }
                dest.write(buf, 0, readCount)
            }
            dest.toByteArray()
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("command completed with non-zero exit code $exitCode: $argList")
        }
        return String(output)
    }

}