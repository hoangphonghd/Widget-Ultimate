package com.magicos.widget

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import kotlin.math.abs

class SwipeLayout(ctx: Context, private val onSwipe: (Int) -> Unit) : LinearLayout(ctx) {
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var sx = 0f
    private var sy = 0f
    private var swiping = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { sx = ev.x; sy = ev.y; swiping = false }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - sx; val dy = ev.y - sy
                if (!swiping && abs(dx) > slop * 2 && abs(dx) > abs(dy) * 1.5f) {
                    swiping = true; parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
        return swiping
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { sx = ev.x; sy = ev.y }
            MotionEvent.ACTION_UP -> {
                if (swiping) { val dx = ev.x - sx; if (abs(dx) > slop * 6) onSwipe(if (dx < 0) 1 else -1) }
                swiping = false
            }
            MotionEvent.ACTION_CANCEL -> swiping = false
        }
        return true
    }
}

class CalendarPage(private val a: UiBase) {
    private val now = Calendar.getInstance()
    private val ty = now.get(Calendar.YEAR); private val tm = now.get(Calendar.MONTH) + 1; private val td = now.get(Calendar.DAY_OF_MONTH)
    private var vy = ty; private var vm = tm
    private var sy = ty; private var sm = tm; private var sd = td
    private lateinit var todayBox: LinearLayout
    private lateinit var monthBox: LinearLayout
    private lateinit var notesBox: LinearLayout
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
            orientation = LinearLayout.VERTICAL; setPadding(a.dp(10), a.dp(14), a.dp(10), a.dp(12))
            background = a.rounded(a.CARD, 20, a.LINE)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(12), a.dp(6), a.dp(12), a.dp(6)) }
        }
        monthBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        mc.addView(monthBox); col.addView(mc)
        col.addView(a.btn(a.tr("Về hôm nay", "Back to today"), false) { vy = ty; vm = tm; sy = ty; sm = tm; sd = td; drawAll() })
        val nc = a.card(col, a.tr("Ghi chú & nhắc nhở", "Notes & reminders")); notesBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; nc.addView(notesBox)
        val dc = a.card(col, a.tr("Chi tiết ngày", "Day details")); detailBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; dc.addView(detailBox)
        val hc = a.card(col, a.tr("Giờ hoàng đạo", "Auspicious hours")); hoursBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; hc.addView(hoursBox)
        val ec = a.card(col, a.tr("Sự kiện tháng", "Events this month")); eventsBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }; ec.addView(eventsBox)
        col.addView(a.ctv(a.tr("Âm lịch tính theo múi giờ Việt Nam (UTC+7). Thông tin ngày giờ tốt xấu mang tính tham khảo theo lịch vạn niên truyền thống.",
            "Lunar dates use Vietnam time (UTC+7). Auspicious day/hour info is traditional and for reference only."), 12f, false, a.MUTED)
            .apply { setPadding(a.dp(20), a.dp(8), a.dp(20), a.dp(8)) })
        drawToday(); drawAll()
        return a.scroll(col)
    }

    private fun drawAll() { drawMonth(); drawNotes(); drawDetail(); drawHours(); drawEvents() }

    private fun go(delta: Int) {
        vm += delta
        if (vm > 12) { vm = 1; vy++ } else if (vm < 1) { vm = 12; vy-- }
        drawMonth(); drawEvents()
    }

    private fun chip(t: String, bg: Int, fg: Int = Color.WHITE): TextView = a.tv(t, 12f, true, fg).apply {
        setPadding(a.dp(12), a.dp(7), a.dp(12), a.dp(7)); background = a.rounded(bg, 16)
    }

    private fun weekOfYear(): Pair<Int, Int> {
        val c = Calendar.getInstance(); c.firstDayOfWeek = Calendar.MONDAY; c.minimalDaysInFirstWeek = 4
        val w = c.get(Calendar.WEEK_OF_YEAR); var y = ty
        if (tm == 12 && w == 1) y++
        if (tm == 1 && w >= 52) y--
        return w to y
    }

    private fun drawToday() {
        val i = Almanac.Info(td, tm, ty)
        todayBox.removeAllViews()
        todayBox.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            if (a.isDark) intArrayOf(0xFF1F3A9E.toInt(), 0xFF352A8A.toInt()) else intArrayOf(0xFF4568F0.toInt(), 0xFF5B3FD6.toInt())).apply { cornerRadius = a.dp(26).toFloat() }
        todayBox.setPadding(a.dp(18), a.dp(16), a.dp(18), a.dp(16))
        todayBox.addView(a.tv(a.tr("HÔM NAY", "TODAY"), 12f, true, 0xCCFFFFFF.toInt()).apply { letterSpacing = 0.1f })
        val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        r.addView(a.tv("$td", 64f, false, Color.WHITE))
        val c = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(14), 0, 0, 0) }
        c.addView(a.tv(Render.WD[now.get(Calendar.DAY_OF_WEEK) - 1], 22f, true, Color.WHITE))
        c.addView(a.tv(a.tr("Tháng $tm năm $ty", "Month $tm, $ty"), 15f, false, 0xE6FFFFFF.toInt()))
        r.addView(c); todayBox.addView(r)
        todayBox.addView(a.tv("Âm lịch · ${i.lunar[0]} tháng ${i.lunar[1]} · năm ${i.yearName}", 16f, true, Color.WHITE).apply { setPadding(0, a.dp(6), 0, a.dp(10)) })
        val flow = FlowLayout(a, a.dp(6), a.dp(6))
        val white = 0x33FFFFFF
        flow.addView(chip("Ngày ${i.dayName}", white)); flow.addView(chip("Tháng ${i.monthName}", white)); flow.addView(chip("Năm ${i.yearName}", white))
        if (i.hoangDao) flow.addView(chip("☾ Hoàng đạo · ${i.starName}", 0xFFFFC53D.toInt(), 0xFF4A3300.toInt()))
        else flow.addView(chip("☾ Hắc đạo · ${i.starName}", 0xFF3B3552.toInt()))
        val ev = i.events().firstOrNull()
        if (ev != null) flow.addView(chip("🎉 $ev", 0xFFFF5A7A.toInt()))
        todayBox.addView(flow)
        val (wk, wy) = weekOfYear()
        val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(6), 0, 0) }
        row.addView(chip(a.tr("Tuần $wk - $wy", "Week $wk - $wy"), 0xFF14B8A6.toInt(), 0xFF052E2B.toInt()), LinearLayout.LayoutParams(-2, -2).apply { marginEnd = a.dp(6) })
        row.addView(chip(a.tr("Quý ${(tm - 1) / 3 + 1} - $ty", "Q${(tm - 1) / 3 + 1} - $ty"), 0xFF38BDF8.toInt(), 0xFF05283B.toInt()))
        todayBox.addView(row)
    }

    private fun dot(color: Int): TextView = a.tv("●", 7f, false, color)

    private fun arrow(s: String, d: Int): TextView = a.ctv(s, 24f, true, a.ACC).apply {
        background = a.rounded(a.SEL, 22); setOnClickListener { go(d) }
    }

    private fun drawMonth() {
        monthBox.removeAllViews()
        val y = vy; val m = vm; val n = dim(y, m)
        val showD = if (sy == y && sm == m) sd else 1
        val ls = Almanac.Info(showD, m, y).lunar
        val noted = Notes.all(a).filter { it.y == y && it.m == m }.map { it.d }.toSet()
        val head = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(arrow("‹", -1), LinearLayout.LayoutParams(a.dp(44), a.dp(44)))
        val mid = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(a.ctv(a.tr("Tháng $m • $y", "$m • $y"), 22f, true), LinearLayout.LayoutParams(-1, -2))
        mid.addView(a.ctv("Âm lịch: ${ls[0]} tháng ${ls[1]} năm ${Lunar.yearName(ls[2])}", 12f, false, a.MUTED), LinearLayout.LayoutParams(-1, -2))
        head.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(arrow("›", 1), LinearLayout.LayoutParams(a.dp(44), a.dp(44)))
        monthBox.addView(head)

        val hr = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(10), 0, a.dp(4)) }
        val names = if (Lang.isVi(a)) arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") else arrayOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        for (k in 0..6) hr.addView(a.ctv(names[k], 13f, true, if (k == 6) a.BAD else a.MUTED), LinearLayout.LayoutParams(0, -2, 1f))
        monthBox.addView(hr)
        val first = Calendar.getInstance().apply { clear(); set(y, m - 1, 1) }
        val off = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        for (r in 0..5) {
            if (r * 7 - off + 1 > n) break
            val row = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0..6) {
                val d = r * 7 + c - off + 1
                val cell = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, a.dp(5), 0, a.dp(4)) }
                if (d in 1..n) {
                    val info = Almanac.Info(d, m, y)
                    val sel = d == sd && m == sm && y == sy
                    val isToday = d == td && m == tm && y == ty
                    cell.background = if (sel) a.rounded(a.ACC, 14) else if (isToday) a.rounded(0, 14, a.ACC) else null
                    cell.addView(a.ctv("$d", 18f, true, if (sel) Color.WHITE else if (c == 6) a.BAD else a.INK), LinearLayout.LayoutParams(-1, -2))
                    val ls2 = if (info.lunar[0] == 1) "1/${info.lunar[1]}" else "${info.lunar[0]}"
                    cell.addView(a.ctv(ls2, 11f, info.lunar[0] == 1, if (sel) 0xE6FFFFFF.toInt() else if (info.lunar[0] == 1) a.ACC else a.MUTED), LinearLayout.LayoutParams(-1, -2))
                    val dots = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
                    if (info.events().isNotEmpty()) dots.addView(dot(RED))
                    if (info.hoangDao) dots.addView(dot(GOLD))
                    if (d in noted) dots.addView(dot(if (sel) Color.WHITE else a.ACC))
                    if (dots.childCount == 0) dots.addView(dot(0x00000000))
                    cell.addView(dots, LinearLayout.LayoutParams(-1, -2))
                    cell.setOnClickListener { sy = y; sm = m; sd = d; drawMonth(); drawNotes(); drawDetail(); drawHours() }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
            }
            monthBox.addView(row)
        }
        val lg = FlowLayout(a, a.dp(10), a.dp(2)).apply { setPadding(a.dp(4), a.dp(10), 0, 0) }
        lg.addView(a.tv("● ${a.tr("Lễ / sự kiện", "Event")}", 12f, false, RED))
        lg.addView(a.tv("● ${a.tr("Hoàng đạo", "Auspicious")}", 12f, false, GOLD))
        lg.addView(a.tv("● ${a.tr("Ghi chú", "Note")}", 12f, false, a.ACC))
        lg.addView(a.tv(a.tr("Vuốt ngang để đổi tháng", "Swipe sideways to change month"), 12f, false, a.MUTED))
        monthBox.addView(lg)
    }

    // ---------- ghi chú & nhắc nhở ----------
    private fun drawNotes() {
        notesBox.removeAllViews()
        notesBox.addView(a.tv("${wd(sy, sm, sd)}, $sd/$sm/$sy", 14f, true, a.MUTED))
        val list = Notes.forDay(a, sy, sm, sd)
        if (list.isEmpty()) notesBox.addView(a.tv(a.tr("Chưa có ghi chú cho ngày này", "No notes for this day"), 14f, false, a.MUTED).apply { setPadding(0, a.dp(8), 0, a.dp(8)) })
        for (n in list) {
            val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, a.dp(8), 0, a.dp(8)) }
            if (n.at > 0) {
                val t = Calendar.getInstance().apply { timeInMillis = n.at }
                r.addView(a.tv("⏰ %02d:%02d".format(t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE)), 14f, true, a.ACC), LinearLayout.LayoutParams(-2, -2).apply { marginEnd = a.dp(10) })
            }
            r.addView(a.tv(n.text, 15f), LinearLayout.LayoutParams(0, -2, 1f))
            r.addView(a.tv("✕", 16f, true, a.MUTED).apply { setPadding(a.dp(12), a.dp(4), a.dp(4), a.dp(4)); setOnClickListener { Notes.delete(a, n.id); drawMonth(); drawNotes() } })
            notesBox.addView(r)
        }
        notesBox.addView(a.btn(a.tr("＋ Thêm ghi chú / nhắc nhở", "＋ Add note / reminder"), false) { addNote() }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, a.dp(8), 0, 0) })
    }

    private fun addNote() {
        val root = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(20), a.dp(8), a.dp(20), 0) }
        val et = EditText(a).apply { hint = a.tr("Nội dung ghi chú", "Note"); minLines = 2; setTextColor(a.INK); setHintTextColor(a.MUTED) }
        root.addView(et)
        val cb = CheckBox(a).apply { text = a.tr("Nhắc nhở lúc", "Remind me at"); setTextColor(a.INK) }
        root.addView(cb)
        var hh = 8; var mm = 0
        val tvT = a.tv("08:00", 18f, true, a.ACC).apply { setPadding(a.dp(8), a.dp(8), a.dp(8), a.dp(8)) }
        tvT.setOnClickListener {
            TimePickerDialog(a, { _, h, m -> hh = h; mm = m; tvT.text = "%02d:%02d".format(h, m); cb.isChecked = true }, hh, mm, true).show()
        }
        root.addView(tvT)
        AlertDialog.Builder(a).setTitle(a.tr("Ghi chú ngày $sd/$sm/$sy", "Note for $sd/$sm/$sy")).setView(root)
            .setPositiveButton(a.tr("Lưu", "Save")) { _, _ ->
                val text = et.text.toString().trim()
                if (text.isEmpty()) return@setPositiveButton
                var at = 0L
                if (cb.isChecked) {
                    at = Calendar.getInstance().apply { clear(); set(sy, sm - 1, sd, hh, mm, 0) }.timeInMillis
                    if (Build.VERSION.SDK_INT >= 33 && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 78)
                }
                Notes.add(a, Notes.Note(System.currentTimeMillis(), sy, sm, sd, text, at))
                drawMonth(); drawNotes()
            }.setNegativeButton(a.tr("Hủy", "Cancel"), null).show()
    }

    // ---------- chi tiết ngày ----------
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
        row(false, "Nhị thập\nbát tú", i.tu)
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
                val cell = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(0, a.dp(12), 0, a.dp(12))
                    background = a.rounded(if (g) (if (a.isDark) 0xFF4A3A1A.toInt() else 0xFFFFF1CC.toInt()) else a.TILE, 14, if (isToday && h == curH) a.ACC else 0) }
                cell.addView(a.ctv(Almanac.CHI[h], 17f, true, if (g) (if (a.isDark) 0xFFF5D547.toInt() else 0xFF9A6400.toInt()) else a.MUTED), LinearLayout.LayoutParams(-1, -2))
                cell.addView(a.ctv(Almanac.hourRange(h), 12f, false, a.MUTED), LinearLayout.LayoutParams(-1, -2))
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(a.dp(3), a.dp(3), a.dp(3), a.dp(3)) })
            }
            hoursBox.addView(row)
        }
        hoursBox.addView(a.tv(a.tr("Vàng: giờ hoàng đạo (tốt) · Xám: giờ hắc đạo · Viền xanh: giờ hiện tại", "Gold: auspicious hours · Gray: inauspicious · Blue outline: current hour"), 12f, false, a.MUTED).apply { setPadding(0, a.dp(8), 0, 0) })
    }

    private fun drawEvents() {
        eventsBox.removeAllViews()
        val y = vy; val m = vm; var any = false
        for (d in 1..dim(y, m)) {
            val i = Almanac.Info(d, m, y)
            for (e in i.events()) {
                any = true
                val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, a.dp(8), 0, a.dp(8)) }
                r.addView(a.tv("$d/$m", 17f, true, a.ACC), LinearLayout.LayoutParams(a.dp(64), -2))
                val c = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
                c.addView(a.tv(e, 16f, true))
                c.addView(a.tv("${wd(y, m, d)} · Âm lịch ${i.lunar[0]}/${i.lunar[1]}", 13f, false, a.MUTED))
                r.addView(c, LinearLayout.LayoutParams(0, -2, 1f)); eventsBox.addView(r)
            }
        }
        if (!any) eventsBox.addView(a.tv(a.tr("Không có sự kiện trong tháng $m", "No events in month $m"), 14f, false, a.MUTED))
    }
}
