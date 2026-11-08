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

import com.literm.listudioorg.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val body: String,
    val apkUrl: String?,
    val htmlUrl: String?,
    val isForceUpdate: Boolean,
) {
    /** 去掉前导 v/V 后的版本号，用于展示/比较。 */
    val versionNameNormalized: String
        get() = tagName.removePrefix("v").removePrefix("V").trim()
}

/**
 * 检查 GitHub Releases 最新版本。
 *
 * - 主接口：api.github.com，超时 5 秒
 * - 失败/超时后自动切换备用：api.kkgithub.com
 * - body 包含 `[ForceUpdate]` 则视为强制更新
 */
object UpdateManager {
    private const val REPO = "LiStudioorg/literm"
    private const val PRIMARY_API = "https://api.github.com/repos/$REPO/releases/latest"
    private const val FALLBACK_API = "https://api.kkgithub.com/repos/$REPO/releases/latest"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    sealed class CheckResult {
        data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : CheckResult()
        data class UpToDate(val currentVersion: String, val latestTag: String) : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    suspend fun checkForUpdate(): CheckResult = withContext(Dispatchers.IO) {
        val current = runCatching { BuildConfig.VERSION_NAME }.getOrDefault("")
        val release = fetchRelease(PRIMARY_API) ?: fetchRelease(FALLBACK_API)
            ?: return@withContext CheckResult.Error("无法连接更新服务器，请稍后重试")

        val latest = release.versionNameNormalized
        val hasUpdate = isNewerVersion(latest, current)
        if (hasUpdate) {
            CheckResult.UpdateAvailable(release, current)
        } else {
            CheckResult.UpToDate(current, release.tagName)
        }
    }

    private fun fetchRelease(url: String): ReleaseInfo? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "LiTerm-Android/${runCatching { BuildConfig.VERSION_NAME }.getOrDefault("unknown")}")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            val bodyStr = resp.body?.string().orEmpty()
            if (bodyStr.isBlank()) return@runCatching null
            parseRelease(JSONObject(bodyStr))
        }
    }.getOrNull()

    private fun parseRelease(obj: JSONObject): ReleaseInfo? {
        val tag = obj.optString("tag_name").orEmpty()
        if (tag.isBlank()) return null
        val body = obj.optString("body").orEmpty()
        val htmlUrl = obj.optString("html_url").takeIf { it.isNotBlank() }
        val apkUrl = findApkUrl(obj)
        val force = body.contains("[ForceUpdate]", ignoreCase = true)
        return ReleaseInfo(
            tagName = tag,
            body = body,
            apkUrl = apkUrl,
            htmlUrl = htmlUrl,
            isForceUpdate = force,
        )
    }

    private fun findApkUrl(obj: JSONObject): String? {
        val assets = obj.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val name = a.optString("name").orEmpty()
            val url = a.optString("browser_download_url").orEmpty()
            if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("http")) {
                return url
            }
        }
        // fallback: any asset url
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val url = a.optString("browser_download_url").orEmpty()
            if (url.startsWith("http")) return url
        }
        return null
    }

    /**
     * 简单语义版本比较：按 `.`/`-` 分段的数字前缀比较；latest > current 返回 true。
     * 无法解析时回退为字符串不等即视为有更新（保守）。
     */
    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val l = normalize(latest)
        val c = normalize(current)
        if (l.isBlank() || c.isBlank()) return latest.trim() != current.trim() && latest.isNotBlank()
        val lp = l.split('.', '-', '_')
        val cp = c.split('.', '-', '_')
        val n = maxOf(lp.size, cp.size)
        for (i in 0 until n) {
            val li = lp.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            val ci = cp.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            if (li != ci) return li > ci
        }
        return false
    }

    private fun normalize(v: String): String = v.trim().removePrefix("v").removePrefix("V")
}
