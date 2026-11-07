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
import com.literm.listudioorg.data.local.SnippetEntity
import com.literm.listudioorg.data.repository.SnippetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 命令片段页（Snippets）的界面状态与增删改操作。
 */
class SnippetViewModel(
    private val repository: SnippetRepository,
) : ViewModel() {
    /** 片段列表，页面退订 5 秒后自动停止查询。 */
    val snippets: StateFlow<List<SnippetEntity>> = repository.snippets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    /** 新增片段；名称或命令为空时直接忽略。 */
    fun addSnippet(name: String, command: String) {
        val trimmedName = name.trim()
        val trimmedCommand = command.trim()
        if (trimmedName.isEmpty() || trimmedCommand.isEmpty()) {
            return
        }

        viewModelScope.launch {
            repository.addSnippet(
                SnippetEntity(
                    name = trimmedName,
                    command = trimmedCommand,
                ),
            )
        }
    }

    fun updateSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.updateSnippet(snippet)
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                )
                val database = AppDatabase.getInstance(application)
                SnippetViewModel(
                    repository = SnippetRepository(database.snippetDao),
                )
            }
        }
    }
}
