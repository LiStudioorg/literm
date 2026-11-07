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

package com.literm.listudioorg

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.literm.listudioorg.logic.FloatingShortcutsPrefs
import com.literm.listudioorg.service.FloatWindowService
import com.literm.listudioorg.ui.screens.MainScreen
import com.literm.listudioorg.ui.theme.LiTermTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LiTermTheme {
                MainScreen()
            }
        }
    }

    /**
     * 每次回到前台都按设置项同步悬浮球：
     * 开关打开且有悬浮窗权限就启动服务，否则停止，避免出现“开着但没权限”的空转服务。
     */
    override fun onResume() {
        super.onResume()
        syncFloatingWindow()
    }

    private fun syncFloatingWindow() {
        if (!FloatingShortcutsPrefs.isEnabled(this)) {
            FloatWindowService.stop(this)
            return
        }

        if (Settings.canDrawOverlays(this)) {
            FloatWindowService.start(this)
        } else {
            // 权限被系统撤销时回退开关，并引导用户去系统设置重新授权。
            FloatingShortcutsPrefs.setEnabled(this, false)
            FloatWindowService.stop(this)
            requestOverlayPermission()
        }
    }

    /** 跳转系统的“显示在其他应用上层”授权页；用户拒绝也不会影响终端功能。 */
    private fun requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) return

        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName"),
                ),
            )
        }
    }
}
