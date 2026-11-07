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

/**
 * 可复用的命令片段，对应 SQLite 表 `snippets`。
 */
data class SnippetEntity(
    val id: Long = 0L,
    val name: String,
    val command: String,
) {
    /** 把多行命令折叠成单行，卡片展示时更紧凑。 */
    val singleLineCommand: String
        get() = command.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .joinToString(separator = " && ")

    /** 命令行数，用于卡片上的元信息。 */
    val lineCount: Int
        get() = command.lineSequence().count { line -> line.isNotBlank() }
}
