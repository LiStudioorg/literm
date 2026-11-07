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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.local.KeyEntity
import com.literm.listudioorg.ui.components.BrandMark
import com.literm.listudioorg.ui.theme.LiTermTheme
import com.literm.listudioorg.ui.viewmodel.HostViewModel
import com.literm.listudioorg.ui.viewmodel.KeyViewModel

private enum class MainDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Hosts(R.string.nav_hosts, Icons.Outlined.Dns),
    Keychain(R.string.nav_keychain, Icons.Outlined.Key),
    Snippets(R.string.nav_snippets, Icons.Outlined.Code),
    Logs(R.string.nav_logs, Icons.Outlined.Article),
    Settings(R.string.nav_settings, Icons.Outlined.Settings),
    LocalLinux(R.string.nav_local_linux, Icons.Outlined.Terminal),
}

/**
 * 应用主页：负责在主机 / 本地 Linux / 密钥 / 片段 / 日志 / 设置之间切换，
 * 并把终端页面的全屏视图叠加在导航内容之上。
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    hostViewModel: HostViewModel = viewModel(factory = HostViewModel.Factory),
    keyViewModel: KeyViewModel = viewModel(factory = KeyViewModel.Factory),
) {
    val hosts by hostViewModel.hosts.collectAsStateWithLifecycle()
    // 添加主机时“使用密钥”需要从 Keychain 取列表。
    val keys by keyViewModel.keys.collectAsStateWithLifecycle()
    var activeHost by remember { mutableStateOf<Host?>(null) }
    var activeLocalLinux by remember { mutableStateOf<String?>(null) }

    val currentHost = activeHost
    val currentLocalLinux = activeLocalLinux
    when {
        currentLocalLinux != null -> {
            LocalTerminalScreen(
                distroId = currentLocalLinux,
                onBack = { activeLocalLinux = null },
                modifier = modifier,
            )
        }
        currentHost != null -> {
            TerminalScreen(
                host = currentHost,
                onBack = { activeHost = null },
                modifier = modifier,
            )
        }
        else -> {
            MainScreenContent(
                hosts = hosts,
                availableKeys = keys,
                onAddHost = hostViewModel::addHost,
                onHostClick = { activeHost = it },
                onStartLocalLinux = { distroId -> activeLocalLinux = distroId },
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun MainScreenContent(
    hosts: List<Host>,
    onAddHost: (Host) -> Unit,
    onHostClick: (Host) -> Unit,
    onStartLocalLinux: (String) -> Unit,
    modifier: Modifier = Modifier,
    availableKeys: List<KeyEntity> = emptyList(),
) {
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.Hosts) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp || maxWidth > maxHeight

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail) {
                    MainBottomBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { selectedDestination = it },
                    )
                }
            },
        ) { contentPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                if (useNavigationRail) {
                    MainNavigationRail(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { selectedDestination = it },
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    DestinationContent(
                        destination = selectedDestination,
                        hosts = hosts,
                        availableKeys = availableKeys,
                        onAddHost = onAddHost,
                        onHostClick = onHostClick,
                        onStartLocalLinux = onStartLocalLinux,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * 根据当前导航目的地渲染对应页面，占位页已全部替换为真实实现。
 */
@Composable
private fun DestinationContent(
    destination: MainDestination,
    hosts: List<Host>,
    availableKeys: List<KeyEntity>,
    onAddHost: (Host) -> Unit,
    onHostClick: (Host) -> Unit,
    onStartLocalLinux: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        MainDestination.Hosts -> HostListScreen(
            hosts = hosts,
            availableKeys = availableKeys,
            onAddHost = onAddHost,
            onHostClick = onHostClick,
            modifier = modifier,
        )
        MainDestination.LocalLinux -> LocalLinuxScreen(
            onStartLocalLinux = onStartLocalLinux,
            modifier = modifier,
        )
        MainDestination.Keychain -> KeychainScreen(modifier = modifier)
        MainDestination.Snippets -> SnippetsScreen(modifier = modifier)
        MainDestination.Logs -> LogsScreen(modifier = modifier)
        MainDestination.Settings -> SettingsScreen(modifier = modifier)
    }
}

@Composable
private fun MainNavigationRail(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .width(96.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandMark()
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        MainDestination.entries
            .filterNot { it == MainDestination.LocalLinux }
            .forEach { destination ->
                NavigationRailItem(
                    selected = selectedDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = {
                        RailLabel(text = stringResource(destination.labelRes))
                    },
                    alwaysShowLabel = true,
                )
            }

        Spacer(Modifier.weight(1f))

        NavigationRailItem(
            selected = selectedDestination == MainDestination.LocalLinux,
            onClick = { onDestinationSelected(MainDestination.LocalLinux) },
            icon = {
                Icon(
                    imageVector = MainDestination.LocalLinux.icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = {
                RailLabel(text = stringResource(MainDestination.LocalLinux.labelRes))
            },
            alwaysShowLabel = true,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.sp,
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
    )
}

@Composable
private fun MainBottomBar(
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        MainDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

@Preview(name = "Main phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainScreenPhonePreview() {
    LiTermTheme {
        MainScreenContent(
            hosts = emptyList(),
            onAddHost = {},
            onHostClick = {},
            onStartLocalLinux = {},
        )
    }
}

@Preview(name = "Main tablet", showBackground = true, widthDp = 1000, heightDp = 720)
@Composable
private fun MainScreenTabletPreview() {
    LiTermTheme {
        MainScreenContent(
            hosts = emptyList(),
            onAddHost = {},
            onHostClick = {},
            onStartLocalLinux = {},
        )
    }
}
