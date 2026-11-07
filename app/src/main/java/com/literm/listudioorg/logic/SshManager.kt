/*
 * Copyright 2024 li63050a <li63050@foxmail.com>
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

class SshManager {
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
            notifyState(SshConnectionState.Connected)
            Result.success(Unit)
        } catch (error: Exception) {
            newSession?.disconnect()
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
                    activeChannel?.disconnect()
                    activeChannel = null
                    notifyError(error.message ?: "Command failed")
                }
            }
        }

        return true
    }

    fun disconnect() {
        closed = true
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
            // The stream can be closed while the channel is shutting down.
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
    }
}
