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

package com.literm.listudioorg.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literm.listudioorg.data.local.AppDatabase
import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.repository.HostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HostViewModel(
    private val repository: HostRepository,
) : ViewModel() {
    val hosts: StateFlow<List<Host>> = repository.hosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    fun addHost(host: Host) {
        viewModelScope.launch {
            repository.addHost(host)
        }
    }

    fun updateHost(host: Host) {
        viewModelScope.launch {
            repository.updateHost(host)
        }
    }

    fun deleteHost(host: Host) {
        viewModelScope.launch {
            repository.deleteHost(host)
        }
    }

    fun deleteHost(id: Long) {
        viewModelScope.launch {
            repository.deleteHost(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                val database = AppDatabase.getInstance(application)
                HostViewModel(
                    repository = HostRepository(database.hostDao()),
                )
            }
        }
    }
}
