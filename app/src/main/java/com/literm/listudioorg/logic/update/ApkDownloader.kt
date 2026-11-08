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

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.literm.listudioorg.R
import java.io.File

/**
 * APK 下载与安装。
 *
 * - 下载统一交给系统 [DownloadManager]，强制落到公共「下载」目录
 *   （[Environment.DIRECTORY_DOWNLOADS]），方便用户在文件管理器里再次安装。
 * - 下载完成后通过 [FileProvider] 生成 content:// URI 调起系统安装界面，
 *   适配 Android 7.0+ 的 FileProvider 要求与 Android 8.0+ 的“未知来源”授权要求。
 */
object ApkDownloader {
    const val MIME_APK = "application/vnd.android.package-archive"

    /** 下载状态回调，便于 UI 提示。 */
    fun interface StatusListener {
        fun onStatus(message: String)
    }

    /**
     * 开始下载并在完成后调起安装。
     *
     * @param apkUrl 已经拼好加速前缀的最终下载地址
     * @param fileName 保存到公共下载目录的文件名
     */
    fun startDownload(
        context: Context,
        apkUrl: String,
        fileName: String,
        listener: StatusListener? = null,
    ) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (manager == null) {
            listener?.onStatus(appContext.getString(R.string.update_download_failed))
            return
        }

        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }
        val target = File(publicDir, fileName)
        if (target.isFile) {
            runCatching { target.delete() }
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle(appContext.getString(R.string.app_name))
            setDescription(fileName)
            setMimeType(MIME_APK)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // 强制保存到手机公共下载目录。
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            addRequestHeader("User-Agent", "LiTerm-Android")
        }

        val downloadId = runCatching { manager.enqueue(request) }.getOrNull()
        if (downloadId == null) {
            listener?.onStatus(appContext.getString(R.string.update_download_failed))
            return
        }

        registerCompletionReceiver(appContext, manager, downloadId, target, listener)
        listener?.onStatus(appContext.getString(R.string.update_download_started))
    }

    private fun registerCompletionReceiver(
        appContext: Context,
        manager: DownloadManager,
        downloadId: Long,
        target: File,
        listener: StatusListener?,
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (completedId != downloadId) return

                runCatching { appContext.unregisterReceiver(this) }

                val cursor = runCatching {
                    manager.query(DownloadManager.Query().setFilterById(downloadId))
                }.getOrNull()

                var successful = false
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                            successful = status == DownloadManager.STATUS_SUCCESSFUL
                        }
                    } finally {
                        runCatching { cursor.close() }
                    }
                }

                if (!successful && target.isFile) {
                    successful = true
                }

                if (!successful) {
                    listener?.onStatus(appContext.getString(R.string.update_download_failed))
                    return
                }

                val installUri = resolveInstallUri(appContext, manager, downloadId, target)
                if (installUri == null) {
                    listener?.onStatus(appContext.getString(R.string.update_install_uri_failed))
                    return
                }
                launchInstall(appContext, installUri, listener)
            }
        }

        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                // DownloadManager 的完成广播由系统发出，必须声明 EXPORTED。
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.onFailure {
            listener?.onStatus(appContext.getString(R.string.update_download_failed))
        }
    }

    private fun resolveInstallUri(
        context: Context,
        manager: DownloadManager,
        downloadId: Long,
        target: File,
    ): Uri? {
        // 优先走 FileProvider（file_paths.xml 已声明公共 Download 目录）。
        if (target.isFile) {
            val providerUri = runCatching {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
            }.getOrNull()
            if (providerUri != null) return providerUri
        }
        return runCatching { manager.getUriForDownloadedFile(downloadId) }.getOrNull()
    }

    /**
     * 调起系统安装界面。Android 8.0+ 需要先获得“安装未知应用”授权，
     * 未授权时跳转对应设置页并提示用户回来重试。
     */
    fun launchInstall(
        context: Context,
        apkUri: Uri,
        listener: StatusListener? = null,
    ) {
        if (!ensureInstallPermission(context)) {
            listener?.onStatus(context.getString(R.string.update_install_permission_needed))
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val launched = runCatching { context.startActivity(intent) }.isSuccess
        if (launched) return

        // 少数机型不认 ACTION_VIEW，回退 ACTION_INSTALL_PACKAGE。
        val fallback = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackLaunched = runCatching { context.startActivity(fallback) }.isSuccess
        if (!fallbackLaunched) {
            listener?.onStatus(context.getString(R.string.update_install_failed))
        }
    }

    /** 从本地选择的 APK（content:// URI）直接安装。 */
    fun installLocalApk(
        context: Context,
        apkUri: Uri,
        listener: StatusListener? = null,
    ) {
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                apkUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        if (!granted) {
            // 一次性授权即可安装，读取失败不阻断后续流程。
        }

        if (!ensureInstallPermission(context)) {
            listener?.onStatus(context.getString(R.string.update_install_permission_needed))
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(intent) }.isSuccess
        if (launched) return

        val fallback = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!runCatching { context.startActivity(fallback) }.isSuccess) {
            listener?.onStatus(context.getString(R.string.update_install_failed))
        }
    }

    /**
     * Android 8.0+ 需要“安装未知应用”权限。已授权返回 true；
     * 未授权则跳转设置页并返回 false。
     */
    private fun ensureInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

        val canInstall = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(true)
        if (canInstall) return true

        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(settingsIntent) }
        return false
    }

    /** 公共下载目录里已有的 APK，便于提示用户。 */
    fun publicDownloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}
