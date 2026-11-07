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

package com.literm.listudioorg.data.repository

import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.local.HostDao
import kotlinx.coroutines.flow.Flow

class HostRepository(
    private val hostDao: HostDao,
) {
    val hosts: Flow<List<Host>> = hostDao.observeAll()

    fun observeHost(id: Long): Flow<Host?> = hostDao.observeById(id)

    suspend fun addHost(host: Host): Long = hostDao.insert(host)

    suspend fun updateHost(host: Host) {
        hostDao.update(host)
    }

    suspend fun deleteHost(host: Host) {
        hostDao.delete(host)
    }

    suspend fun deleteHost(id: Long) {
        hostDao.deleteById(id)
    }
}
