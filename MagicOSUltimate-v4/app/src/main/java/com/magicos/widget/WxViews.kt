package com.magicos.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun sn(x: Double): Float = sin(x).toFloat()

/** Icon thời tiết chuyển động, tự vẽ bằng code */
class WxIconView(ctx: Context, private var code: Int, private var day: Boolean, private val fps: Int = 24) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val LIGHT = 0xFFF4F7FC.toInt()
    private val BACK = 0xFFC4CEDF.toInt()
    private val DARK = 0xFF6F7B93.toInt()

    fun set(code: Int, day: Boolean) { this.code = code; this.day = day; invalidate() }

    private fun kind(): Int = when (code) {
        0 -> if (day) 0 else 1
        1, 2 -> if (day) 2 else 3
        3 -> 4
        45, 48 -> 5
        in 51..57 -> 6
        in 61..67 -> 7
        in 80..82 -> 8
        in 71..77, 85, 86 -> 9
        95, 96, 99 -> 10
        else -> 4
    }

    private fun sway(t: Float, ph: Float): Float = sn(2 * PI * (t * 2 + ph)) * 0.035f

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val s = min(w, h)
        if (s <= 0f) return
        val t = (SystemClock.uptimeMillis() % 8000L) / 8000f
        cv.save(); cv.translate((w - s) / 2f, (h - s) / 2f)
        when (kind()) {
            0 -> sun(cv, s * .5f, s * .5f, s * .24f, t)
            1 -> { moon(cv, s * .5f, s * .5f, s * .28f); stars(cv, s, t) }
            2 -> { sun(cv, s * .38f, s * .38f, s * .2f, t); cloud(cv, s * .55f + sway(t, 0f) * s, s * .62f, s * .62f, LIGHT) }
            3 -> { moon(cv, s * .36f, s * .36f, s * .2f); stars(cv, s, t); cloud(cv, s * .55f + sway(t, 0f) * s, s * .62f, s * .62f, LIGHT) }
            4 -> { cloud(cv, s * .42f + sway(t, 0f) * s, s * .40f, s * .56f, BACK); cloud(cv, s * .58f + sway(t, .5f) * s, s * .60f, s * .64f, LIGHT) }
            5 -> { cloud(cv, s * .5f + sway(t, 0f) * s, s * .34f, s * .62f, BACK); fog(cv, s, t) }
            6 -> { cloud(cv, s * .5f + sway(t, 0f) * s * .5f, s * .34f, s * .68f, DARK); rain(cv, s, t, 3) }
            7 -> { cloud(cv, s * .5f + sway(t, 0f) * s * .5f, s * .34f, s * .68f, DARK); rain(cv, s, t, 5) }
            8 -> { cloud(cv, s * .5f + sway(t, 0f) * s * .5f, s * .34f, s * .7f, 0xFF59647B.toInt()); rain(cv, s, t, 8) }
            9 -> { cloud(cv, s * .5f + sway(t, 0f) * s * .5f, s * .34f, s * .68f, BACK); snow(cv, s, t) }
            else -> { cloud(cv, s * .5f, s * .32f, s * .7f, 0xFF59647B.toInt()); bolt(cv, s, t) }
        }
        cv.restore()
        if (fps >= 60) postInvalidateOnAnimation() else postInvalidateDelayed((1000 / fps).toLong())
    }

    private fun sun(cv: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val pulse = 1f + 0.05f * sn(2 * PI * t * 4)
        p.style = Paint.Style.FILL; p.color = Color.argb(60, 255, 200, 61)
        cv.drawCircle(cx, cy, r * 1.55f * pulse, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.22f; p.color = 0xFFFFC83D.toInt()
        cv.save(); cv.rotate(t * 360f, cx, cy)
        for (i in 0 until 12) {
            cv.save(); cv.rotate(i * 30f, cx, cy)
            val len = if (i % 2 == 0) 1.85f else 1.62f
            cv.drawLine(cx, cy - r * 1.3f, cx, cy - r * len, p)
            cv.restore()
        }
        cv.restore()
        p.style = Paint.Style.FILL; p.color = 0xFFFFD54F.toInt()
        cv.drawCircle(cx, cy, r, p)
    }

    private fun moon(cv: Canvas, cx: Float, cy: Float, r: Float) {
        val a = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val b = Path().apply { addCircle(cx + r * 0.45f, cy - r * 0.2f, r * 0.85f, Path.Direction.CW) }
        a.op(b, Path.Op.DIFFERENCE)
        p.style = Paint.Style.FILL; p.color = 0xFFF3E9B8.toInt()
        cv.drawPath(a, p)
    }

    private fun stars(cv: Canvas, s: Float, t: Float) {
        val pts = arrayOf(floatArrayOf(.78f, .22f), floatArrayOf(.88f, .44f), floatArrayOf(.66f, .1f))
        for (i in pts.indices) {
            val a = (130 + 125 * (0.5f + 0.5f * sn(2 * PI * (t * 3 + i * 0.33)))).toInt()
            p.style = Paint.Style.FILL; p.color = Color.argb(a, 255, 246, 200)
            cv.drawCircle(pts[i][0] * s, pts[i][1] * s, s * 0.022f, p)
        }
    }

    private fun cloud(cv: Canvas, cx: Float, cy: Float, w: Float, color: Int) {
        p.style = Paint.Style.FILL; p.color = color
        cv.drawCircle(cx - w * .22f, cy + w * .02f, w * .24f, p)
        cv.drawCircle(cx + w * .02f, cy - w * .10f, w * .32f, p)
        cv.drawCircle(cx + w * .27f, cy + w * .05f, w * .22f, p)
        cv.drawRoundRect(RectF(cx - w * .46f, cy + w * .02f, cx + w * .48f, cy + w * .27f), w * .13f, w * .13f, p)
    }

    private fun rain(cv: Canvas, s: Float, t: Float, n: Int) {
        p.style = Paint.Style.STROKE; p.strokeWidth = s * .035f
        for (i in 0 until n) {
            val ph = (t * 8f + i / n.toFloat()) % 1f
            val x = s * (0.28f + 0.44f * i / (n - 1).toFloat())
            val y = s * .62f + ph * s * .34f
            p.color = Color.argb((255 * (1f - ph)).toInt().coerceIn(0, 255), 110, 193, 255)
            cv.drawLine(x, y, x - s * .03f, y + s * .08f, p)
        }
    }

    private fun snow(cv: Canvas, s: Float, t: Float) {
        p.style = Paint.Style.FILL
        for (i in 0 until 5) {
            val ph = (t * 3f + i / 5f) % 1f
            val x = s * (0.28f + 0.44f * i / 4f) + sn(2 * PI * (t * 3 + i)) * s * .03f
            val y = s * .62f + ph * s * .34f
            p.color = Color.argb((255 * (1f - ph * .7f)).toInt().coerceIn(0, 255), 255, 255, 255)
            cv.drawCircle(x, y, s * .028f, p)
        }
    }

    private fun fog(cv: Canvas, s: Float, t: Float) {
        p.style = Paint.Style.FILL
        for (i in 0 until 3) {
            val off = sn(2 * PI * (t * 2 + i * 0.3)) * s * .05f
            val y = s * (0.62f + i * 0.12f)
            p.color = Color.argb(200 - i * 40, 220, 228, 240)
            cv.drawRoundRect(RectF(s * .2f + off, y, s * .8f + off, y + s * .05f), s * .025f, s * .025f, p)
        }
    }

    private fun bolt(cv: Canvas, s: Float, t: Float) {
        val f = (t * 8f) % 3f
        val a = if (f < 0.3f) 255 else if (f < 0.55f) 150 else 70
        val path = Path().apply {
            moveTo(s * .52f, s * .56f); lineTo(s * .40f, s * .78f); lineTo(s * .50f, s * .78f)
            lineTo(s * .44f, s * .96f); lineTo(s * .64f, s * .70f); lineTo(s * .53f, s * .70f); lineTo(s * .60f, s * .56f); close()
        }
        p.style = Paint.Style.FILL; p.color = Color.argb(a, 255, 214, 64)
        cv.drawPath(path, p)
    }
}

/** Hình minh họa cho các thẻ thông tin thời tiết */
class WxViz(ctx: Context, private val type: Int) : View(ctx) {
    var v1 = 0f
    var v2 = 0f
    var txt1 = ""
    var txt2 = ""
    var ink = Color.WHITE
    var sub = Color.GRAY
    var track = 0x33808080
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val d = ctx.resources.displayMetrics.density

    companion object { const val COMPASS = 0; const val SUNARC = 1; const val GAUGE = 2; const val MOON = 3; const val UV = 4; const val VIS = 5 }

    override fun onDraw(cv: Canvas) {
        when (type) {
            COMPASS -> compass(cv); SUNARC -> sunArc(cv); GAUGE -> gauge(cv); MOON -> moon(cv); UV -> uv(cv); else -> vis(cv)
        }
    }

    private fun compass(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = min(width, height) / 2f - 4 * d
        p.style = Paint.Style.STROKE; p.color = sub; p.strokeWidth = 1.2f * d
        for (i in 0 until 60) {
            val a = (i * 6.0) * PI / 180; val len = if (i % 5 == 0) r * 0.13f else r * 0.07f
            cv.drawLine(cx + (r - len) * sin(a).toFloat(), cy - (r - len) * cos(a).toFloat(), cx + r * sin(a).toFloat(), cy - r * cos(a).toFloat(), p)
        }
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.textSize = 12 * d; p.color = sub
        cv.drawText("W", cx - r * 0.72f, cy + 4 * d, p); cv.drawText("E", cx + r * 0.72f, cy + 4 * d, p)
        val th = v1 * PI / 180
        val kx = cx + r * 0.80f * sin(th).toFloat(); val ky = cy - r * 0.80f * cos(th).toFloat()
        val hx = cx - r * 0.80f * sin(th).toFloat(); val hy = cy + r * 0.80f * cos(th).toFloat()
        p.style = Paint.Style.STROKE; p.color = ink; p.strokeWidth = 3.5f * d
        cv.drawLine(kx, ky, hx, hy, p)
        p.style = Paint.Style.FILL
        cv.drawCircle(kx, ky, 5 * d, p)
        val dx = (hx - kx) / (2 * r * 0.80f); val dy = (hy - ky) / (2 * r * 0.80f)
        val ah = Path().apply {
            moveTo(hx + dx * 8 * d, hy + dy * 8 * d)
            lineTo(hx - dx * 6 * d - dy * 7 * d, hy - dy * 6 * d + dx * 7 * d)
            lineTo(hx - dx * 6 * d + dy * 7 * d, hy - dy * 6 * d - dx * 7 * d); close()
        }
        cv.drawPath(ah, p)
        p.color = track; cv.drawCircle(cx, cy, r * 0.42f, p)
        p.color = ink; p.textSize = 26 * d; p.isFakeBoldText = true
        cv.drawText(txt1, cx, cy + 6 * d, p)
        p.textSize = 11 * d; p.isFakeBoldText = false; cv.drawText(txt2, cx, cy + 20 * d, p)
    }

    private fun sunArc(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val base = h * 0.66f; val hh = h * 0.52f
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f * d; p.color = track
        cv.drawLine(0f, base, w, base, p)
        p.strokeWidth = 3f * d; p.color = sub
        val path = Path()
        for (i in 0..40) {
            val u = i / 40f; val x = w * (0.04f + 0.92f * u); val y = base - sin(PI * u).toFloat() * hh
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        cv.drawPath(path, p)
        val u = v1.coerceIn(0f, 1f); val x = w * (0.04f + 0.92f * u); val y = base - sin(PI * u).toFloat() * hh
        p.style = Paint.Style.FILL; p.color = if (v1 in 0f..1f) 0xFFFFC83D.toInt() else sub
        cv.drawCircle(x, y, 7 * d, p)
    }

    private fun gauge(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = min(width, height) / 2f - 4 * d
        val f = ((v1 - 980f) / 60f).coerceIn(0f, 1f)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f * d
        for (i in 0..44) {
            val fr = i / 44f
            val a = (135.0 + 270.0 * fr) * PI / 180; val len = r * 0.14f
            p.color = if (fr <= f) ink else track
            cv.drawLine(cx + (r - len) * cos(a).toFloat(), cy + (r - len) * sin(a).toFloat(), cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat(), p)
        }
        p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.color = ink; p.isFakeBoldText = true; p.textSize = 28 * d
        cv.drawText(txt1, cx, cy + 6 * d, p)
        p.isFakeBoldText = false; p.textSize = 12 * d; p.color = sub; cv.drawText(txt2, cx, cy + 24 * d, p)
    }

    private fun moon(cv: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = min(width, height) / 2f - 3 * d
        val f = v1
        p.style = Paint.Style.FILL; p.color = 0xFF2A2F3E.toInt(); cv.drawCircle(cx, cy, r, p)
        val circle = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val right = Path().apply { addArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 180f); close() }
        val left = Path().apply { addArc(RectF(cx - r, cy - r, cx + r, cy + r), 90f, 180f); close() }
        val rx = r * abs(cos(2 * PI * f)).toFloat()
        val ell = Path().apply { addOval(RectF(cx - rx, cy - r, cx + rx, cy + r), Path.Direction.CW) }
        val lit = if (f < 0.5f) right else left
        if (f < 0.25f || f > 0.75f) lit.op(ell, Path.Op.DIFFERENCE) else lit.op(ell, Path.Op.UNION)
        lit.op(circle, Path.Op.INTERSECT)
        cv.save(); cv.clipPath(lit)
        p.color = 0xFFDADDE6.toInt(); cv.drawCircle(cx, cy, r, p)
        p.color = 0x33808899
        cv.drawCircle(cx - r * .3f, cy - r * .25f, r * .18f, p); cv.drawCircle(cx + r * .25f, cy + r * .1f, r * .24f, p)
        cv.drawCircle(cx - r * .1f, cy + r * .45f, r * .14f, p); cv.drawCircle(cx + r * .4f, cy - r * .4f, r * .1f, p)
        cv.restore()
    }

    private fun uv(cv: Canvas) {
        val w = width.toFloat(); val cy = height / 2f; val bh = 6 * d
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(0xFF5FC768.toInt(), 0xFFF5D547.toInt(), 0xFFF2A33A.toInt(), 0xFFE8503E.toInt(), 0xFFD46BF0.toInt()), null, Shader.TileMode.CLAMP)
        cv.drawRoundRect(RectF(0f, cy - bh / 2, w, cy + bh / 2), bh, bh, p)
        p.shader = null
        val x = (v1 / 12f).coerceIn(0f, 1f) * (w - 12 * d) + 6 * d
        p.color = Color.WHITE; cv.drawCircle(x, cy, 6 * d, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f * d; p.color = 0x55000000; cv.drawCircle(x, cy, 6 * d, p)
    }

    private fun vis(cv: Canvas) {
        val w = width.toFloat(); val cy = height / 2f
        val x = v1.coerceIn(0f, 1f) * (w - 12 * d) + 6 * d
        p.style = Paint.Style.STROKE; p.strokeWidth = 5 * d
        p.color = ink; cv.drawLine(0f, cy, x - 8 * d, cy, p)
        p.color = track; cv.drawLine(x + 8 * d, cy, w, cy, p)
        p.style = Paint.Style.FILL; p.color = ink; cv.drawCircle(x, cy, 5 * d, p)
    }
}
