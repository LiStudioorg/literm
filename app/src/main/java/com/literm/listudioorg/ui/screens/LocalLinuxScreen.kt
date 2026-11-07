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

package com.literm.listudioorg.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.literm.listudioorg.R
import com.literm.listudioorg.logic.LinuxDistro
import com.literm.listudioorg.logic.LinuxInstallStatus
import com.literm.listudioorg.logic.LinuxViewModel
import com.literm.listudioorg.logic.defaultLinuxDistros
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme
import kotlin.math.roundToInt

private data class DistroVisual(
    val mark: String,
    val accent: Color,
    @StringRes val sizeRes: Int,
)

private fun visualFor(distroId: String): DistroVisual = when (distroId) {
    "alpine" -> DistroVisual("AL", Color(0xFF0F5D8C), R.string.distro_alpine_size)
    "ubuntu" -> DistroVisual("UB", Color(0xFFE95420), R.string.distro_ubuntu_size)
    "debian" -> DistroVisual("DE", Color(0xFFA81D33), R.string.distro_debian_size)
    "arch" -> DistroVisual("AR", Color(0xFF1793D1), R.string.distro_arch_size)
    "kali" -> DistroVisual("KA", Color(0xFF6E4AA8), R.string.distro_kali_size)
    else -> DistroVisual(
        mark = distroId.take(2).uppercase(),
        accent = Color(0xFF546E7A),
        sizeRes = R.string.distro_alpine_size,
    )
}

@Composable
fun LocalLinuxScreen(
    onStartLocalLinux: (String) -> Unit,
    modifier: Modifier = Modifier,
    linuxViewModel: LinuxViewModel = viewModel(factory = LinuxViewModel.Factory),
) {
    val distros by linuxViewModel.distros.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LocalLinuxScreenContent(
        distros = distros,
        onDownload = { distroId ->
            linuxViewModel.startDownload(context, distroId)
        },
        onStart = { distro -> onStartLocalLinux(distro.id) },
        modifier = modifier,
    )
}

@Composable
internal fun LocalLinuxScreenContent(
    distros: List<LinuxDistro>,
    onDownload: (String) -> Unit,
    onStart: (LinuxDistro) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth > 760.dp) 760.dp else maxWidth
        val installedCount = distros.count { distro ->
            distro.installStatus == LinuxInstallStatus.Installed
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(contentWidth)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LocalLinuxHeader()
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.local_linux_environments),
                    trailingText = stringResource(
                        R.string.local_linux_installed_count,
                        installedCount,
                        distros.size,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(
                items = distros,
                key = { distro -> distro.id },
            ) { distro ->
                LinuxDistroCard(
                    distro = distro,
                    onDownload = { onDownload(distro.id) },
                    onStart = { onStart(distro) },
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LocalLinuxHeader() {
    Column {
        Text(
            text = stringResource(R.string.local_linux_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.local_linux_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.local_linux_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.local_linux_banner_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinuxDistroCard(
    distro: LinuxDistro,
    onDownload: () -> Unit,
    onStart: () -> Unit,
) {
    val visual = visualFor(distro.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                                visual.accent,
                                visual.accent.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = visual.mark,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = distro.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(distro.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                DistroMetaRow(
                    distro = distro,
                    sizeRes = visual.sizeRes,
                )
            }

            Spacer(Modifier.width(12.dp))

            DistroAction(
                distro = distro,
                onDownload = onDownload,
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun DistroMetaRow(
    distro: LinuxDistro,
    @StringRes sizeRes: Int,
) {
    val statusLabelRes: Int? = when (distro.installStatus) {
        LinuxInstallStatus.NotInstalled -> null
        LinuxInstallStatus.Downloading -> R.string.local_linux_downloading
        LinuxInstallStatus.Installed -> R.string.local_linux_installed
    }
    val statusColor = when (distro.installStatus) {
        LinuxInstallStatus.NotInstalled -> MaterialTheme.colorScheme.onSurfaceVariant
        LinuxInstallStatus.Downloading -> MaterialTheme.colorScheme.tertiary
        LinuxInstallStatus.Installed -> MaterialTheme.colorScheme.primary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (statusLabelRes != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = stringResource(statusLabelRes),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "  ·  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(sizeRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DistroAction(
    distro: LinuxDistro,
    onDownload: () -> Unit,
    onStart: () -> Unit,
) {
    when (distro.installStatus) {
        LinuxInstallStatus.NotInstalled -> {
            Button(
                onClick = onDownload,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.local_linux_download),
                    fontSize = 13.sp,
                )
            }
        }

        LinuxInstallStatus.Downloading -> {
            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(
                        R.string.local_linux_progress_percent,
                        (distro.downloadProgress.coerceIn(0f, 1f) * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { distro.downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }

        LinuxInstallStatus.Installed -> {
            FilledTonalButton(
                onClick = onStart,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.local_linux_start),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun previewDistros(): List<LinuxDistro> =
    defaultLinuxDistros().mapIndexed { index, distro ->
        when (index) {
            0 -> distro.copy(
                installStatus = LinuxInstallStatus.Installed,
                downloadProgress = 1f,
            )
            1 -> distro.copy(
                installStatus = LinuxInstallStatus.Downloading,
                downloadProgress = 0.42f,
            )
            else -> distro
        }
    }

@Preview(name = "Local Linux phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LocalLinuxPhonePreview() {
    LiTermTheme {
        LocalLinuxScreenContent(
            distros = previewDistros(),
            onDownload = {},
            onStart = {},
        )
    }
}

@Preview(name = "Local Linux tablet", showBackground = true, widthDp = 1000, heightDp = 720)
@Composable
private fun LocalLinuxTabletPreview() {
    LiTermTheme {
        LocalLinuxScreenContent(
            distros = previewDistros(),
            onDownload = {},
            onStart = {},
        )
    }
}
