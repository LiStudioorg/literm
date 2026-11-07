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

import android.database.Cursor
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull

/**
 * 一组空值安全的游标读取方法。
 *
 * 旧版本数据库可能缺少某些列或存有 NULL，直接 getInt/getString 会抛异常，
 * 因此所有 DAO 统一走这里的扩展函数读取字段。
 */

/** 读取字符串列，NULL 时返回 [default]。 */
internal fun Cursor.stringValue(columnName: String, default: String = ""): String =
    columnIndexOf(columnName).let { index ->
        if (isNull(index)) default else getString(index) ?: default
    }

/** 读取可空字符串列，NULL 时返回 null。 */
internal fun Cursor.stringValueOrNull(columnName: String): String? =
    columnIndexOf(columnName).let { index ->
        if (isNull(index)) null else getStringOrNull(index)
    }

/** 读取 Long 列，NULL 时返回 [default]。 */
internal fun Cursor.longValue(columnName: String, default: Long = 0L): Long =
    columnIndexOf(columnName).let { index ->
        if (isNull(index)) default else getLongOrNull(index) ?: default
    }

/** 读取 Int 列，NULL 时返回 [default]。 */
internal fun Cursor.intValue(columnName: String, default: Int = 0): Int =
    columnIndexOf(columnName).let { index ->
        if (isNull(index)) default else getIntOrNull(index) ?: default
    }

/** SQLite 用 0/1 存布尔值，这里统一转换。 */
internal fun Cursor.booleanValue(columnName: String, default: Boolean = false): Boolean =
    intValue(columnName, if (default) 1 else 0) != 0

/** 列名写错时立刻暴露，而不是静默读到错误数据。 */
private fun Cursor.columnIndexOf(columnName: String): Int {
    val index = getColumnIndex(columnName)
    check(index >= 0) { "结果集中不存在列：$columnName" }
    return index
}
