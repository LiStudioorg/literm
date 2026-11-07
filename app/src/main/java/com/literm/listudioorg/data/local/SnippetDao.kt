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
 * 命令片段页面保存的可复用 Shell 命令读写层。方法都是阻塞的，调用方需要放到 IO 线程；
 * 下面的 [Flow] 查询内部已经切好线程。
 */
class SnippetDao(
    private val helper: SQLiteOpenHelper,
    private val notifier: TableChangeNotifier,
) {
    fun observeAll(): Flow<List<SnippetEntity>> = notifier.listFlow(TABLE) {
        helper.readableDatabase
            .query(TABLE, null, null, null, null, null, "${SnippetColumns.NAME} COLLATE NOCASE ASC")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toSnippetEntity())
                    }
                }
            }
    }

    fun observeById(id: Long): Flow<SnippetEntity?> = notifier.queryFlow(TABLE) {
        helper.readableDatabase.query(
            TABLE,
            null,
            "${SnippetColumns.ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toSnippetEntity() else null
        }
    }

    fun observeCount(): Flow<Int> = notifier.queryFlow(TABLE) { count() }

    fun count(): Int = helper.readableDatabase
        .rawQuery("SELECT COUNT(*) AS ${SnippetColumns.COUNT} FROM $TABLE", null)
        .use { cursor ->
            if (cursor.moveToFirst()) cursor.intValue(SnippetColumns.COUNT) else 0
        }

    fun insert(snippet: SnippetEntity): Long {
        val insertedId = helper.writableDatabase.insert(
            TABLE,
            SnippetColumns.ID,
            snippet.toContentValues(),
        )
        notifier.notifyTableChanged(TABLE)
        return insertedId
    }

    fun update(snippet: SnippetEntity) {
        helper.writableDatabase.update(
            TABLE,
            snippet.toContentValues(),
            "${SnippetColumns.ID} = ?",
            arrayOf(snippet.id.toString()),
        )
        notifier.notifyTableChanged(TABLE)
    }

    fun delete(snippet: SnippetEntity) {
        deleteById(snippet.id)
    }

    fun deleteById(id: Long) {
        helper.writableDatabase.delete(TABLE, "${SnippetColumns.ID} = ?", arrayOf(id.toString()))
        notifier.notifyTableChanged(TABLE)
    }

    companion object {
        const val TABLE = LiTermSQLiteOpenHelper.TABLE_SNIPPETS
    }
}

private object SnippetColumns {
    const val ID = "id"
    const val NAME = "name"
    const val COMMAND = "command"
    const val COUNT = "row_count"
}

private fun SnippetEntity.toContentValues(): ContentValues = ContentValues().apply {
    put(SnippetColumns.NAME, name)
    put(SnippetColumns.COMMAND, command)
}

private fun Cursor.toSnippetEntity(): SnippetEntity = SnippetEntity(
    id = longValue(SnippetColumns.ID),
    name = stringValue(SnippetColumns.NAME),
    command = stringValue(SnippetColumns.COMMAND),
)
