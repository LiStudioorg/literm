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

package com.literm.listudioorg.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.literm.listudioorg.data.local.AppDatabase
import com.literm.listudioorg.data.local.KeyEntity
import com.literm.listudioorg.data.repository.KeyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 密钥页（Keychain）的界面状态与增删改操作。
 */
class KeyViewModel(
    private val repository: KeyRepository,
) : ViewModel() {
    /** 密钥列表，页面退订 5 秒后自动停止查询。 */
    val keys: StateFlow<List<KeyEntity>> = repository.keys.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    /** 新增密钥；名称或私钥为空时直接忽略。 */
    fun addKey(name: String, privateKey: String) {
        val trimmedName = name.trim()
        val trimmedKey = privateKey.trim()
        if (trimmedName.isEmpty() || trimmedKey.isEmpty()) {
            return
        }

        viewModelScope.launch {
            repository.addKey(
                KeyEntity(
                    name = trimmedName,
                    privateKey = trimmedKey,
                ),
            )
        }
    }

    /** 重命名等整行更新。 */
    fun updateKey(key: KeyEntity) {
        viewModelScope.launch {
            repository.updateKey(key)
        }
    }

    fun deleteKey(key: KeyEntity) {
        viewModelScope.launch {
            repository.deleteKey(key)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                val database = AppDatabase.getInstance(application)
                KeyViewModel(
                    repository = KeyRepository(database.keyDao),
                )
            }
        }
    }
}
