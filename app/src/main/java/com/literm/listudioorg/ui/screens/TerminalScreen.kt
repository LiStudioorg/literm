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

package com.literm.listudioorg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.logic.ActiveTerminal
import com.literm.listudioorg.logic.LocalTerminalListener
import com.literm.listudioorg.logic.LocalTerminalManager
import com.literm.listudioorg.logic.LocalTerminalState
import com.literm.listudioorg.logic.SshConnectionState
import com.literm.listudioorg.logic.SshListener
import com.literm.listudioorg.logic.SshManager
import com.literm.listudioorg.logic.defaultLinuxDistros
import com.literm.listudioorg.service.TerminalService
import kotlinx.coroutines.launch

private val TerminalBackground = Color(0xFF000000)
private val TerminalMuted = Color(0xFF8B949E)
private val TerminalDivider = Color(0xFF262626)
private val TerminalInputBackground = Color(0xFF161616)
private val TerminalSendBackground = Color(0xFF2563EB)
private val TerminalSendDisabledBackground = Color(0xFF1C1C1C)

private enum class TerminalLineKind {
    Output,
    Error,
    Command,
    System,
}

private data class TerminalLine(
    val text: String,
    val kind: TerminalLineKind,
)

@Composable
fun TerminalScreen(
    host: Host,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(host.id) { SshManager() }
    val lines = remember(host.id) { mutableStateListOf<TerminalLine>() }
    var connectionState by remember(host.id) {
        mutableStateOf<SshConnectionState>(SshConnectionState.Connecting)
    }
    var command by rememberSaveable(host.id) { mutableStateOf("") }

    DisposableEffect(manager) {
        manager.listener = object : SshListener {
            override fun onOutput(text: String) {
                scope.launch {
                    lines += TerminalLine(text = text, kind = TerminalLineKind.Output)
                }
            }

            override fun onError(text: String) {
                scope.launch {
                    lines += TerminalLine(text = text, kind = TerminalLineKind.Error)
                }
            }

            override fun onStateChanged(state: SshConnectionState) {
                scope.launch { connectionState = state }
            }
        }

        // 进入终端：拉起前台服务防杀后台，并把会话注册给悬浮球。
        TerminalService.start(context)
        ActiveTerminal.attach(manager)

        onDispose {
            // 退出终端：注销会话，没有会话时关闭前台服务。
            ActiveTerminal.detach(manager)
            manager.listener = null
            manager.disconnect()
            TerminalService.stop(context)
        }
    }

    LaunchedEffect(manager) {
        val result = manager.connect(
            host = host.ip,
            port = host.port,
            username = host.username,
            password = host.password,
            privateKey = host.privateKey,
        )
        result.exceptionOrNull()?.let { error ->
            lines += TerminalLine(
                text = context.getString(
                    R.string.terminal_connect_failed,
                    error.message ?: error.javaClass.simpleName,
                ),
                kind = TerminalLineKind.Error,
            )
        }
    }

    fun sendCommand() {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return
        }

        lines += TerminalLine(text = "\$ $trimmed", kind = TerminalLineKind.Command)
        command = ""

        if (!manager.executeCommand(trimmed)) {
            lines += TerminalLine(
                text = context.getString(R.string.terminal_not_connected),
                kind = TerminalLineKind.System,
            )
        }
    }

    val statusText = when (connectionState) {
        SshConnectionState.Connecting -> stringResource(R.string.terminal_status_connecting)
        SshConnectionState.Connected -> stringResource(R.string.terminal_status_connected)
        SshConnectionState.Disconnected -> stringResource(R.string.terminal_status_disconnected)
        is SshConnectionState.Failed -> stringResource(R.string.terminal_status_failed)
    }
    val statusColor = when (connectionState) {
        SshConnectionState.Connecting -> Color(0xFFFFC857)
        SshConnectionState.Connected -> Color(0xFF4CD964)
        SshConnectionState.Disconnected -> TerminalMuted
        is SshConnectionState.Failed -> Color(0xFFFF453A)
    }

    TerminalPane(
        title = host.ip,
        statusText = statusText,
        statusColor = statusColor,
        lines = lines,
        command = command,
        onCommandChange = { command = it },
        onSend = { sendCommand() },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun LocalTerminalScreen(
    distroId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(distroId) { LocalTerminalManager() }
    val lines = remember(distroId) { mutableStateListOf<TerminalLine>() }
    var terminalState by remember(distroId) {
        mutableStateOf<LocalTerminalState>(LocalTerminalState.Starting)
    }
    var command by rememberSaveable(distroId) { mutableStateOf("") }
    val distroName = remember(distroId) {
        defaultLinuxDistros().firstOrNull { it.id == distroId }?.name ?: distroId
    }

    DisposableEffect(manager) {
        manager.listener = object : LocalTerminalListener {
            override fun onOutput(text: String) {
                scope.launch {
                    lines += TerminalLine(text = text, kind = TerminalLineKind.Output)
                }
            }

            override fun onError(text: String) {
                scope.launch {
                    lines += TerminalLine(text = text, kind = TerminalLineKind.Error)
                }
            }

            override fun onStateChanged(state: LocalTerminalState) {
                scope.launch { terminalState = state }
            }
        }

        // 本地 PRoot 终端同样需要前台服务与悬浮球输入通道。
        TerminalService.start(context)
        ActiveTerminal.attach(manager)

        onDispose {
            ActiveTerminal.detach(manager)
            manager.listener = null
            manager.stop()
            TerminalService.stop(context)
        }
    }

    LaunchedEffect(manager) {
        val result = manager.startProot(context, distroId)
        result.exceptionOrNull()?.let { error ->
            lines += TerminalLine(
                text = context.getString(
                    R.string.local_linux_start_failed,
                    error.message ?: error.javaClass.simpleName,
                ),
                kind = TerminalLineKind.Error,
            )
        }
    }

    fun sendCommand() {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return
        }

        lines += TerminalLine(text = "\$ $trimmed", kind = TerminalLineKind.Command)
        command = ""

        if (!manager.sendInput(trimmed)) {
            lines += TerminalLine(
                text = context.getString(R.string.local_linux_not_running),
                kind = TerminalLineKind.System,
            )
        }
    }

    val statusText = when (terminalState) {
        LocalTerminalState.Starting -> stringResource(R.string.local_linux_status_starting)
        LocalTerminalState.Running -> stringResource(R.string.local_linux_status_running)
        LocalTerminalState.Stopped -> stringResource(R.string.local_linux_status_stopped)
        is LocalTerminalState.Failed -> stringResource(R.string.local_linux_status_failed)
    }
    val statusColor = when (terminalState) {
        LocalTerminalState.Starting -> Color(0xFFFFC857)
        LocalTerminalState.Running -> Color(0xFF4CD964)
        LocalTerminalState.Stopped -> TerminalMuted
        is LocalTerminalState.Failed -> Color(0xFFFF453A)
    }

    TerminalPane(
        title = distroName,
        statusText = statusText,
        statusColor = statusColor,
        lines = lines,
        command = command,
        onCommandChange = { command = it },
        onSend = { sendCommand() },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun TerminalPane(
    title: String,
    statusText: String,
    statusColor: Color,
    lines: List<TerminalLine>,
    command: String,
    onCommandChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.terminal_back),
                    tint = Color.White,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = TerminalMuted,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }

        HorizontalDivider(color = TerminalDivider)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(lines) { line ->
                Text(
                    text = line.text,
                    color = line.kind.color(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        HorizontalDivider(color = TerminalDivider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = command,
                onValueChange = onCommandChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocus),
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalInputBackground, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (command.isEmpty()) {
                            Text(
                                text = stringResource(R.string.terminal_input_hint),
                                color = TerminalMuted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            Spacer(Modifier.width(8.dp))

            val canSend = command.isNotBlank()
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (canSend) TerminalSendBackground else TerminalSendDisabledBackground,
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.terminal_send),
                    tint = if (canSend) Color.White else TerminalMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun TerminalLineKind.color(): Color = when (this) {
    TerminalLineKind.Output -> Color(0xFFE6EDF3)
    TerminalLineKind.Error -> Color(0xFFFF7B72)
    TerminalLineKind.Command -> Color(0xFF79C0FF)
    TerminalLineKind.System -> TerminalMuted
}
