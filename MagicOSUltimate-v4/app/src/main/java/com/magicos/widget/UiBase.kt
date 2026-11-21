package com.magicos.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Tự xuống dòng khi hết chỗ, không cắt chữ */
class FlowLayout(ctx: Context, private val hGap: Int, private val vGap: Int) : ViewGroup(ctx) {
    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val maxW = MeasureSpec.getSize(wSpec) - paddingLeft - paddingRight
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val ch = getChildAt(i)
            if (ch.visibility == View.GONE) continue
            ch.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED)
            val w = ch.measuredWidth; val h = ch.measuredHeight
            if (x > 0 && x + w > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            x += w + hGap; rowH = maxOf(rowH, h)
        }
        setMeasuredDimension(MeasureSpec.getSize(wSpec), y + rowH + paddingTop + paddingBottom)
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l - paddingLeft - paddingRight
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val ch = getChildAt(i)
            if (ch.visibility == View.GONE) continue
            val w = ch.measuredWidth; val h = ch.measuredHeight
            if (x > 0 && x + w > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            ch.layout(paddingLeft + x, paddingTop + y, paddingLeft + x + w, paddingTop + y + h)
            x += w + hGap; rowH = maxOf(rowH, h)
        }
    }
}

open class UiBase : Activity() {
    val ui = Handler(Looper.getMainLooper())
    val isDark: Boolean get() = Render.isNight(this)
    fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt()
    fun tr(vi: String, en: String): String = Lang.tr(this, vi, en)

    val BG: Int get() = if (isDark) 0xFF000000.toInt() else 0xFFF4F6FB.toInt()
    val NAV: Int get() = BG
    val CARD: Int get() = if (isDark) 0xFF121214.toInt() else Color.WHITE
    val TILE: Int get() = if (isDark) 0xFF1E1E22.toInt() else 0xFFF1F4FA.toInt()
    val INK: Int get() = if (isDark) 0xFFF2F2F5.toInt() else 0xFF111827.toInt()
    val MUTED: Int get() = if (isDark) 0xFF9A9AA5.toInt() else 0xFF6B7280.toInt()
    val LINE: Int get() = if (isDark) 0xFF2C2C30.toInt() else 0xFFE3E7F0.toInt()
    val SEL: Int get() = if (isDark) 0xFF1D2A4D.toInt() else 0xFFE8EDFF.toInt()
    val ACC: Int get() = if (isDark) 0xFF7C9BFF.toInt() else 0xFF4A6CF7.toInt()
    val GOOD: Int get() = if (isDark) 0xFF4ADE80.toInt() else 0xFF16A34A.toInt()
    val BAD: Int get() = if (isDark) 0xFFF87171.toInt() else 0xFFDC2626.toInt()

    fun rounded(fill: Int, r: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke)
    }

    fun tv(s: String, sp: Float = 14f, bold: Boolean = false, col: Int = INK): TextView = TextView(this).apply {
        text = s; textSize = sp; setTextColor(col); if (bold) setTypeface(null, Typeface.BOLD)
    }

    /** TextView căn giữa chính xác, dùng với LayoutParams(-1,-2) */
    fun ctv(s: String, sp: Float = 14f, bold: Boolean = false, col: Int = INK): TextView = tv(s, sp, bold, col).apply {
        gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER
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
    fun colLayout(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(16)) }
    fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
}
