package com.magicos.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import kotlin.math.abs

class SwipeLayout(ctx: Context, private val onSwipe: (Int) -> Unit) : LinearLayout(ctx) {
    private val det = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (abs(velocityX) > 900 && abs(velocityX) > abs(velocityY) * 1.5f) { onSwipe(if (velocityX < 0) 1 else -1); return true }
            return false
        }
    })
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { det.onTouchEvent(ev); return super.dispatchTouchEvent(ev) }
}

class CalendarPage(private val a: UiBase) {
    private val now = Calendar.getInstance()
    private val ty = now.get(Calendar.YEAR); private val tm = now.get(Calendar.MONTH) + 1; private val td = now.get(Calendar.DAY_OF_MONTH)
    private var vy = ty; private var vm = tm
    private var sy = ty; private var sm = tm; private var sd = td
    private lateinit var todayBox: LinearLayout
    private lateinit var monthBox: LinearLayout
    private lateinit var detailBox: LinearLayout
    private lateinit var hoursBox: LinearLayout
    private lateinit var eventsBox: LinearLayout
    private val RED = 0xFFEF4444.toInt()
    private val GOLD = 0xFFF5B301.toInt()

    private fun dim(y: Int, m: Int): Int = Calendar.getInstance().apply { clear(); set(y, m - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    private fun wd(y: Int, m: Int, d: Int): String = Render.WD[Calendar.getInstance().apply { clear(); set(y, m - 1, d) }.get(Calendar.DAY_OF_WEEK) - 1]

    fun build(): View {
        val col = a.colLayout()
        todayBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        col.addView(todayBox, LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(12), a.dp(6), a.dp(12), a.dp(6)) })
        val mc = SwipeLayout(a) { go(it) }.apply {
            orientation = LinearLayout.VERTICAL; setPadding(a.dp(12), a.dp(14), a.dp(12), a.dp(12))
            background = a.rounded(a.CARD, 20, a.LINE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(12), a.dp(6), a.dp(12), a.dp(6)) }
        }
        monthBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        mc.addView(monthBox); col.addView(mc)
        col.addView(a.btn("Về hôm nay", false) { vy = ty; vm = tm; sy = ty; sm = tm; sd = td; drawMonth(); drawDetail(); drawHours(); drawEvents() })
        val dc = a.card(col, "Chi tiết ngày"); detailBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; dc.addView(detailBox)
        val hc = a.card(col, "Giờ hoàng đạo"); hoursBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; hc.addView(hoursBox)
        val ec = a.card(col, "Sự kiện tháng"); eventsBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; ec.addView(eventsBox)
        col.addView(a.tv("Âm lịch tính theo múi giờ Việt Nam (UTC+7). Thông tin ngày giờ tốt xấu mang tính tham khảo theo lịch vạn niên truyền thống.", 12f, false, a.MUTED)
            .apply { gravity = Gravity.CENTER; setPadding(a.dp(20), a.dp(8), a.dp(20), a.dp(8)) })
        drawToday(); drawMonth(); drawDetail(); drawHours(); drawEvents()
        return a.scroll(col)
    }

    private fun go(delta: Int) {
        vm += delta
        if (vm > 12) { vm = 1; vy++ } else if (vm < 1) { vm = 12; vy-- }
        drawMonth(); drawEvents()
    }

    private fun chip(t: String): TextView = a.tv(t, 12f, true, Color.WHITE).apply {
        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(a.dp(10), a.dp(6), a.dp(10), a.dp(6)); background = a.rounded(0x33FFFFFF, 16)
    }

    private fun drawToday() {
        val i = Almanac.Info(td, tm, ty)
        todayBox.removeAllViews()
        todayBox.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            if (a.isDark) intArrayOf(0xFF2A47B8.toInt(), 0xFF3A2D96.toInt()) else intArrayOf(0xFF4568F0.toInt(), 0xFF5B3FD6.toInt())).apply { cornerRadius = a.dp(26).toFloat() }
        todayBox.setPadding(a.dp(18), a.dp(16), a.dp(18), a.dp(16))
        todayBox.addView(a.tv("HÔM NAY", 12f, true, 0xCCFFFFFF.toInt()).apply { letterSpacing = 0.1f })
        val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        r.addView(a.tv("$td", 64f, false, Color.WHITE))
        val c = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(14), 0, 0, 0) }
        c.addView(a.tv(Render.WD[now.get(Calendar.DAY_OF_WEEK) - 1], 22f, true, Color.WHITE))
        c.addView(a.tv("Tháng $tm, $ty", 15f, false, 0xE6FFFFFF.toInt()))
        r.addView(c); todayBox.addView(r)
        todayBox.addView(a.tv("Âm lịch · ${i.lunar[0]} tháng ${i.lunar[1]} · năm ${i.yearName}", 16f, true, Color.WHITE).apply { setPadding(0, a.dp(6), 0, a.dp(10)) })
        val r1 = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Ngày ${i.dayName}", "Tháng ${i.monthName}", "Năm ${i.yearName}").forEach {
            r1.addView(chip(it), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(a.dp(2), 0, a.dp(2), 0) })
        }
        todayBox.addView(r1)
        val r2 = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(6), 0, 0) }
        r2.addView(chip("☾ ${if (i.hoangDao) "Hoàng đạo" else "Hắc đạo"} · ${i.starName}"), LinearLayout.LayoutParams(-2, -2).apply { setMargins(a.dp(2), 0, 0, 0) })
        todayBox.addView(r2)
    }

    private fun dot(color: Int): TextView = a.tv("●", 7f, false, color)

    private fun drawMonth() {
        monthBox.removeAllViews()
        val y = vy; val m = vm; val n = dim(y, m)
        val f = Almanac.Info(1, m, y).lunar; val l = Almanac.Info(n, m, y).lunar
        val head = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun arrow(s: String, d: Int) = a.tv(s, 24f, true, a.ACC).apply {
            gravity = Gravity.CENTER; background = a.rounded(a.SEL, 22); setOnClickListener { go(d) } }
        head.addView(arrow("‹", -1), LinearLayout.LayoutParams(a.dp(44), a.dp(44)))
        val mid = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        mid.addView(a.tv("Tháng $m · $y", 22f, true))
        mid.addView(a.tv("Âm lịch: tháng ${f[1]}${if (f[1] != l[1]) " – ${l[1]}" else ""} năm ${Lunar.yearName(l[2])}", 12f, false, a.MUTED))
        head.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(arrow("›", 1), LinearLayout.LayoutParams(a.dp(44), a.dp(44)))
        monthBox.addView(head)

        val hr = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(10), 0, a.dp(4)) }
        arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach {
            hr.addView(a.tv(it, 13f, true, if (it == "CN") a.BAD else a.MUTED).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        monthBox.addView(hr)
        val first = Calendar.getInstance().apply { clear(); set(y, m - 1, 1) }
        val off = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        for (r in 0..5) {
            if (r * 7 - off + 1 > n) break
            val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..6) {
                val d = r * 7 + c - off + 1
                val cell = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, a.dp(5), 0, a.dp(4)) }
                if (d in 1..n) {
                    val info = Almanac.Info(d, m, y)
                    val sel = d == sd && m == sm && y == sy
                    val isToday = d == td && m == tm && y == ty
                    cell.background = if (sel) a.rounded(a.ACC, 14) else if (isToday) a.rounded(0, 14, a.ACC) else null
                    val white = sel
                    cell.addView(a.tv("$d", 18f, true, if (white) Color.WHITE else if (c == 6) a.BAD else a.INK))
                    val ls = if (info.lunar[0] == 1) "1/${info.lunar[1]}" else "${info.lunar[0]}"
                    cell.addView(a.tv(ls, 11f, info.lunar[0] == 1, if (white) 0xE6FFFFFF.toInt() else if (info.lunar[0] == 1) a.ACC else a.MUTED))
                    val dots = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
                    if (info.events().isNotEmpty()) dots.addView(dot(RED))
                    if (info.hoangDao) dots.addView(dot(GOLD))
                    if (dots.childCount == 0) dots.addView(dot(0x00000000))
                    cell.addView(dots)
                    cell.setOnClickListener { sy = y; sm = m; sd = d; drawMonth(); drawDetail(); drawHours() }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(a.dp(1), a.dp(1), a.dp(1), a.dp(1)) })
            }
            monthBox.addView(row)
        }
        val lg = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(a.dp(4), a.dp(10), 0, 0) }
        lg.addView(dot(RED)); lg.addView(a.tv(" Lễ / sự kiện   ", 12f, false, a.MUTED))
        lg.addView(dot(GOLD)); lg.addView(a.tv(" Hoàng đạo   ", 12f, false, a.MUTED))
        lg.addView(a.tv("Vuốt ngang để đổi tháng", 12f, false, a.MUTED))
        monthBox.addView(lg)
    }

    private fun row(first: Boolean, label: String, value: String, color: Int = 0) {
        if (!first) detailBox.addView(View(a).apply { setBackgroundColor(a.LINE) }, LinearLayout.LayoutParams(-1, 1))
        val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(10), 0, a.dp(10)) }
        r.addView(a.tv(label, 14f, false, a.MUTED), LinearLayout.LayoutParams(a.dp(104), -2))
        r.addView(a.tv(value, 15f, true, if (color == 0) a.INK else color), LinearLayout.LayoutParams(0, -2, 1f))
        detailBox.addView(r)
    }

    private fun drawDetail() {
        detailBox.removeAllViews()
        val i = Almanac.Info(sd, sm, sy)
        val ky = i.kyList(); val ev = i.events()
        row(true, "Dương lịch", "${wd(sy, sm, sd)}, $sd/$sm/$sy")
        row(false, "Âm lịch", "${i.lunar[0]} tháng ${i.lunar[1]}${if (i.lunar[3] == 1) " nhuận" else ""} năm ${i.yearName} (${i.animal})")
        row(false, "Can chi", "Ngày ${i.dayName} · Tháng ${i.monthName} · Năm ${i.yearName}")
        row(false, "Tiết khí", i.termInfo())
        row(false, if (i.hoangDao) "Hoàng đạo" else "Hắc đạo", "Sao ${i.starName} · ${if (i.hoangDao) "ngày tốt" else "ngày xấu"}")
        row(false, "Trực", "${i.trucName} — ${i.trucText}", if (i.trucRating > 0) a.GOOD else if (i.trucRating < 0) a.BAD else 0)
        row(false, "Nhị thập bát tú", i.tu)
        row(false, "Lục diệu", "${i.ldName} — ${i.ldText}", if (i.ldGood) a.GOOD else a.BAD)
        row(false, "Xuất hành", "Hỷ thần: ${i.hyThan}\nTài thần: ${i.taiThan}")
        row(false, "Tuổi xung", i.tuoiXung)
        row(false, "Ngày kỵ", if (ky.isEmpty()) "Không phạm ngày kỵ" else "Phạm " + ky.joinToString(", "), if (ky.isEmpty()) a.GOOD else a.BAD)
        row(false, "Sự kiện", if (ev.isEmpty()) "Không có sự kiện đặc biệt" else ev.joinToString("\n"))
    }

    private fun drawHours() {
        hoursBox.removeAllViews()
        val i = Almanac.Info(sd, sm, sy)
        val good = i.goodHours.toSet()
        val curH = ((now.get(Calendar.HOUR_OF_DAY) + 1) / 2) % 12
        val isToday = sd == td && sm == tm && sy == ty
        for (r in 0..2) {
            val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..3) {
                val h = r * 4 + c
                val g = h in good
                val cell = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(0, a.dp(12), 0, a.dp(12))
                    background = a.rounded(if (g) (if (a.isDark) 0xFF4A3A1A.toInt() else 0xFFFFF1CC.toInt()) else a.TILE, 14, if (isToday && h == curH) a.ACC else 0) }
                cell.addView(a.tv(Almanac.CHI[h], 17f, true, if (g) (if (a.isDark) 0xFFF5D547.toInt() else 0xFF9A6400.toInt()) else a.MUTED))
                cell.addView(a.tv(Almanac.hourRange(h), 12f, false, a.MUTED))
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(a.dp(3), a.dp(3), a.dp(3), a.dp(3)) })
            }
            hoursBox.addView(row)
        }
        hoursBox.addView(a.tv("Vàng: giờ hoàng đạo (tốt) · Xám: giờ hắc đạo · Viền xanh: giờ hiện tại", 12f, false, a.MUTED).apply { setPadding(0, a.dp(8), 0, 0) })
    }

    private fun drawEvents() {
        eventsBox.removeAllViews()
        val y = vy; val m = vm; var any = false
        for (d in 1..dim(y, m)) {
            val i = Almanac.Info(d, m, y); val ev = i.events()
            for (e in ev) {
                any = true
                val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(8), 0, a.dp(8)) }
                r.addView(a.tv("$d/$m", 17f, true, a.ACC), LinearLayout.LayoutParams(a.dp(64), -2))
                val c = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                c.addView(a.tv(e, 16f, true))
                c.addView(a.tv("${wd(y, m, d)} · Âm lịch ${i.lunar[0]}/${i.lunar[1]}", 13f, false, a.MUTED))
                r.addView(c, LinearLayout.LayoutParams(0, -2, 1f)); eventsBox.addView(r)
            }
        }
        if (!any) eventsBox.addView(a.tv("Không có sự kiện trong tháng $m", 14f, false, a.MUTED))
    }
}
