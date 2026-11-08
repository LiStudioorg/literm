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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.local.KeyEntity
import com.literm.listudioorg.ui.components.AddHostDialog
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme

private val HostAccentColors = listOf(
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
    Color(0xFF00838F),
)

private val PreviewHosts = listOf(
    Host(
        id = 1L,
        name = "prod-web",
        ip = "192.168.1.100",
        port = 22,
        username = "root",
    ),
    Host(
        id = 2L,
        name = "home-nas",
        ip = "192.168.1.20",
        port = 22,
        username = "admin",
    ),
    Host(
        id = 3L,
        name = "dev-sandbox",
        ip = "dev.literm.app",
        port = 2222,
        username = "literm",
    ),
    Host(
        id = 4L,
        name = "edge-pi",
        ip = "10.0.0.42",
        port = 22,
        username = "ubuntu",
    ),
)

/**
 * 主机列表页：展示 SQLite 中的已保存主机，支持搜索、新增、编辑、删除与连接。
 */
@Composable
fun HostListScreen(
    hosts: List<Host>,
    onAddHost: (Host) -> Unit,
    onHostClick: (Host) -> Unit,
    modifier: Modifier = Modifier,
    availableKeys: List<KeyEntity> = emptyList(),
    onUpdateHost: (Host) -> Unit = {},
    onDeleteHost: (Host) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAddHostDialog by rememberSaveable { mutableStateOf(false) }
    var editingHost by remember { mutableStateOf<Host?>(null) }
    var pendingDelete by remember { mutableStateOf<Host?>(null) }
    val filteredHosts = remember(hosts, query) {
        hosts.filter { host -> host.matches(query) }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val horizontalPadding = if (maxWidth >= 1000.dp) 32.dp else 20.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isTablet) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                top = 24.dp,
                end = horizontalPadding,
                bottom = 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HostListHeader(
                    query = query,
                    onQueryChange = { query = it },
                    hostCount = hosts.size,
                )
            }

            if (hosts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyHostDatabase()
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = stringResource(
                            if (query.isBlank()) R.string.hosts_all else R.string.hosts_search_results,
                        ),
                        trailingText = pluralStringResource(
                            R.plurals.host_count,
                            filteredHosts.size,
                            filteredHosts.size,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (filteredHosts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NoMatchingHosts(onClearSearch = { query = "" })
                    }
                } else {
                    items(
                        items = filteredHosts,
                        key = { host -> host.id },
                    ) { host ->
                        HostCard(
                            host = host,
                            onClick = { onHostClick(host) },
                            onEdit = { editingHost = host },
                            onDelete = { pendingDelete = host },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddHostDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.hosts_add),
            )
        }

        if (showAddHostDialog) {
            AddHostDialog(
                onDismissRequest = { showAddHostDialog = false },
                availableKeys = availableKeys,
                onSave = { host ->
                    onAddHost(host)
                    showAddHostDialog = false
                },
            )
        }

        editingHost?.let { host ->
            AddHostDialog(
                onDismissRequest = { editingHost = null },
                availableKeys = availableKeys,
                initialHost = host,
                onSave = { updated ->
                    onUpdateHost(updated)
                    editingHost = null
                },
            )
        }

        pendingDelete?.let { host ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.host_delete_title)) },
                text = { Text(stringResource(R.string.host_delete_message, host.name.ifBlank { host.ip })) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteHost(host)
                            pendingDelete = null
                        },
                    ) {
                        Text(stringResource(R.string.host_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.host_delete_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun HostListHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    hostCount: Int,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.hosts_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = hostCount.toString(),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.hosts_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.hosts_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.search_clear),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/**
 * 主机卡片：点击连接，长按弹出编辑/删除菜单。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HostCard(
    host: Host,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = accentFor(host)
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent,
                                accent.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name.ifBlank { host.ip },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "${host.username}@${host.ip}:${host.port}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HostTag(tag = "SSH")
                    HostTag(tag = stringResource(R.string.host_port_tag, host.port))
                }
            }

            Spacer(Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.host_menu_edit)) },
                onClick = {
                    showMenu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.host_menu_delete)) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun HostTag(tag: String) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (tag.lowercase()) {
        "ssh" -> colorScheme.primaryContainer.copy(alpha = 0.72f) to colorScheme.onPrimaryContainer
        else -> colorScheme.surfaceContainerHighest to colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun EmptyHostDatabase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.hosts_empty_database_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.hosts_empty_database_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoMatchingHosts(onClearSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.hosts_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.hosts_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onClearSearch) {
            Text(text = stringResource(R.string.search_clear))
        }
    }
}

private fun accentFor(host: Host): Color {
    val index = (host.id.hashCode() and Int.MAX_VALUE) % HostAccentColors.size
    return HostAccentColors[index]
}

private fun Host.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.trim()
    return name.contains(normalizedQuery, ignoreCase = true) ||
        ip.contains(normalizedQuery, ignoreCase = true) ||
        username.contains(normalizedQuery, ignoreCase = true) ||
        port.toString().contains(normalizedQuery) ||
        "ssh".contains(normalizedQuery, ignoreCase = true)
}

@Preview(name = "Hosts phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostListPhonePreview() {
    LiTermTheme {
        HostListScreen(
            hosts = PreviewHosts,
            onAddHost = {},
            onUpdateHost = {},
            onDeleteHost = {},
            onHostClick = {},
        )
    }
}

@Preview(name = "Hosts tablet", showBackground = true, widthDp = 1000, heightDp = 720)
@Composable
private fun HostListTabletPreview() {
    LiTermTheme {
        HostListScreen(
            hosts = PreviewHosts,
            onAddHost = {},
            onUpdateHost = {},
            onDeleteHost = {},
            onHostClick = {},
        )
    }
}

@Preview(name = "Hosts empty database", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostListEmptyPreview() {
    LiTermTheme {
        HostListScreen(
            hosts = emptyList(),
            onAddHost = {},
            onUpdateHost = {},
            onDeleteHost = {},
            onHostClick = {},
        )
    }
}
