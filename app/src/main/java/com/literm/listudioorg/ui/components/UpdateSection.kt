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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.literm.listudioorg.R
import com.literm.listudioorg.logic.update.ApkDownloader
import com.literm.listudioorg.logic.update.ReleaseInfo
import com.literm.listudioorg.logic.update.UpdateManager
import com.literm.listudioorg.logic.update.UpdatePrefs
import kotlinx.coroutines.launch

/**
 * 设置页的“检查更新”卡片：手动触发 GitHub Releases 检查，
 * 有新版本弹出 [UpdateDialog]，否则用 Toast 提示结果。
 */
@Composable
fun UpdateCheckSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var pendingRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var currentVersion by remember { mutableStateOf("") }

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    val listener = ApkDownloader.StatusListener { message -> toast(message) }

    fun performCheck() {
        if (checking) return
        checking = true
        toast(context.getString(R.string.update_checking))
        scope.launch {
            when (val result = UpdateManager.checkForUpdate()) {
                is UpdateManager.CheckResult.UpdateAvailable -> {
                    currentVersion = result.currentVersion
                    pendingRelease = result.release
                }
                is UpdateManager.CheckResult.UpToDate -> {
                    toast(context.getString(R.string.update_up_to_date, result.currentVersion))
                }
                is UpdateManager.CheckResult.Error -> {
                    toast(context.getString(R.string.update_check_failed, result.message))
                }
            }
            checking = false
        }
    }

    UpdateCard(
        modifier = modifier,
        titleRes = R.string.update_check_title,
        descriptionRes = R.string.update_check_description,
        actionLabelRes = R.string.update_action_check,
        onAction = ::performCheck,
    )

    val release = pendingRelease
    if (release != null) {
        UpdateDialog(
            release = release,
            currentVersion = currentVersion,
            onUpdate = {
                val apkUrl = release.apkUrl
                if (apkUrl.isNullOrBlank()) {
                    toast(context.getString(R.string.update_no_apk_link))
                } else {
                    val finalUrl = UpdatePrefs.resolveDownloadUrl(context, apkUrl)
                    val fileName = "LiTerm-${release.versionNameNormalized}.apk"
                    ApkDownloader.startDownload(context, finalUrl, fileName, listener)
                    if (!release.isForceUpdate) {
                        pendingRelease = null
                    }
                }
            },
            onDismiss = { pendingRelease = null },
        )
    }
}

/**
 * 设置页的“从本地安装更新”卡片：选择已下载好的 APK 直接调起系统安装，
 * 作为下载/网络链路不可用时的兜底。
 */
@Composable
fun LocalInstallSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        ApkDownloader.installLocalApk(
            context = context,
            apkUri = uri,
            listener = ApkDownloader.StatusListener { message -> toast(message) },
        )
    }

    UpdateCard(
        modifier = modifier,
        titleRes = R.string.update_local_install_title,
        descriptionRes = R.string.update_local_install_description,
        actionLabelRes = R.string.update_action_pick,
        onAction = {
            runCatching {
                picker.launch(arrayOf("application/vnd.android.package-archive"))
            }.onFailure {
                toast(context.getString(R.string.update_local_pick_failed))
            }
        },
    )
}

/** 更新卡片通用外观：标题 + 描述 + 右侧动作按钮，与设置页现有卡片风格一致。 */
@Composable
private fun UpdateCard(
    modifier: Modifier = Modifier,
    titleRes: Int,
    descriptionRes: Int,
    actionLabelRes: Int,
    onAction: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(12.dp))

            TextButton(onClick = onAction) {
                Text(stringResource(actionLabelRes))
            }
        }
    }
}
