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
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROOT_BINARY_NAME = "libproot.so"

/** 写入 app.log 的标签。 */
private const val TAG = "PRoot"

sealed interface LocalTerminalState {
    data object Starting : LocalTerminalState
    data object Running : LocalTerminalState
    data object Stopped : LocalTerminalState
    data class Failed(val message: String) : LocalTerminalState
}

interface LocalTerminalListener {
    fun onOutput(text: String)

    fun onError(text: String)

    fun onStateChanged(state: LocalTerminalState)
}

/**
 * 本地 PRoot 终端管理器。
 *
 * 通过 [ProcessBuilder] 启动 `nativeLibraryDir/libproot.so`，[sendInput] 会补换行，
 * [sendRawInput] 则原样写入（供悬浮球的 Ctrl/Esc/Tab/方向键使用）。
 */
class LocalTerminalManager : TerminalInputTarget {
    @Volatile
    var listener: LocalTerminalListener? = null

    @Volatile
    private var process: Process? = null

    @Volatile
    private var running = false

    @Volatile
    private var closed = false

    suspend fun startProot(context: Context, distroId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (closed) {
                return@withContext Result.failure(
                    IllegalStateException("Terminal is closed"),
                )
            }
            if (running) {
                return@withContext Result.failure(
                    IllegalStateException("PRoot is already running"),
                )
            }

            notifyState(LocalTerminalState.Starting)

            try {
                val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
                    ?: throw IOException("Native library directory is unavailable")
                val prootFile = File(nativeLibraryDir, PROOT_BINARY_NAME)
                if (!prootFile.isFile) {
                    throw IOException("PRoot binary is missing: ${prootFile.absolutePath}")
                }

                val rootfsDir = File(context.filesDir, "instances/$distroId/rootfs")
                if (!rootfsDir.isDirectory) {
                    throw IOException("Rootfs is not installed: ${rootfsDir.absolutePath}")
                }

                if (closed) {
                    return@withContext Result.failure(
                        IllegalStateException("Terminal is closed"),
                    )
                }

                val shell = if (distroId == "alpine") "/bin/sh" else "/bin/bash"
                // PRoot 启动命令会写进 app.log，方便在日志页直接复现问题。
                val command = listOf(
                    prootFile.absolutePath,
                    "--link2symlink",
                    "-0",
                    "-r",
                    rootfsDir.canonicalPath,
                    "-b",
                    "/dev",
                    "-b",
                    "/proc",
                    "-b",
                    "/sys",
                    "-w",
                    "/root",
                    "/usr/bin/env",
                    "-i",
                    "HOME=/root",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                    "TERM=xterm-256color",
                    shell,
                )

                AppLogger.log(TAG, "启动 PRoot：${command.joinToString(" ")}")

                // Termux 构建的 proot 默认从硬编码的 Termux 路径查找 loader；
                // 通过环境变量显式指向 jniLibs 中提取出的 loader，确保在本应用内可用。
                val loader = File(nativeLibraryDir, "libproot_loader.so")
                val loader32 = File(nativeLibraryDir, "libproot_loader32.so")
                val newProcess = ProcessBuilder(command)
                    .directory(context.filesDir)
                    .apply {
                        val env = environment()
                        if (loader.isFile) {
                            env["PROOT_LOADER"] = loader.absolutePath
                        }
                        if (loader32.isFile) {
                            env["PROOT_LOADER_32"] = loader32.absolutePath
                        }
                    }
                    .start()
                process = newProcess
                running = true
                notifyState(LocalTerminalState.Running)

                coroutineScope {
                    launch { pump(newProcess.inputStream, isError = false) }
                    launch { pump(newProcess.errorStream, isError = true) }
                }

                val exitCode = newProcess.waitFor()
                AppLogger.log(TAG, "PRoot 退出，exitCode=$exitCode（发行版 $distroId）")
                if (process === newProcess) {
                    process = null
                }
                running = false
                if (!closed) {
                    if (exitCode != 0) {
                        notifyError("PRoot exited with code $exitCode")
                    }
                    notifyState(LocalTerminalState.Stopped)
                }
                Result.success(Unit)
            } catch (cancelled: CancellationException) {
                process?.destroy()
                process = null
                running = false
                throw cancelled
            } catch (error: Exception) {
                AppLogger.log(TAG, "PRoot 启动失败（发行版 $distroId）", error)
                process?.destroy()
                process = null
                running = false
                if (!closed) {
                    notifyState(
                        LocalTerminalState.Failed(
                            error.message ?: error.javaClass.simpleName,
                        ),
                    )
                }
                Result.failure(error)
            }
        }

    fun sendInput(input: String): Boolean {
        if (closed || !running || input.isEmpty()) {
            return false
        }

        val currentProcess = process ?: return false
        return try {
            val text = if (input.endsWith("\n")) input else "$input\n"
            currentProcess.outputStream.write(text.toByteArray(Charsets.UTF_8))
            currentProcess.outputStream.flush()
            true
        } catch (_: IOException) {
            false
        }
    }

    /**
     * 悬浮球快捷键注入：原样写入进程 stdin，不补换行，
     * 否则 Ctrl/方向键这类控制序列会被当成回车执行。
     */
    override fun sendRawInput(text: String): Boolean {
        if (closed || !running || text.isEmpty()) {
            return false
        }

        val currentProcess = process ?: return false
        return try {
            currentProcess.outputStream.write(text.toByteArray(Charsets.UTF_8))
            currentProcess.outputStream.flush()
            true
        } catch (error: IOException) {
            AppLogger.log(TAG, "写入快捷键失败", error)
            false
        }
    }

    fun stop() {
        closed = true
        running = false
        val currentProcess = process
        process = null
        currentProcess?.destroy()
        notifyState(LocalTerminalState.Stopped)
    }

    private fun pump(stream: InputStream, isError: Boolean) {
        try {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (isError) {
                        notifyError(line)
                    } else {
                        notifyOutput(line)
                    }
                }
            }
        } catch (_: IOException) {
            // PRoot 退出过程中流可能已被关闭，这里忽略即可。
        }
    }

    private fun notifyOutput(text: String) {
        listener?.onOutput(text)
    }

    private fun notifyError(text: String) {
        listener?.onError(text)
    }

    private fun notifyState(state: LocalTerminalState) {
        listener?.onStateChanged(state)
    }
}
