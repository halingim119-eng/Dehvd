package com.webtoon.helper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class AutoService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var bar: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var autoNext = true
    private var speed = 5 // 1..10
    private lateinit var startBtn: TextView
    private lateinit var nextBtn: TextView

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBar()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() { running = false }

    override fun onDestroy() {
        running = false
        bar?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun showBar() {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(120) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.argb(190, 20, 20, 20)); cornerRadius = dp(20).toFloat()
            }
        }
        fun tv(t: String) = TextView(this).apply {
            text = t; setTextColor(Color.WHITE); textSize = 13f
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val handle = tv("≡").apply { textSize = 18f }
        startBtn = tv("▶ 시작")
        nextBtn = tv("자동다음화 ON")
        val seek = SeekBar(this).apply {
            max = 9; progress = speed - 1
            layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { speed = p + 1 }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        root.addView(handle); root.addView(startBtn); root.addView(nextBtn); root.addView(seek)

        // drag
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = ev.rawX; ty = ev.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = sx + (ev.rawX - tx).toInt(); lp.y = sy + (ev.rawY - ty).toInt()
                    wm.updateViewLayout(root, lp)
                }
            }
            true
        }
        startBtn.setOnClickListener {
            running = !running
            startBtn.text = if (running) "■ 정지" else "▶ 시작"
            if (running) handler.post(tick)
        }
        nextBtn.setOnClickListener {
            autoNext = !autoNext
            nextBtn.text = if (autoNext) "자동다음화 ON" else "자동다음화 OFF"
        }
        bar = root
        wm.addView(root, lp)
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (autoNext && clickNext()) { handler.postDelayed(this, 1800); return }
            swipe()
        }
    }

    private fun swipe() {
        val m = resources.displayMetrics
        val x = m.widthPixels * 0.5f
        val y0 = m.heightPixels * 0.75f
        val dist = speed * 50f
        val path = Path().apply { moveTo(x, y0); lineTo(x, (y0 - dist).coerceAtLeast(50f)) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250)).build()
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { if (running) handler.post(tick) }
            override fun onCancelled(d: GestureDescription?) { if (running) handler.postDelayed(tick, 200) }
        }, null)
        if (!ok && running) handler.postDelayed(tick, 300)
    }

    private fun clickNext(): Boolean {
        val root = rootInActiveWindow ?: return false
        for (key in listOf("다음화 보기", "다음화보기")) {
            for (n in root.findAccessibilityNodeInfosByText(key)) {
                if (!n.isVisibleToUser) continue
                var c: AccessibilityNodeInfo? = n
                while (c != null && !c.isClickable) c = c.parent
                if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                val r = Rect(); n.getBoundsInScreen(r)
                if (!r.isEmpty) { tap(r.centerX().toFloat(), r.centerY().toFloat()); return true }
            }
        }
        return false
    }

    private fun tap(x: Float, y: Float) {
        val p = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 80)).build(), null, null)
    }
}
