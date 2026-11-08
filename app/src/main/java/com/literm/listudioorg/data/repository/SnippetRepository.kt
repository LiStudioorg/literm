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

package com.literm.listudioorg.data.repository

import com.literm.listudioorg.data.local.SnippetDao
import com.literm.listudioorg.data.local.SnippetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 命令片段数据仓库，写操作统一切到 IO 线程。
 */
class SnippetRepository(
    private val snippetDao: SnippetDao,
) {
    /** 片段列表的可观察流。 */
    val snippets: Flow<List<SnippetEntity>> = snippetDao.observeAll()

    /** 订阅单个片段。 */
    fun observeSnippet(id: Long): Flow<SnippetEntity?> = snippetDao.observeById(id)

    /** 新增片段，返回自增主键。 */
    suspend fun addSnippet(snippet: SnippetEntity): Long = withContext(Dispatchers.IO) {
        snippetDao.insert(snippet)
    }

    /** 整行更新。 */
    suspend fun updateSnippet(snippet: SnippetEntity) = withContext(Dispatchers.IO) {
        snippetDao.update(snippet)
    }

    suspend fun deleteSnippet(snippet: SnippetEntity) = withContext(Dispatchers.IO) {
        snippetDao.delete(snippet)
    }
}
