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
import android.os.Build
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object DownloadHelper {
    private const val ARCHIVE_NAME = "rootfs.tar.gz"

    private const val BUFFER_SIZE = 64 * 1024
    private const val PROGRESS_STEP = 0.01f
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val USER_AGENT = "LiTerm/0.1.0"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    suspend fun download(
        context: Context,
        distro: LinuxDistro,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val architecture = currentArchitecture()
        val url = distro.resolveUrl(architecture)
        val instanceDir = File(context.filesDir, "instances/${distro.id}")
        if (!instanceDir.exists() && !instanceDir.mkdirs()) {
            throw IOException("Unable to create ${instanceDir.absolutePath}")
        }

        val archiveFile = File(instanceDir, ARCHIVE_NAME)
        try {
            downloadToFile(
                url = url,
                targetFile = archiveFile,
                onProgress = onProgress,
            )
            archiveFile
        } catch (throwable: Throwable) {
            runCatching { archiveFile.delete() }
            throw throwable
        }
    }

    private suspend fun downloadToFile(
        url: String,
        targetFile: File,
        onProgress: (Float) -> Unit,
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()

        onProgress(0f)
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed with HTTP ${response.code}")
            }

            val body = response.body
                ?: throw IOException("Download returned an empty body")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                targetFile.outputStream().buffered(BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloadedBytes = 0L
                    var lastReportedProgress = -1f

                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (totalBytes > 0L) {
                            val progress = (downloadedBytes.toDouble() / totalBytes.toDouble())
                                .toFloat()
                                .coerceIn(0f, 0.99f)
                            if (progress - lastReportedProgress >= PROGRESS_STEP) {
                                lastReportedProgress = progress
                                onProgress(progress)
                            }
                        }
                    }

                    output.flush()
                }
            }
        }
        onProgress(1f)
    }

    private fun currentArchitecture(): LinuxArch {
        for (abi in Build.SUPPORTED_ABIS) {
            when (abi) {
                "arm64-v8a" -> return LinuxArch.Arm64
                "x86_64" -> return LinuxArch.X86_64
            }
        }

        throw IOException("Unsupported device ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun LinuxDistro.resolveUrl(architecture: LinuxArch): String {
        val arch = when (architecture) {
            LinuxArch.Arm64 -> arm64Arch
            LinuxArch.X86_64 -> x86_64Arch
        }

        return downloadUrlTemplate
            .replace("{arch}", arch)
            .replace("{version}", downloadVersion)
    }

    private enum class LinuxArch {
        Arm64,
        X86_64,
    }
}
