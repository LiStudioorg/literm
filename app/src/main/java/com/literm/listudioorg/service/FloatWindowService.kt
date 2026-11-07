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

package com.literm.listudioorg.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.literm.listudioorg.R
import com.literm.listudioorg.logic.ActiveTerminal
import com.literm.listudioorg.logic.AppLogger
import com.literm.listudioorg.logic.FloatingShortcutsPrefs
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 悬浮球快捷按键服务（纯 View 实现，不依赖 Compose）。
 *
 * 默认只显示一个 ">_" 圆球，可拖动；点开后提供 Ctrl、Esc、Tab 与方向键，
 * 按键通过 [ActiveTerminal] 写入当前终端会话。需要“显示在其他应用上层”权限，
 * 进程存活由 [TerminalService] 前台服务保证，因此这里只是普通后台服务。
 */
class FloatWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var rootView: LinearLayout? = null
    private var panelView: LinearLayout? = null
    private var ctrlButton: TextView? = null

    /** Ctrl 是否处于按下待命状态：按下后再点其他键即组合键。 */
    private var ctrlArmed = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (rootView == null) {
            if (!Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            addOverlay()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        rootView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        rootView = null
        panelView = null
        ctrlButton = null
        super.onDestroy()
    }

    /** 挂载悬浮窗：外层竖向容器 = 圆球 + 默认隐藏的按键面板。 */
    private fun addOverlay() {
        if (rootView != null) return

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(220)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        val ball = buildBall()
        val panel = buildPanel()

        container.addView(ball)
        container.addView(panel)
        rootView = container
        panelView = panel

        try {
            windowManager.addView(container, layoutParams)
            attachDrag(ball)
        } catch (error: Exception) {
            AppLogger.log("Overlay", "无法添加悬浮窗（可能刚被撤销悬浮权限）", error)
            rootView = null
            panelView = null
            stopSelf()
        }
    }

    private fun buildBall(): TextView = TextView(this).apply {
        text = ">_"
        textSize = 16f
        typeface = Typeface.MONOSPACE
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        isClickable = true
        contentDescription = getString(R.string.float_window_open)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF2563EB.toInt())
            setStroke(dp(2), 0x66FFFFFF)
        }
    }

    private fun buildPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(0xE6181818.toInt())
                setStroke(dp(1), 0x33FFFFFF)
            }
        }

        val firstRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val ctrl = createKey(getString(R.string.float_window_ctrl)) { toggleCtrl() }
        ctrlButton = ctrl
        firstRow.addView(ctrl, keyLayoutParams())
        firstRow.addView(
            createKey(getString(R.string.float_window_esc)) { sendKey("\u001B") },
            keyLayoutParams(),
        )
        firstRow.addView(
            createKey(getString(R.string.float_window_tab)) { sendKey("\t") },
            keyLayoutParams(),
        )

        val secondRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        secondRow.addView(
            createKey(getString(R.string.float_window_left)) {
                sendKey("\u001B[D", "\u001B[1;5D")
            },
            keyLayoutParams(),
        )
        secondRow.addView(
            createKey(getString(R.string.float_window_up)) {
                sendKey("\u001B[A", "\u001B[1;5A")
            },
            keyLayoutParams(),
        )
        secondRow.addView(
            createKey(getString(R.string.float_window_down)) {
                sendKey("\u001B[B", "\u001B[1;5B")
            },
            keyLayoutParams(),
        )
        secondRow.addView(
            createKey(getString(R.string.float_window_right)) {
                sendKey("\u001B[C", "\u001B[1;5C")
            },
            keyLayoutParams(),
        )

        panel.addView(firstRow)
        panel.addView(secondRow)
        return panel
    }

    private fun createKey(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 14f
        typeface = Typeface.MONOSPACE
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = false
        background = keyBackground(false)
        setOnClickListener { onClick() }
    }

    private fun keyLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(48), dp(40)).apply {
            marginEnd = dp(4)
            bottomMargin = dp(4)
        }

    private fun keyBackground(active: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(if (active) 0xFF2563EB.toInt() else 0xFF2C2C2C.toInt())
            setStroke(dp(1), if (active) 0xFFFFFFFF.toInt() else 0x33FFFFFF)
        }

    /**
     * 圆球的拖拽手势：位移超过 touchSlop 才算拖动，
     * 否则抬手时视为点击，展开或收起按键面板。
     */
    private fun attachDrag(view: View) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var startTouchX = 0f
        var startTouchY = 0f
        var dragging = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        moveTo(startX + dx, startY + dy)
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    if (!dragging && event.actionMasked == MotionEvent.ACTION_UP) {
                        togglePanel()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun togglePanel() {
        val panel = panelView ?: return
        val show = panel.visibility != View.VISIBLE
        panel.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            resetCtrl()
        }
        rootView?.post { moveTo(layoutParams.x, layoutParams.y) }
    }

    private fun toggleCtrl() {
        ctrlArmed = !ctrlArmed
        ctrlButton?.background = keyBackground(ctrlArmed)
    }

    private fun resetCtrl() {
        ctrlArmed = false
        ctrlButton?.background = keyBackground(false)
    }

    private fun sendKey(base: String, ctrl: String = base) {
        val sequence = if (ctrlArmed) ctrl else base
        resetCtrl()
        if (!ActiveTerminal.sendRawInput(sequence)) {
            Toast.makeText(this, R.string.float_window_no_session, Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveTo(x: Int, y: Int) {
        val root = rootView ?: return
        val width = root.width.takeIf { it > 0 } ?: dp(240)
        val height = root.height.takeIf { it > 0 } ?: dp(64)
        val metrics = resources.displayMetrics
        layoutParams.x = x.coerceIn(0, (metrics.widthPixels - width).coerceAtLeast(0))
        layoutParams.y = y.coerceIn(0, (metrics.heightPixels - height).coerceAtLeast(0))
        windowManager.updateViewLayout(root, layoutParams)
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, FloatWindowService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatWindowService::class.java))
        }

        fun startIfEnabled(context: Context) {
            if (
                FloatingShortcutsPrefs.isEnabled(context) &&
                Settings.canDrawOverlays(context)
            ) {
                start(context)
            }
        }
    }
}
