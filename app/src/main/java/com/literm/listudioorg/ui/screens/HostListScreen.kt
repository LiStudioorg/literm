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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.literm.listudioorg.ui.components.SectionHeader
import com.literm.listudioorg.ui.theme.LiTermTheme

private data class Host(
    val id: String,
    val alias: String,
    val address: String,
    val user: String,
    val tags: List<String>,
    val accent: Color,
    val isOnline: Boolean,
)

private val sampleHosts = listOf(
    Host(
        id = "prod-web",
        alias = "prod-web",
        address = "192.168.1.100",
        user = "root",
        tags = listOf("ssh"),
        accent = Color(0xFF2E7D32),
        isOnline = true,
    ),
    Host(
        id = "home-nas",
        alias = "home-nas",
        address = "192.168.1.20",
        user = "admin",
        tags = listOf("ssh", "sftp"),
        accent = Color(0xFF1565C0),
        isOnline = true,
    ),
    Host(
        id = "dev-sandbox",
        alias = "dev-sandbox",
        address = "dev.literm.app",
        user = "literm",
        tags = listOf("ssh", "docker"),
        accent = Color(0xFF6A1B9A),
        isOnline = true,
    ),
    Host(
        id = "edge-pi",
        alias = "edge-pi",
        address = "10.0.0.42",
        user = "ubuntu",
        tags = listOf("ssh", "arm64"),
        accent = Color(0xFFEF6C00),
        isOnline = false,
    ),
)

@Composable
fun HostListScreen(modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredHosts = remember(query) {
        sampleHosts.filter { host -> host.matches(query) }
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
                bottom = 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HostListHeader(
                    query = query,
                    onQueryChange = { query = it },
                    hostCount = sampleHosts.size,
                )
            }

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
                    EmptyHosts(onClearSearch = { query = "" })
                }
            } else {
                items(
                    items = filteredHosts,
                    key = { host -> host.id },
                ) { host ->
                    HostCard(host = host)
                }
            }
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
            Column(modifier = Modifier.weight(1f)) {
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
            }

            Spacer(Modifier.width(12.dp))
            FilledTonalIconButton(
                onClick = {},
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.hosts_add),
                )
            }
        }

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

@Composable
private fun HostCard(host: Host) {
    Card(
        onClick = {},
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                host.accent,
                                host.accent.copy(alpha = 0.72f),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = host.alias,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (host.isOnline) {
                                    Color(0xFF35C759)
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            ),
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "${host.user}@${host.address}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    host.tags.forEach { tag ->
                        HostTag(tag = tag)
                    }
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
}

@Composable
private fun HostTag(tag: String) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (tag.lowercase()) {
        "ssh" -> colorScheme.primaryContainer.copy(alpha = 0.72f) to colorScheme.onPrimaryContainer
        "sftp" -> colorScheme.tertiaryContainer.copy(alpha = 0.72f) to colorScheme.onTertiaryContainer
        "docker" -> colorScheme.secondaryContainer.copy(alpha = 0.72f) to colorScheme.onSecondaryContainer
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
private fun EmptyHosts(onClearSearch: () -> Unit) {
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

private fun Host.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.trim()
    return alias.contains(normalizedQuery, ignoreCase = true) ||
        address.contains(normalizedQuery, ignoreCase = true) ||
        user.contains(normalizedQuery, ignoreCase = true) ||
        tags.any { tag -> tag.contains(normalizedQuery, ignoreCase = true) }
}

@Preview(name = "Hosts phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HostListPhonePreview() {
    LiTermTheme {
        HostListScreen()
    }
}

@Preview(name = "Hosts tablet", showBackground = true, widthDp = 1000, heightDp = 720)
@Composable
private fun HostListTabletPreview() {
    LiTermTheme {
        HostListScreen()
    }
}
