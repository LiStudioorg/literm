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

package com.literm.listudioorg.data.local

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update

/**
 * 把原生 SQLite 的写入变成 Kotlin [Flow]，用来替代 Room 的自动可观察查询。
 *
 * SQLite 自身不会推送变更事件，所以每个 DAO 在写完数据后要主动调用 [notifyTableChanged]，
 * 订阅方收到通知就在 IO 线程重跑一次查询。进程内单例注册表足够当前应用使用。
 *
 * 注意：本类作为各 DAO（public）构造函数的参数类型出现，
 * 不能声明为 internal，否则整个 app 模块会因“public 签名暴露 internal 类型”编译失败。
 */
class TableChangeNotifier(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** 每张表一个版本号，写入时自增，作为“数据变了”的信号。 */
    private val versions = ConcurrentHashMap<String, MutableStateFlow<Long>>()

    /**
     * 订阅时先查询一次，之后每当 [table] 发生变更就重新执行 [query]。
     *
     * 先记录版本号再执行查询：如果写入通知正好落在“查询完成”与“开始等待通知”之间，
     * 也能通过 `it > seen` 立刻察觉并补一次查询，不会漏掉这次刷新。
     */
    fun <T> queryFlow(
        table: String,
        query: () -> T,
    ): Flow<T> = flow {
        val version = versionFlow(table)
        var seen = version.value
        while (true) {
            emit(query())
            // 等待该表的下一次写入；StateFlow 会重放最新值，因此多次写入只会补一次查询。
            seen = version.first { current -> current > seen }
        }
    }.flowOn(ioDispatcher).distinctUntilChanged()

    /** 列表查询的快捷写法，调用处不必再传空列表。 */
    fun <R> listFlow(
        table: String,
        query: () -> List<R>,
    ): Flow<List<R>> = queryFlow(table, query)

    /** 通知某张表的数据已变化，触发该表所有订阅重新查询。 */
    fun notifyTableChanged(table: String) {
        versionFlow(table).update { current -> current + 1L }
    }

    private fun versionFlow(table: String): MutableStateFlow<Long> =
        versions.getOrPut(table) { MutableStateFlow(0L) }
}
