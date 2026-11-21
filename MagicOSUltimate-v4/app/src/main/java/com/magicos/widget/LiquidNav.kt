package com.magicos.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/** Thanh menu kính lỏng: chạm hoặc đặt tay lên rồi lướt ngang để chuyển tab */
class LiquidNav(ctx: Context, private val items: List<String>, private val onSelect: (Int) -> Unit) : View(ctx) {
    var dark = false
    var accent = Color.BLUE
    var muted = Color.GRAY
    var selected = 0
        private set
    private var pos = 0f
    private var blob = 0f
    private var anim: ValueAnimator? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = ctx.resources.displayMetrics.density

    fun select(i: Int) { selected = i; if (!pressed2) animatePos(i.toFloat()) }
    private var pressed2 = false

    private fun animatePos(target: Float) {
        anim?.cancel()
        val a = ValueAnimator.ofFloat(pos, target)
        a.duration = 280; a.interpolator = DecelerateInterpolator()
        a.addUpdateListener { pos = it.animatedValue as Float; invalidate() }
        anim = a; a.start()
    }

    private fun animBlob(to: Float) {
        val a = ValueAnimator.ofFloat(blob, to)
        a.duration = 180
        a.addUpdateListener { blob = it.animatedValue as Float; invalidate() }
        a.start()
    }

    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(MeasureSpec.getSize(w), (66 * d).toInt()) }

    private fun clampPos(v: Float) = v.coerceIn(0f, (items.size - 1).toFloat())

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val tabW = width / items.size.toFloat()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                pressed2 = true; anim?.cancel(); animBlob(1f); pos = clampPos(e.x / tabW - 0.5f); invalidate()
            }
            MotionEvent.ACTION_MOVE -> { pos = clampPos(e.x / tabW - 0.5f); invalidate() }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressed2 = false; animBlob(0f)
                if (e.actionMasked == MotionEvent.ACTION_UP) {
                    val i = (e.x / tabW).toInt().coerceIn(0, items.size - 1)
                    selected = i; animatePos(i.toFloat()); onSelect(i)
                } else animatePos(selected.toFloat())
            }
        }
        return true
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = h / 2f
        // khung kính
        p.shader = null; p.style = Paint.Style.FILL
        p.color = if (dark) 0xF0161618.toInt() else 0xF2FFFFFF.toInt()
        cv.drawRoundRect(RectF(0f, 0f, w, h), r, r, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = d * 1.2f
        p.color = if (dark) 0x40FFFFFF else 0x26000000
        cv.drawRoundRect(RectF(d, d, w - d, h - d), r - d, r - d, p)
        val tabW = w / items.size
        // viên kính di chuyển theo ngón tay
        val cx = (pos + 0.5f) * tabW
        val hw = tabW - 6 * d + blob * 12 * d; val hh = h - 12 * d + blob * 8 * d
        val hr = RectF(cx - hw / 2, h / 2 - hh / 2, cx + hw / 2, h / 2 + hh / 2)
        p.style = Paint.Style.FILL
        p.color = if (dark) 0x38FFFFFF else 0x264A6CF7
        cv.drawRoundRect(hr, hh / 2, hh / 2, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = d
        p.color = if (dark) 0x66FFFFFF else 0x554A6CF7
        cv.drawRoundRect(hr, hh / 2, hh / 2, p)
        // biểu tượng + chữ
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER
        for (i in items.indices) {
            val parts = items[i].split("\n")
            val f = (1f - abs(i - pos)).coerceIn(0f, 1f)
            p.color = Color.WHITE; p.isFakeBoldText = false; p.textSize = (19f + 3f * f) * d
            cv.drawText(parts[0], (i + 0.5f) * tabW, h / 2 - 3 * d, p)
            p.color = blend(muted, accent, f); p.isFakeBoldText = true; p.textSize = 10.5f * d
            cv.drawText(parts.getOrElse(1) { "" }, (i + 0.5f) * tabW, h / 2 + 17 * d, p)
        }
        p.isFakeBoldText = false
    }
}
