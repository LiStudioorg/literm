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
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.Flow

/**
 * Keychain 页面管理的 SSH 私钥读写层。方法都是阻塞的，调用方需要放到 IO 线程；
 * 下面的 [Flow] 查询内部已经切好线程。
 */
class KeyDao(
    private val helper: SQLiteOpenHelper,
    private val notifier: TableChangeNotifier,
) {
    fun observeAll(): Flow<List<KeyEntity>> = notifier.listFlow(TABLE) {
        helper.readableDatabase
            .query(TABLE, null, null, null, null, null, "${KeyColumns.NAME} COLLATE NOCASE ASC")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toKeyEntity())
                    }
                }
            }
    }

    fun observeById(id: Long): Flow<KeyEntity?> = notifier.queryFlow(TABLE) {
        helper.readableDatabase.query(
            TABLE,
            null,
            "${KeyColumns.ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toKeyEntity() else null
        }
    }

    fun observeCount(): Flow<Int> = notifier.queryFlow(TABLE) { count() }

    fun count(): Int = helper.readableDatabase
        .rawQuery("SELECT COUNT(*) AS ${KeyColumns.COUNT} FROM $TABLE", null)
        .use { cursor ->
            if (cursor.moveToFirst()) cursor.intValue(KeyColumns.COUNT) else 0
        }

    fun insert(key: KeyEntity): Long {
        val insertedId = helper.writableDatabase.insert(TABLE, KeyColumns.ID, key.toContentValues())
        notifier.notifyTableChanged(TABLE)
        return insertedId
    }

    fun update(key: KeyEntity) {
        helper.writableDatabase.update(
            TABLE,
            key.toContentValues(),
            "${KeyColumns.ID} = ?",
            arrayOf(key.id.toString()),
        )
        notifier.notifyTableChanged(TABLE)
    }

    fun delete(key: KeyEntity) {
        deleteById(key.id)
    }

    fun deleteById(id: Long) {
        helper.writableDatabase.delete(TABLE, "${KeyColumns.ID} = ?", arrayOf(id.toString()))
        notifier.notifyTableChanged(TABLE)
    }

    companion object {
        const val TABLE = LiTermSQLiteOpenHelper.TABLE_KEYS
    }
}

private object KeyColumns {
    const val ID = "id"
    const val NAME = "name"
    const val PRIVATE_KEY = "privateKey"
    const val COUNT = "row_count"
}

private fun KeyEntity.toContentValues(): ContentValues = ContentValues().apply {
    put(KeyColumns.NAME, name)
    put(KeyColumns.PRIVATE_KEY, privateKey)
}

private fun Cursor.toKeyEntity(): KeyEntity = KeyEntity(
    id = longValue(KeyColumns.ID),
    name = stringValue(KeyColumns.NAME),
    privateKey = stringValue(KeyColumns.PRIVATE_KEY),
)
