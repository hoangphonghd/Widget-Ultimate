package com.magicos.widget

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

open class UiBase : Activity() {
    val ui = Handler(Looper.getMainLooper())
    val isDark: Boolean get() = Render.isNight(this)
    fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt()

    val BG: Int get() = if (isDark) 0xFF0F1320.toInt() else 0xFFF4F6FB.toInt()
    val NAV: Int get() = if (isDark) 0xFF151A2B.toInt() else Color.WHITE
    val CARD: Int get() = if (isDark) 0xFF181E30.toInt() else Color.WHITE
    val TILE: Int get() = if (isDark) 0xFF222A42.toInt() else 0xFFF1F4FA.toInt()
    val INK: Int get() = if (isDark) 0xFFE8ECF6.toInt() else 0xFF111827.toInt()
    val MUTED: Int get() = if (isDark) 0xFF9AA4BC.toInt() else 0xFF6B7280.toInt()
    val LINE: Int get() = if (isDark) 0xFF2A3350.toInt() else 0xFFE3E7F0.toInt()
    val SEL: Int get() = if (isDark) 0xFF26325A.toInt() else 0xFFE8EDFF.toInt()
    val ACC: Int get() = if (isDark) 0xFF7C9BFF.toInt() else 0xFF4A6CF7.toInt()
    val GOOD: Int get() = if (isDark) 0xFF4ADE80.toInt() else 0xFF16A34A.toInt()
    val BAD: Int get() = if (isDark) 0xFFF87171.toInt() else 0xFFDC2626.toInt()

    fun rounded(fill: Int, r: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke)
    }

    fun tv(s: String, sp: Float = 14f, bold: Boolean = false, col: Int = INK): TextView = TextView(this).apply {
        text = s; textSize = sp; setTextColor(col); if (bold) setTypeface(null, Typeface.BOLD)
    }

    fun card(parent: LinearLayout, title: String? = null): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(CARD, 20, LINE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
        }
        if (title != null) c.addView(tv(title.uppercase(), 12f, true, MUTED).apply { setPadding(0, 0, 0, dp(8)); letterSpacing = 0.08f })
        parent.addView(c); return c
    }

    fun btn(text: String, primary: Boolean, onClick: () -> Unit): TextView = tv(text, 16f, true, if (primary) Color.WHITE else ACC).apply {
        gravity = Gravity.CENTER; setPadding(dp(12), dp(14), dp(12), dp(14))
        background = rounded(if (primary) ACC else SEL, 18)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(5), dp(12), dp(5)) }
        setOnClickListener { onClick() }
    }

    fun scroll(col: LinearLayout): ScrollView = ScrollView(this).apply { addView(col); isVerticalScrollBarEnabled = false }
    fun colLayout(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(24)) }
    fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
    fun spacer(h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(-1, dp(h)) }
}
