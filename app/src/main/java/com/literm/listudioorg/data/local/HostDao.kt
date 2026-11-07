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

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.Flow

/**
 * 已保存 SSH 主机的原生 SQLite 读写层。
 *
 * 写方法都是阻塞的，调用方（仓库层）需要放到 IO 线程执行；
 * [Flow] 查询内部已经切到 IO 线程，可以直接在 ViewModel 里订阅。
 */
class HostDao(
    private val helper: SQLiteOpenHelper,
    private val notifier: TableChangeNotifier,
) {
    /** 按别名排序订阅全部主机。 */
    fun observeAll(): Flow<List<Host>> = notifier.listFlow(TABLE) { selectAll() }

    /** 订阅单台主机，删除后会发出 null。 */
    fun observeById(id: Long): Flow<Host?> = notifier.queryFlow(TABLE) { selectById(id) }

    /** 新增主机，返回自增主键。 */
    fun insert(host: Host): Long {
        val insertedId = helper.writableDatabase.insert(TABLE, HostColumns.ID, host.toContentValues())
        notifier.notifyTableChanged(TABLE)
        return insertedId
    }

    /** 没有主键就插入，有主键则整行替换，行为与旧的 REPLACE 一致。 */
    fun upsert(host: Host): Long {
        if (host.id <= 0L) {
            return insert(host)
        }

        val values = host.toContentValues()
        values.put(HostColumns.ID, host.id)
        helper.writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        notifier.notifyTableChanged(TABLE)
        return host.id
    }

    /** 按主键整行更新。 */
    fun update(host: Host) {
        helper.writableDatabase.update(
            TABLE,
            host.toContentValues(),
            "${HostColumns.ID} = ?",
            arrayOf(host.id.toString()),
        )
        notifier.notifyTableChanged(TABLE)
    }

    fun delete(host: Host) {
        deleteById(host.id)
    }

    /** 按主键删除。 */
    fun deleteById(id: Long) {
        helper.writableDatabase.delete(TABLE, "${HostColumns.ID} = ?", arrayOf(id.toString()))
        notifier.notifyTableChanged(TABLE)
    }

    private fun selectAll(): List<Host> = helper.readableDatabase.query(
        TABLE,
        null,
        null,
        null,
        null,
        null,
        "${HostColumns.NAME} COLLATE NOCASE ASC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(cursor.toHost())
            }
        }
    }

    private fun selectById(id: Long): Host? = helper.readableDatabase.query(
        TABLE,
        null,
        "${HostColumns.ID} = ?",
        arrayOf(id.toString()),
        null,
        null,
        null,
        "1",
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.toHost() else null
    }

    companion object {
        const val TABLE = LiTermSQLiteOpenHelper.TABLE_HOSTS
    }
}

/** hosts 表的列名，避免手写字符串散落各处。 */
private object HostColumns {
    const val ID = "id"
    const val NAME = "name"
    const val IP = "ip"
    const val PORT = "port"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val PRIVATE_KEY = "privateKey"
}

/** 把主机对象转成写入用的 ContentValues；主键由 SQLite 自增生成。 */
private fun Host.toContentValues(): ContentValues = ContentValues().apply {
    put(HostColumns.NAME, name)
    put(HostColumns.IP, ip)
    put(HostColumns.PORT, port)
    put(HostColumns.USERNAME, username)
    put(HostColumns.PASSWORD, password)
    put(HostColumns.PRIVATE_KEY, privateKey)
}

/** 把当前游标行映射成 [Host]。 */
private fun Cursor.toHost(): Host = Host(
    id = longValue(HostColumns.ID),
    name = stringValue(HostColumns.NAME),
    ip = stringValue(HostColumns.IP),
    port = intValue(HostColumns.PORT, default = 22),
    username = stringValue(HostColumns.USERNAME),
    password = stringValueOrNull(HostColumns.PASSWORD),
    privateKey = stringValueOrNull(HostColumns.PRIVATE_KEY),
)
