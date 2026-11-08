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

import com.literm.listudioorg.data.local.Host
import com.literm.listudioorg.data.local.HostDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 主机数据仓库。
 *
 * DAO 的写方法是阻塞的，这里统一切到 IO 线程，ViewModel 只面对挂起函数。
 */
class HostRepository(
    private val hostDao: HostDao,
) {
    /** 主机列表的可观察流，查询已在 DAO 内部切到 IO 线程。 */
    val hosts: Flow<List<Host>> = hostDao.observeAll()

    /** 订阅单台主机。 */
    fun observeHost(id: Long): Flow<Host?> = hostDao.observeById(id)

    /** 新增主机，返回自增主键。 */
    suspend fun addHost(host: Host): Long = withContext(Dispatchers.IO) {
        hostDao.insert(host)
    }

    /** 整行更新。 */
    suspend fun updateHost(host: Host) = withContext(Dispatchers.IO) {
        hostDao.update(host)
    }

    suspend fun deleteHost(host: Host) = withContext(Dispatchers.IO) {
        hostDao.delete(host)
    }
}
