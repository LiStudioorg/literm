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

package com.literm.listudioorg.ui.components

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
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.literm.listudioorg.R
import com.literm.listudioorg.data.local.Host

@Composable
fun AddHostDialog(
    onDismissRequest: () -> Unit,
    onSave: (Host) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var ip by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var privateKey by rememberSaveable { mutableStateOf("") }
    var attemptedSave by remember { mutableStateOf(false) }

    val nameValid = name.isNotBlank()
    val ipValid = ip.isNotBlank()
    val usernameValid = username.isNotBlank()
    val portNumber = port.toIntOrNull()
    val portValid = portNumber != null && portNumber in 1..65535
    val formValid = nameValid && ipValid && usernameValid && portValid

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
                        text = stringResource(R.string.add_host_title),
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

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_password)) },
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = privateKey,
                            onValueChange = { privateKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.add_host_private_key)) },
                            placeholder = {
                                Text(stringResource(R.string.add_host_private_key_hint))
                            },
                            minLines = 3,
                            maxLines = 6,
                            singleLine = false,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
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
                                            name = name.trim(),
                                            ip = ip.trim(),
                                            port = portNumber ?: 22,
                                            username = username.trim(),
                                            password = password.takeIf { it.isNotBlank() },
                                            privateKey = privateKey.takeIf { it.isNotBlank() },
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
}
