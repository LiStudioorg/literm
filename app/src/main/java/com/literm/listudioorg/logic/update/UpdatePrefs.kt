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

package com.literm.listudioorg.logic.update

import android.content.Context
import android.content.SharedPreferences

/**
 * 更新模块的本地偏好：下载加速源选择与自定义地址。
 *
 * 变更会立即写入 SharedPreferences（commit/apply 由调用方语义决定，这里使用 apply 实时持久化）。
 */
object UpdatePrefs {
    private const val PREFS_NAME = "literm_update_prefs"
    private const val KEY_MIRROR_MODE = "download_mirror_mode"
    private const val KEY_CUSTOM_MIRROR = "download_custom_mirror"

    /** 预置加速源（用于拼接：前缀 + 原始 APK 直链）。 */
    enum class MirrorMode(val label: String, val prefix: String) {
        DIRECT("直连", ""),
        GHPROXY_NET("https://ghproxy.net/", "https://ghproxy.net/"),
        GH_PROXY_COM("https://gh-proxy.com/", "https://gh-proxy.com/"),
        CUSTOM("自定义", ""),
        ;

        companion object {
            fun fromName(name: String?): MirrorMode =
                entries.firstOrNull { it.name == name } ?: DIRECT
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMirrorMode(context: Context): MirrorMode =
        MirrorMode.fromName(prefs(context).getString(KEY_MIRROR_MODE, MirrorMode.DIRECT.name))

    fun setMirrorMode(context: Context, mode: MirrorMode) {
        prefs(context).edit().putString(KEY_MIRROR_MODE, mode.name).apply()
    }

    fun getCustomMirror(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_MIRROR, "").orEmpty()

    fun setCustomMirror(context: Context, value: String) {
        prefs(context).edit().putString(KEY_CUSTOM_MIRROR, value).apply()
    }

    /**
     * 根据当前偏好计算最终下载地址。
     * - 直连：返回原始链接
     * - 预置镜像：`prefix + originalUrl`
     * - 自定义：将用户输入规范化后作为前缀（自动补 `/`），拼接原始链接；为空则回退直连
     */
    fun resolveDownloadUrl(context: Context, originalUrl: String): String {
        return when (val mode = getMirrorMode(context)) {
            MirrorMode.DIRECT -> originalUrl
            MirrorMode.CUSTOM -> {
                val raw = getCustomMirror(context).trim()
                if (raw.isBlank()) originalUrl else normalizePrefix(raw) + originalUrl
            }
            else -> mode.prefix + originalUrl
        }
    }

    private fun normalizePrefix(raw: String): String {
        val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
