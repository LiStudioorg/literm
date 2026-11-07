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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 原生 SQLite 建表与升级入口。
 *
 * 表名和列名与此前 Room 版本完全一致（`literm.db`，版本 3），
 * 因此老用户从版本 1、2 升级上来时数据可以无损保留。
 */
internal class LiTermSQLiteOpenHelper(
    context: Context,
) : SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    /** 首次创建数据库时建立全部表。 */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_HOSTS_TABLE)
        db.execSQL(CREATE_LINUX_INSTANCES_TABLE)
        db.execSQL(CREATE_KEYS_TABLE)
        db.execSQL(CREATE_SNIPPETS_TABLE)
    }

    /**
     * 逐版本向前累加式升级：
     * 1 -> 2 增加本地 Linux 实例表；2 -> 3 增加密钥表与命令片段表。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion
        if (version < 2) {
            db.execSQL(CREATE_LINUX_INSTANCES_TABLE)
            version = 2
        }
        if (version < 3) {
            db.execSQL(CREATE_KEYS_TABLE)
            db.execSQL(CREATE_SNIPPETS_TABLE)
            version = 3
        }
    }

    /** 升级失败时重建表结构，保证应用还能继续启动。 */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS `$TABLE_HOSTS`")
        db.execSQL("DROP TABLE IF EXISTS `$TABLE_LINUX_INSTANCES`")
        db.execSQL("DROP TABLE IF EXISTS `$TABLE_KEYS`")
        db.execSQL("DROP TABLE IF EXISTS `$TABLE_SNIPPETS`")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "literm.db"
        const val DATABASE_VERSION = 3

        const val TABLE_HOSTS = "hosts"
        const val TABLE_LINUX_INSTANCES = "linux_instances"
        const val TABLE_KEYS = "ssh_keys"
        const val TABLE_SNIPPETS = "snippets"

        private const val CREATE_HOSTS_TABLE = """
            CREATE TABLE IF NOT EXISTS `hosts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `ip` TEXT NOT NULL,
                `port` INTEGER NOT NULL,
                `username` TEXT NOT NULL,
                `password` TEXT,
                `privateKey` TEXT
            )
        """

        private const val CREATE_LINUX_INSTANCES_TABLE = """
            CREATE TABLE IF NOT EXISTS `linux_instances` (
                `id` TEXT NOT NULL,
                `isInstalled` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """

        private const val CREATE_KEYS_TABLE = """
            CREATE TABLE IF NOT EXISTS `ssh_keys` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `privateKey` TEXT NOT NULL
            )
        """

        private const val CREATE_SNIPPETS_TABLE = """
            CREATE TABLE IF NOT EXISTS `snippets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `command` TEXT NOT NULL
            )
        """
    }
}
