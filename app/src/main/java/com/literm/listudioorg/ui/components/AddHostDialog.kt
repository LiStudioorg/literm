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

package com.literm.listudioorg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.local.KeyEntity

/**
 * 添加 / 编辑主机表单。
 *
 * 认证方式二选一：密码或密钥。选择密钥时从 Keychain（SQLite `ssh_keys` 表）中挑选，
 * 保存时把选中密钥的明文内容写入 [Host.privateKey]，连接逻辑无需改动。
 *
 * [initialHost] 非空时表示编辑模式：预填表单并在保存时保留原有 id。
 */
@Composable
fun AddHostDialog(
    onDismissRequest: () -> Unit,
    onSave: (Host) -> Unit,
    modifier: Modifier = Modifier,
    availableKeys: List<KeyEntity> = emptyList(),
    initialHost: Host? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialHost?.name.orEmpty()) }
    var ip by rememberSaveable { mutableStateOf(initialHost?.ip.orEmpty()) }
    var port by rememberSaveable { mutableStateOf((initialHost?.port ?: 22).toString()) }
    var username by rememberSaveable { mutableStateOf(initialHost?.username.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(initialHost?.password.orEmpty()) }
    // 用布尔值保存认证方式：enum 无法直接进 rememberSaveable 的 Bundle。
    var useKeyAuth by rememberSaveable { mutableStateOf(!initialHost?.privateKey.isNullOrBlank()) }
    var selectedKeyId by rememberSaveable { mutableStateOf(0L) }
    var showKeyPicker by rememberSaveable { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }

    val selectedKey = remember(availableKeys, selectedKeyId, initialHost) {
        availableKeys.firstOrNull { key -> key.id == selectedKeyId }
            ?: run {
                // 编辑模式：原主机保存的是密钥明文，尝试按内容匹配回 Keychain 中的条目。
                val saved = initialHost?.privateKey
                if (saved.isNullOrBlank()) {
                    null
                } else {
                    availableKeys.firstOrNull { key -> key.privateKey == saved }
                }
            }
    }


    val nameValid = name.isNotBlank()
    val ipValid = ip.isNotBlank()
    val usernameValid = username.isNotBlank()
    val portNumber = port.toIntOrNull()
    val portValid = portNumber != null && portNumber in 1..65535
    // 选择密钥认证时必须挑一把。
    val keySelected = !useKeyAuth || selectedKey != null
    val formValid = nameValid && ipValid && usernameValid && portValid && keySelected

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 720.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(
                            if (initialHost == null) R.string.add_host_title else R.string.edit_host_title,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(Modifier.heightIn(min = 16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_name)) },
                            placeholder = { Text(stringResource(R.string.add_host_name_hint)) },
                            singleLine = true,
                            isError = attemptedSave && !nameValid,
                            supportingText = if (attemptedSave && !nameValid) {
                                { Text(stringResource(R.string.add_host_name_required)) }
                            } else {
                                null
                            },
                        )

                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_ip)) },
                            placeholder = { Text(stringResource(R.string.add_host_ip_hint)) },
                            singleLine = true,
                            isError = attemptedSave && !ipValid,
                            supportingText = if (attemptedSave && !ipValid) {
                                { Text(stringResource(R.string.add_host_ip_required)) }
                            } else {
                                null
                            },
                        )

                        OutlinedTextField(
                            value = port,
                            onValueChange = { newValue ->
                                if (newValue.length <= 5 && newValue.all { it.isDigit() }) {
                                    port = newValue
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_port)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = attemptedSave && !portValid,
                            supportingText = if (attemptedSave && !portValid) {
                                { Text(stringResource(R.string.add_host_port_invalid)) }
                            } else {
                                null
                            },
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_username)) },
                            singleLine = true,
                            isError = attemptedSave && !usernameValid,
                            supportingText = if (attemptedSave && !usernameValid) {
                                { Text(stringResource(R.string.add_host_username_required)) }
                            } else {
                                null
                            },
                        )

                        // 认证方式单选：密码 / 密钥。
                        AuthModeSelector(
                            useKeyAuth = useKeyAuth,
                            onUseKeyAuthChange = { useKeyAuth = it },
                        )

                        if (useKeyAuth) {
                            KeyPickerField(
                                selectedKey = selectedKey,
                                hasKeys = availableKeys.isNotEmpty(),
                                showError = attemptedSave && !keySelected,
                                onClick = { showKeyPicker = true },
                            )
                        } else {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.add_host_password)) },
                                singleLine = true,
                            )
                        }
                    }

                    Spacer(Modifier.heightIn(min = 20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.add_host_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                attemptedSave = true
                                if (formValid) {
                                    onSave(
                                        Host(
                                            // 编辑模式保留原有 id；新增时 id=0 交由 SQLite 自增。
                                            id = initialHost?.id ?: 0L,
                                            name = name.trim(),
                                            ip = ip.trim(),
                                            port = portNumber ?: 22,
                                            username = username.trim(),
                                            password = if (useKeyAuth) {
                                                null
                                            } else {
                                                password.takeIf { it.isNotBlank() }
                                            },
                                            privateKey = if (useKeyAuth) {
                                                selectedKey?.privateKey
                                            } else {
                                                null
                                            },
                                        ),
                                    )
                                }
                            },
                        ) {
                            Text(stringResource(R.string.add_host_save))
                        }
                    }
                }
            }
        }
    }

    if (showKeyPicker) {
        KeyPickerDialog(
            availableKeys = availableKeys,
            selectedKeyId = selectedKeyId,
            onDismiss = { showKeyPicker = false },
            onPick = { key ->
                selectedKeyId = key.id
                showKeyPicker = false
            },
        )
    }
}

/** 密码 / 密钥两种认证方式的单选行。 */
@Composable
private fun AuthModeSelector(
    useKeyAuth: Boolean,
    onUseKeyAuthChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.add_host_auth_method),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AuthModeOption(
            label = stringResource(R.string.add_host_auth_password),
            selected = !useKeyAuth,
            onSelect = { onUseKeyAuthChange(false) },
        )

        AuthModeOption(
            label = stringResource(R.string.add_host_auth_key),
            selected = useKeyAuth,
            onSelect = { onUseKeyAuthChange(true) },
        )
    }
}

/** 单个认证方式选项，整行可点。 */
@Composable
private fun AuthModeOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** 只读输入框样式的按钮，点击后弹出密钥选择器。 */
@Composable
private fun KeyPickerField(
    selectedKey: KeyEntity?,
    hasKeys: Boolean,
    showError: Boolean,
    onClick: () -> Unit,
) {
    OutlinedTextField(
        value = selectedKey?.name.orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = hasKeys,
        // 只读字段不会自己处理点击，这里把整行点击转发给密钥选择器。
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasKeys, onClick = onClick),
        label = { Text(stringResource(R.string.add_host_select_key)) },
        placeholder = { Text(stringResource(R.string.add_host_select_key_placeholder)) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
            )
        },
        singleLine = true,
        isError = showError,
        supportingText = when {
            showError -> {
                { Text(stringResource(R.string.add_host_select_key_required)) }
            }
            !hasKeys -> {
                { Text(stringResource(R.string.add_host_no_keys)) }
            }
            else -> null
        },
    )
}

/** 从 Keychain 中选择一把密钥。 */
@Composable
private fun KeyPickerDialog(
    availableKeys: List<KeyEntity>,
    selectedKeyId: Long,
    onDismiss: () -> Unit,
    onPick: (KeyEntity) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.add_host_pick_key_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )

                Spacer(Modifier.heightIn(min = 8.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    availableKeys.forEach { key ->
                        KeyPickerRow(
                            key = key,
                            checked = key.id == selectedKeyId,
                            onClick = { onPick(key) },
                        )
                    }
                }

                Spacer(Modifier.heightIn(min = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.add_host_cancel))
                    }
                }
            }
        }
    }
}

/** 密钥选择器里的一行。 */
@Composable
private fun KeyPickerRow(
    key: KeyEntity,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = key.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = key.keyTypeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
