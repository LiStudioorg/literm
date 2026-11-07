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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.KeyEntity
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme
import com.literm.listudioorg.ui.viewmodel.KeyViewModel

/**
 * 密钥页：展示 SQLite `ssh_keys` 表中的私钥，支持新增、查看与删除。
 *
 * 交互约定：点击卡片查看私钥全文，长按卡片弹出删除确认。
 */
@Composable
fun KeychainScreen(
    modifier: Modifier = Modifier,
    keyViewModel: KeyViewModel = viewModel(factory = KeyViewModel.Factory),
) {
    val keys by keyViewModel.keys.collectAsStateWithLifecycle()

    KeychainScreenContent(
        keys = keys,
        onAddKey = keyViewModel::addKey,
        onDeleteKey = keyViewModel::deleteKey,
        modifier = modifier,
    )
}

@Composable
internal fun KeychainScreenContent(
    keys: List<KeyEntity>,
    onAddKey: (name: String, privateKey: String) -> Unit,
    onDeleteKey: (KeyEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var viewedKey by remember { mutableStateOf<KeyEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<KeyEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.keychain_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.keychain_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (keys.isEmpty()) {
                item {
                    EmptyKeychain(
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = stringResource(R.string.keychain_saved_keys),
                        trailingText = pluralStringResource(
                            R.plurals.key_count,
                            keys.size,
                            keys.size,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(items = keys, key = { key -> key.id }) { key ->
                    KeyCard(
                        key = key,
                        onClick = { viewedKey = key },
                        onLongClick = { pendingDelete = key },
                    )
                }

                item {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.keychain_add),
            )
        }
    }

    if (showAddDialog) {
        KeyEditorDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, privateKey ->
                onAddKey(name, privateKey)
                showAddDialog = false
            },
        )
    }

    viewedKey?.let { key ->
        KeyViewerDialog(
            key = key,
            onDismiss = { viewedKey = null },
        )
    }

    pendingDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.key_delete_title)) },
            text = { Text(stringResource(R.string.key_delete_message, key.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteKey(key)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.key_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.key_delete_cancel))
                }
            },
        )
    }
}

/** 单个密钥卡片：点击查看详情，长按删除。 */
@Composable
private fun KeyCard(
    key: KeyEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3F51B5),
                                Color(0xFF3F51B5).copy(alpha = 0.72f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = key.keyTypeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = key.previewLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 新增密钥弹窗：输入名称与私钥明文。 */
@Composable
private fun KeyEditorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, privateKey: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var privateKey by rememberSaveable { mutableStateOf("") }
    var attemptedSave by remember { mutableStateOf(false) }

    val nameValid = name.isNotBlank()
    val keyValid = privateKey.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.key_new_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.key_name)) },
                            placeholder = { Text(stringResource(R.string.key_name_hint)) },
                            singleLine = true,
                            isError = attemptedSave && !nameValid,
                            supportingText = if (attemptedSave && !nameValid) {
                                { Text(stringResource(R.string.key_name_required)) }
                            } else {
                                null
                            },
                        )

                        OutlinedTextField(
                            value = privateKey,
                            onValueChange = { privateKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.key_content)) },
                            placeholder = { Text(stringResource(R.string.key_content_hint)) },
                            minLines = 5,
                            maxLines = 10,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            isError = attemptedSave && !keyValid,
                            supportingText = if (attemptedSave && !keyValid) {
                                { Text(stringResource(R.string.key_content_required)) }
                            } else {
                                null
                            },
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.key_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                attemptedSave = true
                                if (nameValid && keyValid) {
                                    onSave(name, privateKey)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.key_save))
                        }
                    }
                }
            }
        }
    }
}

/** 查看密钥弹窗：展示完整私钥文本，可滚动。 */
@Composable
private fun KeyViewerDialog(
    key: KeyEntity,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = key.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = key.keyTypeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Text(
                            text = key.privateKey,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text(stringResource(R.string.key_close))
                        }
                    }
                }
            }
        }
    }
}

/** 空列表占位。 */
@Composable
private fun EmptyKeychain(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.keychain_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.keychain_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val PreviewKeys = listOf(
    KeyEntity(
        id = 1L,
        name = "work-rsa",
        privateKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAx...\n-----END RSA PRIVATE KEY-----",
    ),
    KeyEntity(
        id = 2L,
        name = "home-ed25519",
        privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEA...\n-----END OPENSSH PRIVATE KEY-----",
    ),
)

@Preview(name = "Keychain phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KeychainPhonePreview() {
    LiTermTheme {
        KeychainScreenContent(
            keys = PreviewKeys,
            onAddKey = { _, _ -> },
            onDeleteKey = {},
        )
    }
}

@Preview(name = "Keychain empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun KeychainEmptyPreview() {
    LiTermTheme {
        KeychainScreenContent(
            keys = emptyList(),
            onAddKey = { _, _ -> },
            onDeleteKey = {},
        )
    }
}
