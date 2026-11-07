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

import com.literm.listudioorg.data.local.KeyDao
import com.literm.listudioorg.data.local.KeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 密钥（SSH 私钥）数据仓库，写操作统一切到 IO 线程。
 */
class KeyRepository(
    private val keyDao: KeyDao,
) {
    /** 密钥列表的可观察流。 */
    val keys: Flow<List<KeyEntity>> = keyDao.observeAll()

    /** 订阅单个密钥。 */
    fun observeKey(id: Long): Flow<KeyEntity?> = keyDao.observeById(id)

    /** 新增密钥，返回自增主键。 */
    suspend fun addKey(key: KeyEntity): Long = withContext(Dispatchers.IO) {
        keyDao.insert(key)
    }

    /** 整行更新。 */
    suspend fun updateKey(key: KeyEntity) = withContext(Dispatchers.IO) {
        keyDao.update(key)
    }

    suspend fun deleteKey(key: KeyEntity) = withContext(Dispatchers.IO) {
        keyDao.delete(key)
    }

    suspend fun deleteKey(id: Long) = withContext(Dispatchers.IO) {
        keyDao.deleteById(id)
    }
}
