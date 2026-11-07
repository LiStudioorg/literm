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

import android.content.Context

/**
 * 应用的 SQLite 数据库入口，取代原来的 RoomDatabase。
 *
 * 只负责持有 [LiTermSQLiteOpenHelper] 和表变更通知器，并把各张表的 DAO 暴露出去；
 * 真正的读写逻辑都在 DAO 里，DAO 的写方法是阻塞的，仓库层负责切到 IO 线程。
 */
class AppDatabase private constructor(context: Context) {

    private val openHelper = LiTermSQLiteOpenHelper(context)

    /** 写入后驱动 Flow 刷新的通知器，全库共用一个即可。 */
    private val notifier = TableChangeNotifier()

    val hostDao: HostDao by lazy { HostDao(openHelper, notifier) }

    val linuxInstanceDao: LinuxInstanceDao by lazy { LinuxInstanceDao(openHelper, notifier) }

    val keyDao: KeyDao by lazy { KeyDao(openHelper, notifier) }

    val snippetDao: SnippetDao by lazy { SnippetDao(openHelper, notifier) }

    /** 数据库文件路径，便于日志排查（`literm.db`）。 */
    fun databasePath(context: Context): String =
        context.getDatabasePath(LiTermSQLiteOpenHelper.DATABASE_NAME).absolutePath

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** 进程内单例，SQLiteOpenHelper 本身也是懒建库的。 */
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: AppDatabase(context.applicationContext)
                    .also { created -> instance = created }
            }
    }
}
