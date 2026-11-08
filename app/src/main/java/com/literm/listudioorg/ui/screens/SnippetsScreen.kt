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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
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
import com.literm.listudioorg.data.local.SnippetEntity
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme
import com.literm.listudioorg.ui.viewmodel.SnippetViewModel

/**
 * 命令片段页：展示 SQLite `snippets` 表中的命令。
 *
 * 交互约定：点击卡片把命令复制到剪贴板，长按卡片弹出删除确认。
 */
@Composable
fun SnippetsScreen(
    modifier: Modifier = Modifier,
    snippetViewModel: SnippetViewModel = viewModel(factory = SnippetViewModel.Factory),
) {
    val snippets by snippetViewModel.snippets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SnippetsScreenContent(
        snippets = snippets,
        onAddSnippet = snippetViewModel::addSnippet,
        onUpdateSnippet = snippetViewModel::updateSnippet,
        onDeleteSnippet = snippetViewModel::deleteSnippet,
        onCopyCommand = { snippet ->
            copyCommandToClipboard(context, snippet)
        },
        modifier = modifier,
    )
}

@Composable
internal fun SnippetsScreenContent(
    snippets: List<SnippetEntity>,
    onAddSnippet: (name: String, command: String) -> Unit,
    onDeleteSnippet: (SnippetEntity) -> Unit,
    onCopyCommand: (SnippetEntity) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateSnippet: (SnippetEntity) -> Unit = {},
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingSnippet by remember { mutableStateOf<SnippetEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<SnippetEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.snippets_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.snippets_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (snippets.isEmpty()) {
                item {
                    EmptySnippets(
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = stringResource(R.string.snippets_saved),
                        trailingText = pluralStringResource(
                            R.plurals.snippet_count,
                            snippets.size,
                            snippets.size,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(items = snippets, key = { snippet -> snippet.id }) { snippet ->
                    SnippetCard(
                        snippet = snippet,
                        onClick = { onCopyCommand(snippet) },
                        onEdit = { editingSnippet = snippet },
                        onDelete = { pendingDelete = snippet },
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
                contentDescription = stringResource(R.string.snippets_add),
            )
        }
    }

    if (showAddDialog) {
        SnippetEditorDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, command ->
                onAddSnippet(name, command)
                showAddDialog = false
            },
        )
    }

    editingSnippet?.let { snippet ->
        SnippetEditorDialog(
            initialSnippet = snippet,
            onDismiss = { editingSnippet = null },
            onSave = { name, command ->
                onUpdateSnippet(snippet.copy(name = name, command = command))
                editingSnippet = null
            },
        )
    }

    pendingDelete?.let { snippet ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.snippet_delete_title)) },
            text = { Text(stringResource(R.string.snippet_delete_message, snippet.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSnippet(snippet)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.snippet_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.snippet_delete_cancel))
                }
            },
        )
    }
}

/** 单个片段卡片：点击复制命令，长按弹出编辑/删除菜单。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SnippetCard(
    snippet: SnippetEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
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
                                    Color(0xFF00897B),
                                    Color(0xFF00897B).copy(alpha = 0.72f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snippet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = snippet.singleLineCommand,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.snippets_copy_hint),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.snippet_menu_edit)) },
                onClick = {
                    showMenu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.snippet_menu_delete)) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

/** 新增 / 编辑片段弹窗：输入名称与命令内容。 */
@Composable
private fun SnippetEditorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, command: String) -> Unit,
    initialSnippet: SnippetEntity? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialSnippet?.name.orEmpty()) }
    var command by rememberSaveable { mutableStateOf(initialSnippet?.command.orEmpty()) }
    var attemptedSave by remember { mutableStateOf(false) }

    val nameValid = name.isNotBlank()
    val commandValid = command.isNotBlank()

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
                        text = stringResource(
                            if (initialSnippet == null) R.string.snippet_new_title else R.string.snippet_edit_title,
                        ),
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
                            label = { Text(stringResource(R.string.snippet_name)) },
                            placeholder = { Text(stringResource(R.string.snippet_name_hint)) },
                            singleLine = true,
                            isError = attemptedSave && !nameValid,
                            supportingText = if (attemptedSave && !nameValid) {
                                { Text(stringResource(R.string.snippet_name_required)) }
                            } else {
                                null
                            },
                        )

                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.snippet_command)) },
                            placeholder = { Text(stringResource(R.string.snippet_command_hint)) },
                            minLines = 4,
                            maxLines = 10,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            isError = attemptedSave && !commandValid,
                            supportingText = if (attemptedSave && !commandValid) {
                                { Text(stringResource(R.string.snippet_command_required)) }
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
                            Text(stringResource(R.string.snippet_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                attemptedSave = true
                                if (nameValid && commandValid) {
                                    onSave(name, command)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.snippet_save))
                        }
                    }
                }
            }
        }
    }
}

/** 空列表占位。 */
@Composable
private fun EmptySnippets(
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
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.snippets_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.snippets_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 用系统剪贴板复制命令，并提示结果。 */
private fun copyCommandToClipboard(context: Context, snippet: SnippetEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard == null) {
        Toast.makeText(context, R.string.snippets_copy_failed, Toast.LENGTH_SHORT).show()
        return
    }

    clipboard.setPrimaryClip(ClipData.newPlainText(snippet.name, snippet.command))
    Toast.makeText(context, R.string.snippets_copied, Toast.LENGTH_SHORT).show()
}

private val PreviewSnippets = listOf(
    SnippetEntity(
        id = 1L,
        name = "查看磁盘占用",
        command = "du -sh /* 2>/dev/null | sort -h | tail -20",
    ),
    SnippetEntity(
        id = 2L,
        name = "重启网络",
        command = "systemctl restart NetworkManager",
    ),
)

@Preview(name = "Snippets phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SnippetsPhonePreview() {
    LiTermTheme {
        SnippetsScreenContent(
            snippets = PreviewSnippets,
            onAddSnippet = { _, _ -> },
            onDeleteSnippet = {},
            onCopyCommand = {},
        )
    }
}

@Preview(name = "Snippets empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SnippetsEmptyPreview() {
    LiTermTheme {
        SnippetsScreenContent(
            snippets = emptyList(),
            onAddSnippet = { _, _ -> },
            onDeleteSnippet = {},
            onCopyCommand = {},
        )
    }
}
