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

package com.literm.listudioorg.logic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literm.listudioorg.data.local.AppDatabase
import com.literm.listudioorg.data.local.LinuxInstance
import com.literm.listudioorg.data.local.LinuxInstanceDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LinuxViewModel(
    private val instanceDao: LinuxInstanceDao,
) : ViewModel() {
    private val _distros = MutableStateFlow(defaultLinuxDistros())
    val distros: StateFlow<List<LinuxDistro>> = _distros.asStateFlow()

    @Volatile
    private var installedIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            instanceDao.observeInstalledIds().collect { ids ->
                installedIds = ids.toSet()
                applyInstalledState(installedIds)
            }
        }
    }

    private fun applyInstalledState(installedIds: Set<String>) {
        updateDistros { distros ->
            distros.map { distro ->
                when {
                    distro.installStatus == LinuxInstallStatus.Downloading -> distro
                    distro.id in installedIds -> distro.copy(
                        installStatus = LinuxInstallStatus.Installed,
                        downloadProgress = 1f,
                    )
                    distro.installStatus == LinuxInstallStatus.Installed -> distro.copy(
                        installStatus = LinuxInstallStatus.NotInstalled,
                        downloadProgress = 0f,
                    )
                    else -> distro
                }
            }
        }
    }

    fun startDownload(context: Context, distroId: String) {
        val distro = _distros.value.firstOrNull { it.id == distroId } ?: return
        if (distro.installStatus != LinuxInstallStatus.NotInstalled) return
        if (distroId in installedIds) return

        updateDistro(distroId) { current ->
            current.copy(
                installStatus = LinuxInstallStatus.Downloading,
                downloadProgress = 0f,
            )
        }

        val applicationContext = context.applicationContext ?: context
        viewModelScope.launch {
            try {
                val archiveFile = DownloadHelper.download(
                    context = applicationContext,
                    distro = distro,
                    onProgress = { progress ->
                        updateDistro(distroId) { current ->
                            if (current.installStatus == LinuxInstallStatus.Downloading) {
                                current.copy(
                                    downloadProgress = progress.coerceIn(0f, 1f),
                                )
                            } else {
                                current
                            }
                        }
                    },
                )

                ExtractHelper.extract(
                    context = applicationContext,
                    distroId = distroId,
                    archiveFile = archiveFile,
                )

                instanceDao.upsert(
                    LinuxInstance(
                        id = distroId,
                        isInstalled = true,
                    ),
                )

                updateDistro(distroId) { current ->
                    current.copy(
                        installStatus = LinuxInstallStatus.Installed,
                        downloadProgress = 1f,
                    )
                }
            } catch (cancelled: CancellationException) {
                resetDownload(distroId)
                throw cancelled
            } catch (_: Exception) {
                resetDownload(distroId)
            }
        }
    }

    private fun resetDownload(distroId: String) {
        val stillInstalled = distroId in installedIds
        updateDistro(distroId) { current ->
            if (current.installStatus == LinuxInstallStatus.Downloading) {
                current.copy(
                    installStatus = if (stillInstalled) {
                        LinuxInstallStatus.Installed
                    } else {
                        LinuxInstallStatus.NotInstalled
                    },
                    downloadProgress = if (stillInstalled) 1f else 0f,
                )
            } else {
                current
            }
        }
    }

    private fun updateDistro(
        distroId: String,
        transform: (LinuxDistro) -> LinuxDistro,
    ) {
        updateDistros { distros ->
            distros.map { distro ->
                if (distro.id == distroId) transform(distro) else distro
            }
        }
    }

    private fun updateDistros(transform: (List<LinuxDistro>) -> List<LinuxDistro>) {
        _distros.update(transform)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                val database = AppDatabase.getInstance(application)
                LinuxViewModel(
                    instanceDao = database.linuxInstanceDao(),
                )
            }
        }
    }
}
