package com.magicos.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/** Vẽ nền widget đúng kích thước thật: bo góc tùy chỉnh + hiệu ứng kính lỏng */
object Bg {
    private fun white(a: Float): Int = Color.argb((a * 255).toInt().coerceIn(0, 255), 255, 255, 255)

    fun make(c: Context, look: Int, wDp: Float, hDp: Float): Bitmap {
        val dens = c.resources.displayMetrics.density
        var sc = dens
        if (wDp * sc > 720f) sc = 720f / wDp
        val w = (wDp * sc).toInt().coerceAtLeast(40); val h = (hDp * sc).toInt().coerceAtLeast(40)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val r = (S.i(c, "cr", 24) * sc).coerceAtMost(minOf(w, h) / 2f)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (look != 2) {
            p.color = Render.col(c, if (look == 0) "bgL" else "bgD")
            cv.drawRoundRect(rect, r, r, p)
            return bmp
        }
        val tint = Render.col(c, "glass")
        val ga = S.i(c, "ga", 30); val gf = S.i(c, "gf", 40); val gd = S.i(c, "gd", 50) / 100f
        val fa = gf / 100f
        val clip = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
        // lớp màu kính (độ đậm)
        p.color = (tint and 0xFFFFFF) or ((ga * 255 / 100 * Color.alpha(tint) / 255) shl 24)
        cv.drawRoundRect(rect, r, r, p)
        cv.save(); cv.clipPath(clip)
        // lớp mờ đục kiểu kính nhám (độ mờ)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), white(fa * 0.34f), white(fa * 0.08f), Shader.TileMode.CLAMP)
        cv.drawRect(rect, p)
        // vệt sáng phía trên (độ nổi khối)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.5f, white(gd * 0.28f), white(0f), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w.toFloat(), h * 0.5f, p)
        // bóng đổ bên trong phía dưới
        p.shader = LinearGradient(0f, h * 0.55f, 0f, h.toFloat(), Color.argb(0, 0, 0, 0), Color.argb((gd * 70).toInt(), 0, 0, 0), Shader.TileMode.CLAMP)
        cv.drawRect(0f, h * 0.55f, w.toFloat(), h.toFloat(), p)
        cv.restore()
        // viền sáng
        val bw = S.i(c, "bw", 1)
        if (bw > 0) {
            val sw = maxOf(0.8f, bw * sc * (0.8f + gd * 0.8f))
            p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), white(0.35f + 0.55f * gd), white(0.06f + 0.2f * gd), Shader.TileMode.CLAMP)
            p.style = Paint.Style.STROKE; p.strokeWidth = sw
            val inset = sw / 2f
            cv.drawRoundRect(RectF(inset, inset, w - inset, h - inset), r - inset, r - inset, p)
        }
        return bmp
    }
}
