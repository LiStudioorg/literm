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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.literm.listudioorg.R
import com.literm.listudioorg.logic.update.ReleaseInfo
import com.literm.listudioorg.logic.update.UpdatePrefs
import com.literm.listudioorg.ui.theme.LiTermTheme

/**
 * 更新弹窗。
 *
 * - 展示新版本号、更新日志
 * - [ReleaseInfo.isForceUpdate] 为 true 时隐藏取消按钮并禁用返回键/点击外部关闭
 * - 内置“下载加速源”切换区：预置下拉 + 自定义输入，选择即时写入 SharedPreferences
 */
@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    currentVersion: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forceUpdate = release.isForceUpdate
    Dialog(
        onDismissRequest = { if (!forceUpdate) onDismiss() },
        properties = DialogProperties(
            // 强制更新：返回键和点击遮罩都不能关闭。
            dismissOnBackPress = !forceUpdate,
            dismissOnClickOutside = !forceUpdate,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.update_available_title, release.versionNameNormalized),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.update_current_version, currentVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (forceUpdate) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.update_force_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.update_changelog),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = release.body.trim().ifBlank {
                            stringResource(R.string.update_no_changelog)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                MirrorSelector(modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (!forceUpdate) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.update_action_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onUpdate) {
                        Text(stringResource(R.string.update_action_update))
                    }
                }
            }
        }
    }
}

/**
 * 下载加速源切换区：预置源下拉 + 自定义前缀输入，任何改动即时持久化到 [UpdatePrefs]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirrorSelector(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(UpdatePrefs.getMirrorMode(context)) }
    var customValue by remember { mutableStateOf(UpdatePrefs.getCustomMirror(context)) }
    var menuExpanded by remember { mutableStateOf(false) }

    val modeLabel = when (mode) {
        UpdatePrefs.MirrorMode.DIRECT -> stringResource(R.string.update_mirror_direct)
        UpdatePrefs.MirrorMode.GHPROXY_NET -> UpdatePrefs.MirrorMode.GHPROXY_NET.prefix
        UpdatePrefs.MirrorMode.GH_PROXY_COM -> UpdatePrefs.MirrorMode.GH_PROXY_COM.prefix
        UpdatePrefs.MirrorMode.CUSTOM -> {
            customValue.ifBlank { stringResource(R.string.update_mirror_custom_hint) }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.update_mirror_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            // 用可点击卡片模拟下拉触发器，避免额外依赖 ExposedDropdownMenu 的实验 API。
            Card(
                onClick = { menuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                UpdatePrefs.MirrorMode.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    UpdatePrefs.MirrorMode.DIRECT ->
                                        stringResource(R.string.update_mirror_direct)
                                    UpdatePrefs.MirrorMode.CUSTOM ->
                                        stringResource(R.string.update_mirror_custom_hint)
                                    else -> option.prefix
                                },
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            mode = option
                            UpdatePrefs.setMirrorMode(context, option)
                        },
                    )
                }
            }
        }

        if (mode == UpdatePrefs.MirrorMode.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customValue,
                onValueChange = {
                    customValue = it
                    UpdatePrefs.setCustomMirror(context, it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.update_mirror_custom_hint)) },
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateDialogPreview() {
    LiTermTheme {
        UpdateDialog(
            release = ReleaseInfo(
                tagName = "v0.2.0",
                body = "- 修复终端闪退\n- 新增片段管理",
                apkUrl = "https://example.com/literm.apk",
                htmlUrl = null,
                isForceUpdate = false,
            ),
            currentVersion = "0.1.0",
            onUpdate = {},
            onDismiss = {},
        )
    }
}
