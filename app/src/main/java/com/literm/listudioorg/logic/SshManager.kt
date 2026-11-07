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

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SshConnectionState {
    data object Disconnected : SshConnectionState
    data object Connecting : SshConnectionState
    data object Connected : SshConnectionState
    data class Failed(val message: String) : SshConnectionState
}

interface SshListener {
    fun onOutput(text: String)

    fun onError(text: String)

    fun onStateChanged(state: SshConnectionState)
}

/**
 * SSH 连接管理器。
 *
 * 会话是 exec 模式（一条命令一个 channel），[sendRawInput] 只在命令执行期间有效，
 * 属于尽力而为的快捷键注入；同时实现 [TerminalInputTarget] 供悬浮球写入。
 */
class SshManager : TerminalInputTarget {
    @Volatile
    var listener: SshListener? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()

    @Volatile
    private var session: Session? = null

    @Volatile
    private var activeChannel: ChannelExec? = null

    @Volatile
    private var closed = false

    /** 日志里显示的连接目标，仅在连接开始后存在。 */
    @Volatile
    private var logTarget: String? = null

    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String?,
        privateKey: String?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (closed) {
            return@withContext Result.failure(IllegalStateException("Connection is closed"))
        }

        logTarget = "$username@$host:$port"
        AppLogger.log(TAG, "正在连接 $logTarget")
        notifyState(SshConnectionState.Connecting)
        var newSession: Session? = null

        try {
            val jsch = JSch()
            jsch.hostKeyRepository = TrustAllHostKeyRepository

            if (!privateKey.isNullOrBlank()) {
                jsch.addIdentity(
                    "literm-private-key",
                    privateKey.toByteArray(Charsets.UTF_8),
                    null,
                    null,
                )
            }

            newSession = jsch.getSession(username, host, port).apply {
                if (!password.isNullOrBlank()) {
                    setPassword(password)
                }
                setConfig("StrictHostKeyChecking", "no")
                timeout = CONNECT_TIMEOUT_MS
            }
            val connectedSession = checkNotNull(newSession)
            connectedSession.connect(CONNECT_TIMEOUT_MS)

            if (closed) {
                connectedSession.disconnect()
                return@withContext Result.failure(IllegalStateException("Connection is closed"))
            }

            session = connectedSession
            AppLogger.log(TAG, "连接成功 $logTarget")
            notifyState(SshConnectionState.Connected)
            Result.success(Unit)
        } catch (error: Exception) {
            newSession?.disconnect()
            AppLogger.log(TAG, "连接失败 ${logTarget ?: host}", error)
            if (!closed) {
                notifyState(
                    SshConnectionState.Failed(
                        error.message ?: error.javaClass.simpleName,
                    ),
                )
            }
            Result.failure(error)
        }
    }

    fun executeCommand(command: String): Boolean {
        val currentSession = session
        if (closed || currentSession == null || !currentSession.isConnected) {
            return false
        }

        AppLogger.log(TAG, "执行命令：$command（${logTarget ?: "unknown"}）")

        scope.launch {
            commandMutex.withLock {
                try {
                    val channel = currentSession.openChannel("exec") as ChannelExec
                    activeChannel = channel
                    channel.setCommand(command)
                    val stdout = channel.inputStream
                    val stderr = channel.errStream
                    channel.connect(CONNECT_TIMEOUT_MS)

                    coroutineScope {
                        launch { pump(stdout) { notifyOutput(it) } }
                        launch { pump(stderr) { notifyError(it) } }
                    }

                    channel.disconnect()
                    activeChannel = null
                } catch (error: Exception) {
                    AppLogger.log(TAG, "命令执行失败：$command", error)
                    activeChannel?.disconnect()
                    activeChannel = null
                    notifyError(error.message ?: "Command failed")
                }
            }
        }

        return true
    }

    /**
     * 悬浮球快捷键注入：exec 通道只有在命令运行期间才有输入流，
     * 因此这里尽力写入，失败就返回 false 让界面提示“无活动会话”。
     */
    override fun sendRawInput(text: String): Boolean {
        if (closed || text.isEmpty()) return false

        val channel = activeChannel ?: return false
        if (!channel.isConnected) return false

        return try {
            channel.outputStream.write(text.toByteArray(Charsets.UTF_8))
            channel.outputStream.flush()
            true
        } catch (error: Exception) {
            AppLogger.log(TAG, "写入快捷键失败", error)
            false
        }
    }

    fun disconnect() {
        closed = true
        AppLogger.log(TAG, "断开连接 ${logTarget.orEmpty()}")
        activeChannel?.disconnect()
        activeChannel = null
        session?.disconnect()
        session = null
        scope.cancel()
        notifyState(SshConnectionState.Disconnected)
    }

    private fun pump(stream: InputStream, onLine: (String) -> Unit) {
        try {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            }
        } catch (_: IOException) {
            // 通道关闭过程中流可能已经被关掉，忽略即可。
        }
    }

    private fun notifyOutput(text: String) {
        listener?.onOutput(text)
    }

    private fun notifyError(text: String) {
        listener?.onError(text)
    }

    private fun notifyState(state: SshConnectionState) {
        listener?.onStateChanged(state)
    }

    private object TrustAllHostKeyRepository : HostKeyRepository {
        override fun check(host: String?, key: ByteArray?): Int = HostKeyRepository.OK

        override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

        override fun remove(host: String?, type: String?) = Unit

        override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

        override fun getKnownHostsRepositoryID(): String = "literm-trust-all"

        override fun getHostKey(): Array<HostKey> = emptyArray()

        override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000

        /** 写入 app.log 的标签。 */
        const val TAG = "SSH"
    }
}
