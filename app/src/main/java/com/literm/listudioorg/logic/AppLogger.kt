/*
 * Copyright 2026 li63050a <li63050@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.literm.listudioorg.logic

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用内日志：写入 `files/logs/app.log`，供日志页展示与分享。
 *
 * 单文件上限 1MB，超出后清空重写；所有磁盘操作都在 [Dispatchers.IO] 或锁内完成，
 * 写失败只忽略，绝不影响终端主流程。
 */
object AppLogger {
    private const val LOG_DIRECTORY = "logs"
    private const val LOG_FILE_NAME = "app.log"
    private const val MAX_LOG_BYTES = 1_000_000L
    private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"

    private val lock = Any()

    @Volatile
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun log(tag: String, message: String) {
        write("[$tag] $message")
    }

    fun log(tag: String, message: String, error: Throwable) {
        write("[$tag] $message\n${Log.getStackTraceString(error)}")
    }

    suspend fun readLines(): List<String> = withContext(Dispatchers.IO) {
        val file = logFileOrNull() ?: return@withContext emptyList()
        synchronized(lock) {
            if (!file.isFile) {
                emptyList()
            } else {
                runCatching {
                    file.readLines(Charsets.UTF_8)
                }.getOrDefault(emptyList())
            }
        }
    }

    suspend fun clear() {
        val file = logFileOrNull() ?: return
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                runCatching {
                    file.writeText("")
                }
            }
        }
    }

    fun logFile(context: Context): File {
        val file = File(File(context.filesDir, LOG_DIRECTORY), LOG_FILE_NAME)
        synchronized(lock) {
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                runCatching { file.createNewFile() }
            }
        }
        return file
    }

    private fun write(message: String) {
        val file = logFileOrNull() ?: return
        val timestamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US).format(Date())
        val entry = "$timestamp $message\n"

        synchronized(lock) {
            try {
                file.parentFile?.mkdirs()
                if (file.length() > MAX_LOG_BYTES) {
                    file.writeText("")
                }
                file.appendText(entry, Charsets.UTF_8)
            } catch (_: IOException) {
                // 日志写入失败不能影响终端正常运行，直接忽略。
            }
        }
    }

    private fun logFileOrNull(): File? =
        applicationContext?.let { context ->
            File(File(context.filesDir, LOG_DIRECTORY), LOG_FILE_NAME)
        }
}
