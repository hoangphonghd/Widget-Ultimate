package com.magicos.widget

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var holder: FrameLayout
    private lateinit var navs: List<TextView>
    private var frame: FrameLayout? = null
    private val ui = Handler(Looper.getMainLooper())
    private val d get() = resources.displayMetrics.density
    private fun dp(x: Int) = (x * d).toInt()
    private val INK = 0xFF111827.toInt()
    private val MUTED = 0xFF6B7280.toInt()
    private val BLUE = 0xFF4A6CF7.toInt()

    private val pushWidgets = Runnable { thread { Render.updateAll(applicationContext) } }
    private fun changed() { renderPreview(); ui.removeCallbacks(pushWidgets); ui.postDelayed(pushWidgets, 400) }

    private fun rounded(fill: Int, r: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(r).toFloat(); if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun tv(s: String, sp: Float = 14f, bold: Boolean = false, col: Int = INK) = TextView(this).apply {
        text = s; textSize = sp; setTextColor(col); if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun card(parent: LinearLayout, title: String? = null): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, 20, 0xFFE3E7F0.toInt())
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
        }
        if (title != null) c.addView(tv(title.uppercase(), 12f, true, MUTED).apply { setPadding(0, 0, 0, dp(8)); letterSpacing = 0.08f })
        parent.addView(c); return c
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFF4F6FB.toInt()); fitsSystemWindows = true }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF3B6FF5.toInt(), 0xFF5B3FD6.toInt())).apply { cornerRadius = dp(28).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(8), dp(12), dp(4)) }
        }
        val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ht.addView(tv("MagicOS Widget", 24f, true, Color.WHITE))
        ht.addView(tv("Đồng hồ · Lịch Việt · Số liệu realtime", 12f, false, 0xFFE0E7FF.toInt()))
        head.addView(ht, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("v2.0", 13f, true, Color.WHITE).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(0x33FFFFFF, 20) })
        root.addView(head)
        holder = FrameLayout(this)
        root.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(0, dp(6), 0, dp(6)) }
        val items = arrayOf("🗓️\nLịch Việt", "⛅\nThời tiết", "🎨\nTùy chỉnh", "🆕\nVersion")
        navs = items.mapIndexed { i, s ->
            tv(s, 12f, true, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6)); setOnClickListener { show(i) } }
        }
        navs.forEach { nav.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
        root.addView(nav)
        setContentView(root)
        show(2)
        thread { Render.updateAll(applicationContext) }
    }

    private fun show(i: Int) {
        navs.forEachIndexed { k, v -> v.setTextColor(if (k == i) BLUE else MUTED); v.background = if (k == i) rounded(0xFFE8EDFF.toInt(), 16) else null }
        holder.removeAllViews(); frame = null
        holder.addView(when (i) { 0 -> calendarPage(); 1 -> weatherPage(); 2 -> customizePage(); else -> versionPage() })
    }

    private fun scroll(col: LinearLayout) = ScrollView(this).apply { addView(col); isVerticalScrollBarEnabled = false }
    private fun colLayout() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(24)) }

    // =============== TÙY CHỈNH ===============
    private fun renderPreview() {
        val f = frame ?: return
        f.removeAllViews()
        try { f.addView(Render.build(this, S.i(this, "pv", 0)).apply(this, f)) } catch (e: Exception) { }
    }

    private fun customizePage(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        // --- xem trước cố định ---
        val pc = card(page, "Xem trước trực tiếp")
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val names = arrayOf("Chuẩn · Sáng", "Chuẩn · Tối", "Liquid Glass")
        val chipViews = ArrayList<TextView>()
        fun paintChips() { chipViews.forEachIndexed { k, v -> val on = S.i(this, "pv", 0) == k
            v.setTextColor(if (on) BLUE else INK); v.background = rounded(if (on) 0xFFE8EDFF.toInt() else Color.WHITE, 18, if (on) BLUE else 0xFFE3E7F0.toInt()) } }
        names.forEachIndexed { k, s ->
            val v = tv(s, 12f, true).apply { gravity = Gravity.CENTER; setPadding(dp(8), dp(8), dp(8), dp(8)); setOnClickListener { S.p(ctx()).edit().putInt("pv", k).apply(); paintChips(); renderPreview() } }
            chipViews.add(v); chips.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        pc.addView(chips); paintChips()
        val fr = FrameLayout(this).apply { background = rounded(0xFF4A74B0.toInt(), 14); setPadding(dp(6), dp(6), dp(6), dp(6)) }
        pc.addView(fr, LinearLayout.LayoutParams(-1, dp(170)).apply { topMargin = dp(10) })
        frame = fr
        pc.addView(tv("Mọi thay đổi áp dụng ngay lên widget ngoài màn hình chính.", 11f, false, MUTED).apply { setPadding(0, dp(6), 0, 0) })
        renderPreview()

        val col = colLayout()
        // --- kiểu hiển thị ---
        val kc = card(col, "Kiểu hiển thị widget")
        val modes = arrayOf("Kiểu mới · Thông số hệ thống" to "Pin, nhiệt độ máy, RAM, ổ cứng cập nhật realtime",
            "Kiểu cổ điển · Thời tiết" to "Thời tiết kèm vị trí và dự báo", "Kiểu đơn giản · Thời tiết" to "Tên thương hiệu, nhiệt độ, vị trí và dự báo gọn")
        val mviews = ArrayList<LinearLayout>()
        fun paintModes() { mviews.forEachIndexed { k, v -> val on = S.i(this, "mode", 0) == k
            v.background = rounded(if (on) 0xFFE8EDFF.toInt() else Color.WHITE, 16, if (on) BLUE else 0xFFE3E7F0.toInt()) } }
        modes.forEachIndexed { k, m ->
            val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12))
                addView(tv(m.first, 15f, true)); addView(tv(m.second, 12f, false, MUTED))
                setOnClickListener { S.p(ctx()).edit().putInt("mode", k).apply(); paintModes(); changed() } }
            mviews.add(v); kc.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        paintModes()

        val cc = card(col, "Đồng hồ")
        sw(cc, "Hiện số giây", "sec", false)

        val tc = card(col, "Tùy biến hiển thị")
        tc.addView(tv("Ngày và lịch", 13f, true, MUTED))
        sw(tc, "Ngày dương lịch", "solar", true); sw(tc, "Âm lịch", "lunar", true); sw(tc, "Lịch tuần (7 ngày phía dưới)", "week", true)
        tc.addView(tv("Bảng số liệu máy", 13f, true, MUTED).apply { setPadding(0, dp(10), 0, 0) })
        sw(tc, "Dòng tên + biểu tượng", "nameRow", true); sw(tc, "Pin", "bt", true); sw(tc, "Nhiệt độ máy", "tp", true)
        sw(tc, "RAM", "rm", true); sw(tc, "Ổ cứng", "dk", true)

        val sc = card(col, "Kích thước")
        sl(sc, "Cỡ đồng hồ", "cs", 50, 150, 100, "%")
        sl(sc, "Độ rộng viền (Liquid Glass)", "bw", 0, 4, 1, " dp")

        val bc = card(col, "Nền widget")
        colorRow(bc, "Nền bản Chuẩn · chế độ sáng", "bgL"); colorRow(bc, "Nền bản Chuẩn · chế độ tối", "bgD")
        bc.addView(tv("Bản Chuẩn tự đổi giữa nền sáng và tối theo Dark mode của hệ thống.", 12f, false, MUTED))

        val gc = card(col, "Liquid Glass")
        sl(gc, "Độ đậm kính", "ga", 0, 100, 25, "%"); colorRow(gc, "Màu phủ kính", "glass")

        val xc = card(col, "Màu chữ")
        colorRow(xc, "Chữ · Chuẩn sáng", "txL"); colorRow(xc, "Chữ · Chuẩn tối", "txD"); colorRow(xc, "Chữ · Liquid Glass", "txG")

        val mc = card(col, "Màu các thông số")
        colorRow(mc, "Pin", "cBt"); colorRow(mc, "Nhiệt độ máy", "cTp"); colorRow(mc, "RAM", "cRm")
        colorRow(mc, "Ổ cứng", "cDk"); colorRow(mc, "Cảnh báo (pin yếu, máy nóng…)", "cWarn")

        val lc = card(col, "Lịch"); colorRow(lc, "Màu nổi ngày hôm nay", "cToday")

        val nc = card(col, "Dòng tên và biểu tượng")
        val et = EditText(this).apply { hint = "Ví dụ: MagicOS"; setText(S.s(ctx(), "name", "")); setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { S.p(ctx()).edit().putString("name", s.toString()).apply(); changed() }
            }) }
        nc.addView(et)
        nc.addView(tv("Để trống sẽ hiện tên máy.", 12f, false, MUTED))
        nc.addView(tv("Biểu tượng", 14f, true).apply { setPadding(0, dp(10), 0, dp(6)) })
        val irow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val iv = ArrayList<TextView>()
        fun paintIco() { iv.forEachIndexed { k, v -> val on = S.i(this, "ico", 1) == k
            v.background = rounded(if (on) 0xFFE8EDFF.toInt() else Color.WHITE, 14, if (on) BLUE else 0xFFE3E7F0.toInt()) } }
        Render.ICONS.forEachIndexed { k, s ->
            val v = tv(if (k == 0) "Ẩn" else s, 16f, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { S.p(ctx()).edit().putInt("ico", k).apply(); paintIco(); changed() } }
            iv.add(v); irow.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        nc.addView(irow); paintIco()
        colorRow(nc, "Màu biểu tượng", "cIco")

        val rc = card(col, "Số liệu realtime")
        val st = Render.stats(this)
        rc.addView(tv("Pin ${st.bl}% (${if (st.chg) "đang sạc" else "không sạc"}) · Nhiệt ${st.bt}°C\nRAM ${st.ramPct}% · Ổ cứng ${st.dkPct}%", 13f, false, MUTED))
        rc.addView(Button(this).apply { text = "Làm mới số liệu ngay"; setOnClickListener { thread { Render.updateAll(applicationContext) }; changed(); Toast.makeText(ctx(), "Đã làm mới", Toast.LENGTH_SHORT).show() } })

        page.addView(scroll(col), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun ctx() = this

    private fun sw(parent: LinearLayout, label: String, key: String, def: Boolean) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        r.addView(tv(label, 15f), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(Switch(this).apply { isChecked = S.b(ctx(), key, def)
            setOnCheckedChangeListener { _, on -> S.p(ctx()).edit().putBoolean(key, on).apply(); changed() } })
        parent.addView(r)
    }

    private fun sl(parent: LinearLayout, label: String, key: String, min: Int, max: Int, def: Int, unit: String) {
        val cur = S.i(this, key, def)
        val t = tv("$label: $cur$unit", 14f).apply { setPadding(0, dp(6), 0, 0) }
        parent.addView(t)
        parent.addView(SeekBar(this).apply { this.max = max - min; progress = cur - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    if (!u) return
                    S.p(ctx()).edit().putInt(key, p + min).apply(); t.text = "$label: ${p + min}$unit"; changed()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            }) })
    }

    private val PALETTE = intArrayOf(0xFF111827.toInt(), 0xFFFFFFFF.toInt(), 0xFFD2691E.toInt(), 0xFFE8503E.toInt(), 0xFFF2A33A.toInt(), 0xFFF5D547.toInt(),
        0xFF5FC768.toInt(), 0xFF5BC8B3.toInt(), 0xFF4A7BF7.toInt(), 0xFF7B7BF5.toInt(), 0xFFB05CE0.toInt(), 0xFFEC6FA5.toInt(),
        0xFF1B2030.toInt(), 0xFFF4F6FB.toInt(), 0xFF9CA3AF.toInt(), 0xFF6B7280.toInt(), 0xFF0EA5E9.toInt(), 0xFF84CC16.toInt())

    private fun colorRow(parent: LinearLayout, label: String, key: String) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        val sw = View(this).apply { background = rounded(Render.col(ctx(), key), 10, 0xFFD1D5DB.toInt()) }
        r.addView(sw, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
        val lt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lt.addView(tv(label, 15f, true))
        val sub = tv(if (S.p(this).contains(key)) "Tùy chỉnh" else "Mặc định", 12f, false, MUTED)
        lt.addView(sub); r.addView(lt)
        r.setOnClickListener {
            val g = GridLayout(this).apply { columnCount = 6; setPadding(dp(12), dp(12), dp(12), dp(4)) }
            lateinit var dlg: AlertDialog
            PALETTE.forEach { c ->
                g.addView(View(this).apply { background = rounded(c, 10, 0xFFD1D5DB.toInt())
                    setOnClickListener { S.p(ctx()).edit().putInt(key, c).apply(); sw.background = rounded(c, 10, 0xFFD1D5DB.toInt()); sub.text = "Tùy chỉnh"; dlg.dismiss(); changed() } },
                    GridLayout.LayoutParams().apply { width = dp(44); height = dp(44); setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            dlg = AlertDialog.Builder(this).setTitle(label).setView(g)
                .setNeutralButton("Mặc định") { _, _ -> S.p(ctx()).edit().remove(key).apply(); sw.background = rounded(Render.col(ctx(), key), 10, 0xFFD1D5DB.toInt()); sub.text = "Mặc định"; changed() }
                .setNegativeButton("Đóng", null).create()
            dlg.show()
        }
        parent.addView(r)
    }

    // =============== LỊCH VIỆT ===============
    private fun calendarPage(): View {
        val col = colLayout()
        val n = Calendar.getInstance(); val l = Lunar.conv(n.get(Calendar.DAY_OF_MONTH), n.get(Calendar.MONTH) + 1, n.get(Calendar.YEAR))
        val tc = card(col, "Hôm nay")
        tc.addView(tv("${Render.WD[n.get(Calendar.DAY_OF_WEEK) - 1]}, ${n.get(Calendar.DAY_OF_MONTH)}/${n.get(Calendar.MONTH) + 1}/${n.get(Calendar.YEAR)}", 20f, true))
        tc.addView(tv("Âm lịch: ${l[0]} tháng ${l[1]} năm ${Lunar.yearName(l[2])} · ${Lunar.hourName(n.get(Calendar.HOUR_OF_DAY))}", 14f, false, MUTED))
        val cc = card(col)
        val cur = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val title = tv("", 16f, true).apply { gravity = Gravity.CENTER }
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun draw() {
            val y = cur.get(Calendar.YEAR); val m = cur.get(Calendar.MONTH) + 1
            title.text = "Tháng $m / $y"; grid.removeAllViews()
            fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }.also { grid.addView(it) }
            val hr = row()
            arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { hr.addView(tv(it, 12f, true, if (it == "CN") 0xFFE8503E.toInt() else MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6)) }, LinearLayout.LayoutParams(0, -2, 1f)) }
            val off = (cur.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val dim = cur.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (r in 0..5) {
                if (r * 7 - off + 1 > dim) break
                val rw = row()
                for (c in 0..6) {
                    val day = r * 7 + c - off + 1
                    val cell = TextView(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6)) }
                    if (day in 1..dim) {
                        val ll = Lunar.conv(day, m, y)
                        val ls = if (ll[0] == 1) "${ll[0]}/${ll[1]}" else "${ll[0]}"
                        val sp = SpannableString("$day\n$ls"); sp.setSpan(RelativeSizeSpan(0.65f), "$day\n".length, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cell.text = sp; textSizeOf(cell, 15f)
                        val today = day == n.get(Calendar.DAY_OF_MONTH) && m == n.get(Calendar.MONTH) + 1 && y == n.get(Calendar.YEAR)
                        cell.setTextColor(if (today) Color.WHITE else if (c == 6) 0xFFE8503E.toInt() else INK)
                        if (today) cell.background = rounded(Render.col(this, "cToday"), 12)
                    }
                    rw.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
                }
            }
        }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nav.addView(Button(this).apply { text = "‹"; setOnClickListener { cur.add(Calendar.MONTH, -1); draw() } })
        nav.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(Button(this).apply { text = "›"; setOnClickListener { cur.add(Calendar.MONTH, 1); draw() } })
        cc.addView(nav); cc.addView(grid); draw()
        return scroll(col)
    }
    private fun textSizeOf(t: TextView, sp: Float) { t.textSize = sp }

    // =============== THỜI TIẾT ===============
    private fun weatherPage(): View {
        val col = colLayout()
        val cc = card(col, "Vị trí")
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Render.CITY)
        sp.setSelection(S.i(this, "city", 1))
        val big = tv("", 44f, true); val desc = tv("", 16f, true); val rng = tv("", 13f, false, MUTED); val sun = tv("", 13f, false, MUTED)
        fun fill() {
            val p = S.p(this); val ok = Render.hasWx(this)
            big.text = Render.tempStr(this)
            desc.text = if (ok) "${Render.wxIcon(p.getInt("code", 0))} ${Render.wxText(p.getInt("code", 0))}" else "Đang tải…"
            rng.text = Render.rangeStr(this)
            sun.text = if (ok) "🌅 ${p.getString("sr", "--")}   🌇 ${p.getString("ss", "--")}" else ""
        }
        fun reload() { thread { val ok = Render.refreshWeather(applicationContext, true); Render.updateAll(applicationContext)
            ui.post { fill(); if (!ok) Toast.makeText(this, "Không tải được thời tiết, kiểm tra mạng", Toast.LENGTH_SHORT).show() } } }
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos != S.i(ctx(), "city", 1)) { S.p(ctx()).edit().putInt("city", pos).putLong("wt", 0).apply(); fill(); reload() }
            }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }
        cc.addView(sp)
        val wc = card(col, "Hiện tại")
        wc.addView(big); wc.addView(desc); wc.addView(rng); wc.addView(sun)
        wc.addView(Button(this).apply { text = "Làm mới thời tiết"; setOnClickListener { reload() } })
        fill(); thread { Render.refreshWeather(applicationContext, false); ui.post { fill() } }
        return scroll(col)
    }

    // =============== VERSION ===============
    private fun versionPage(): View {
        val col = colLayout()
        val hc = card(col, "Cách thêm widget")
        hc.addView(tv("1. Nhấn giữ vào khoảng trống trên màn hình chính\n2. Chọn MagicOS Widget: bản Chuẩn hoặc Liquid Glass\n3. Chọn kích thước 4×2 rồi kéo ra màn hình", 15f))
        val ac = card(col, "Tác giả")
        ac.addView(tv("Designer by MagicOS Ultimate", 18f, true))
        ac.addView(tv("MagicOS Widget · Phiên bản 2.0", 13f, false, MUTED))
        val vc = card(col, "Ghi chú")
        vc.addView(tv("Widget tự đổi sáng/tối theo Dark mode (bản Chuẩn). Số liệu pin, nhiệt độ, RAM, ổ cứng và thời tiết cập nhật định kỳ khoảng 15 phút; vào tab Tùy chỉnh bấm \"Làm mới số liệu ngay\" để cập nhật liền. Nên cho app chạy nền không bị hạn chế pin.", 13f, false, MUTED))
        return scroll(col)
    }
}
