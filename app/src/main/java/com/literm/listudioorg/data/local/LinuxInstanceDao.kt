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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.Flow

/**
 * 记录哪些 Linux 发行版已经安装，供本地 PRoot 终端使用。
 */
class LinuxInstanceDao(
    private val helper: SQLiteOpenHelper,
    private val notifier: TableChangeNotifier,
) {
    /** 订阅已安装的发行版 id 列表。 */
    fun observeInstalledIds(): Flow<List<String>> = notifier.listFlow(TABLE) {
        helper.readableDatabase.query(
            TABLE,
            arrayOf(LinuxInstanceColumns.ID),
            "${LinuxInstanceColumns.INSTALLED} = 1",
            null,
            null,
            null,
            "${LinuxInstanceColumns.ID} ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.stringValue(LinuxInstanceColumns.ID))
                }
            }
        }
    }

    /** 存在则覆盖，不存在则插入。 */
    fun upsert(instance: LinuxInstance) {
        helper.writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            ContentValues().apply {
                put(LinuxInstanceColumns.ID, instance.id)
                put(LinuxInstanceColumns.INSTALLED, if (instance.isInstalled) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        notifier.notifyTableChanged(TABLE)
    }

    /** 卸载时移除安装记录。 */
    fun deleteById(id: String) {
        helper.writableDatabase.delete(
            TABLE,
            "${LinuxInstanceColumns.ID} = ?",
            arrayOf(id),
        )
        notifier.notifyTableChanged(TABLE)
    }

    companion object {
        const val TABLE = LiTermSQLiteOpenHelper.TABLE_LINUX_INSTANCES
    }
}

/** linux_instances 表的列名。 */
private object LinuxInstanceColumns {
    const val ID = "id"
    const val INSTALLED = "isInstalled"
}
