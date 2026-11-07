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
 * 已保存的 SSH 主机，对应 SQLite 表 `hosts`。
 */
data class Host(
    val id: Long = 0L,
    val name: String,
    val ip: String,
    val port: Int = 22,
    val username: String,
    // 按当前需求以明文保存。
    val password: String? = null,
    // 按当前需求以明文保存。
    val privateKey: String? = null,
)
