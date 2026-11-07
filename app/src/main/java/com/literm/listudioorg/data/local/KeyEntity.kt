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
 * 密钥页维护的 SSH 私钥，对应 SQLite 表 `ssh_keys`。
 */
data class KeyEntity(
    val id: Long = 0L,
    val name: String,
    // 按当前需求以明文保存完整私钥文本。
    val privateKey: String,
) {
    /** 私钥首个非空行，用于列表里的紧凑预览。 */
    val previewLine: String
        get() = privateKey.lineSequence()
            .map { line -> line.trim() }
            .firstOrNull { line -> line.isNotEmpty() }
            .orEmpty()

    /** 去掉首尾 `----` 标记后的类型提示，例如 `RSA PRIVATE KEY`。 */
    val keyTypeLabel: String
        get() = privateKey.lineSequence()
            .map { line -> line.trim() }
            .firstOrNull { line -> line.startsWith("-----") && line.endsWith("-----") }
            ?.trim('-')
            ?.takeIf { label -> label.isNotBlank() }
            ?: "PRIVATE KEY"
}
