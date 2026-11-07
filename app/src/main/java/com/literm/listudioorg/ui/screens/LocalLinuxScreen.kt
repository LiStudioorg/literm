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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.literm.listudioorg.R
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme

private data class LinuxDistro(
    val id: String,
    val name: String,
    val mark: String,
    val accent: Color,
    @StringRes val descriptionRes: Int,
    @StringRes val sizeRes: Int,
    val installedByDefault: Boolean,
)

private val linuxDistros = listOf(
    LinuxDistro(
        id = "alpine",
        name = "Alpine Linux",
        mark = "AL",
        accent = Color(0xFF0F5D8C),
        descriptionRes = R.string.distro_alpine_desc,
        sizeRes = R.string.distro_alpine_size,
        installedByDefault = true,
    ),
    LinuxDistro(
        id = "ubuntu",
        name = "Ubuntu",
        mark = "UB",
        accent = Color(0xFFE95420),
        descriptionRes = R.string.distro_ubuntu_desc,
        sizeRes = R.string.distro_ubuntu_size,
        installedByDefault = true,
    ),
    LinuxDistro(
        id = "debian",
        name = "Debian",
        mark = "DE",
        accent = Color(0xFFA81D33),
        descriptionRes = R.string.distro_debian_desc,
        sizeRes = R.string.distro_debian_size,
        installedByDefault = false,
    ),
    LinuxDistro(
        id = "arch",
        name = "Arch Linux",
        mark = "AR",
        accent = Color(0xFF1793D1),
        descriptionRes = R.string.distro_arch_desc,
        sizeRes = R.string.distro_arch_size,
        installedByDefault = false,
    ),
    LinuxDistro(
        id = "kali",
        name = "Kali Linux",
        mark = "KA",
        accent = Color(0xFF6E4AA8),
        descriptionRes = R.string.distro_kali_desc,
        sizeRes = R.string.distro_kali_size,
        installedByDefault = false,
    ),
)

@Composable
fun LocalLinuxScreen(modifier: Modifier = Modifier) {
    var installedDistroIds by remember {
        mutableStateOf(
            linuxDistros
                .filter { distro -> distro.installedByDefault }
                .map { distro -> distro.id }
                .toSet(),
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val contentWidth = if (maxWidth > 760.dp) 760.dp else maxWidth

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
                        installedDistroIds.size,
                        linuxDistros.size,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(
                items = linuxDistros,
                key = { distro -> distro.id },
            ) { distro ->
                val isInstalled = distro.id in installedDistroIds
                LinuxDistroCard(
                    distro = distro,
                    isInstalled = isInstalled,
                    onAction = {
                        if (!isInstalled) {
                            installedDistroIds = installedDistroIds + distro.id
                        }
                    },
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
    isInstalled: Boolean,
    onAction: () -> Unit,
) {
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
                                distro.accent,
                                distro.accent.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = distro.mark,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isInstalled) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.local_linux_installed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "  ·  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(distro.sizeRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (isInstalled) {
                FilledTonalButton(
                    onClick = onAction,
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
            } else {
                Button(
                    onClick = onAction,
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
                        text = stringResource(R.string.local_linux_install),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Preview(name = "Local Linux phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LocalLinuxPhonePreview() {
    LiTermTheme {
        LocalLinuxScreen()
    }
}

@Preview(name = "Local Linux tablet", showBackground = true, widthDp = 1000, heightDp = 720)
@Composable
private fun LocalLinuxTabletPreview() {
    LiTermTheme {
        LocalLinuxScreen()
    }
}
