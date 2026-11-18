package com.magicos.widget

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : UiBase() {
    private lateinit var holder: FrameLayout
    private lateinit var navs: List<TextView>
    private var page = 3
    private var frame: FrameLayout? = null
    private var weather: WeatherPage? = null
    private val pushWidgets = Runnable { thread { Render.updateAll(applicationContext, false) } }
    private fun changed() { renderPreview(); ui.removeCallbacks(pushWidgets); ui.postDelayed(pushWidgets, 400) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setupBars()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); fitsSystemWindows = true }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                if (isDark) intArrayOf(0xFF2A47B8.toInt(), 0xFF3A2D96.toInt()) else intArrayOf(0xFF3B6FF5.toInt(), 0xFF5B3FD6.toInt())).apply { cornerRadius = dp(28).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(8), dp(12), dp(4)) }
        }
        val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ht.addView(tv("MagicOS Ultimate", 24f, true, Color.WHITE))
        ht.addView(tv("Đồng hồ · Lịch Việt · Số liệu realtime", 12f, false, 0xFFE0E7FF.toInt()))
        head.addView(ht, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("v3.0", 13f, true, Color.WHITE).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(0x33FFFFFF, 20) })
        root.addView(head)
        holder = FrameLayout(this)
        root.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(NAV); setPadding(0, dp(4), 0, dp(4)) }
        val items = arrayOf("🗓️\nLịch Việt", "⛅\nThời tiết", "🎨\nTùy chỉnh", "🧩\nWidget", "🆕\nVersion")
        navs = items.mapIndexed { i, s ->
            tv(s, 11f, true, MUTED).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(6)); setOnClickListener { show(i) } }
        }
        navs.forEach { nav.addView(it, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }) }
        root.addView(nav)
        setContentView(root)
        show(3)
        thread { Render.updateAll(applicationContext, false) }
    }

    override fun onResume() { super.onResume(); if (page == 3) show(3) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        weather?.onPermission(grantResults.any { it == PackageManager.PERMISSION_GRANTED })
    }

    /** Thanh trạng thái / điều hướng trong suốt theo màu nền sáng-tối của hệ thống */
    private fun setupBars() {
        window.statusBarColor = BG
        window.navigationBarColor = NAV
        val ls = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val ln = if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        var f = window.decorView.systemUiVisibility
        f = if (isDark) f and (ls or ln).inv() else f or ls or ln
        window.decorView.systemUiVisibility = f
    }

    private fun show(i: Int) {
        page = i
        navs.forEachIndexed { k, v -> v.setTextColor(if (k == i) ACC else MUTED); v.background = if (k == i) rounded(SEL, 16) else null }
        holder.removeAllViews(); frame = null; weather = null
        holder.addView(when (i) {
            0 -> CalendarPage(this).build()
            1 -> WeatherPage(this).also { weather = it }.build()
            2 -> customizePage()
            3 -> widgetPage()
            else -> versionPage()
        })
    }

    // =============== TAB WIDGET ===============
    private class Kind(val chip: String, val desc: String, val cls: Class<*>, val look: Int, val btn: String)

    private fun widgetPage(): View {
        val col = colLayout()
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                if (isDark) intArrayOf(0xFF3347B8.toInt(), 0xFF4B3FC4.toInt()) else intArrayOf(0xFF4A6CF7.toInt(), 0xFF6B4FE8.toInt())).apply { cornerRadius = dp(26).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
        }
        hero.addView(tv("THÊM WIDGET", 12f, true, 0xCCFFFFFF.toInt()))
        hero.addView(tv("Đưa widget ra màn hình chính", 22f, true, Color.WHITE).apply { setPadding(0, dp(4), 0, dp(6)) })
        hero.addView(tv("Chọn kiểu bên dưới và chạm một lần. Màn hình chính sẽ hỏi vị trí đặt widget — không cần nhấn giữ hay tìm trong danh sách.", 14f, false, 0xE6FFFFFF.toInt()))
        col.addView(hero)

        // Các kiểu widget sau này sẽ được thêm vào danh sách này
        val kinds = listOf(
            Kind("LIQUID GLASS", "Nền kính mờ trong suốt theo hình nền", GlassWidget::class.java, 2, "Liquid Glass"),
            Kind("BASIC", "Nền chuẩn, tự đổi sáng / tối theo hệ thống", StdWidget::class.java, -1, "Basic"))
        val mgr = AppWidgetManager.getInstance(this)
        kinds.forEach { k ->
            val c = card(col)
            c.addView(tv(k.chip, 13f, true, ACC).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(SEL, 14) })
            c.addView(tv(k.desc, 15f).apply { setPadding(0, dp(10), 0, dp(10)) })
            val f = FrameLayout(this).apply { background = rounded(if (k.look == 2) 0xFF5B7FD6.toInt() else TILE, 16); setPadding(dp(4), dp(4), dp(4), dp(4)) }
            c.addView(f, LinearLayout.LayoutParams(-1, dp(170)))
            try { f.addView(Render.build(this, if (k.look >= 0) k.look else if (isDark) 1 else 0).apply(this, f)) } catch (e: Exception) { }
            val n = mgr.getAppWidgetIds(ComponentName(this, k.cls)).size
            c.addView(tv(if (n > 0) "✓ Đã có $n widget trên màn hình" else "Chưa thêm widget này", 14f, false, if (n > 0) GOOD else MUTED).apply { setPadding(0, dp(10), 0, 0) })
            c.addView(btn("+  Thêm ${k.btn} ra màn hình", true) {
                if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(this, k.cls), null, null)
                else toast("Launcher không hỗ trợ thêm nhanh. Hãy nhấn giữ màn hình chính → Widget → MagicOS Ultimate")
            }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(10), 0, 0) })
        }
        val hc = card(col, "Không thấy hộp thoại thêm widget?")
        hc.addView(tv("Một số launcher không hỗ trợ thêm nhanh. Khi đó: nhấn giữ vào chỗ trống trên màn hình chính → chọn Widget → tìm \"MagicOS Ultimate\" rồi kéo ra màn hình. Chạm vào widget để làm mới dữ liệu. Bạn có thể đổi màu, chữ, bố cục ở tab Tùy chỉnh / Thời tiết — widget sẽ tự cập nhật.", 14f))
        return scroll(col)
    }

    // =============== TAB VERSION ===============
    private fun versionPage(): View {
        val col = colLayout()
        val hc = card(col, "Cách thêm widget")
        hc.addView(tv("1. Mở tab Widget, bấm \"Thêm ... ra màn hình\"\n2. Hoặc nhấn giữ vào khoảng trống trên màn hình chính\n3. Chọn MagicOS Ultimate: Basic hoặc Liquid Glass, kích thước 4×2, rồi kéo ra màn hình", 15f))
        val ac = card(col, "Tác giả")
        ac.addView(tv("Designer by MagicOS Ultimate", 18f, true))
        ac.addView(tv("MagicOS Ultimate · Phiên bản 3.0", 13f, false, MUTED))
        val vc = card(col, "Ghi chú")
        vc.addView(tv("Widget tự đổi sáng/tối theo Dark mode (bản Basic). Chạm vào widget để làm mới pin, nhiệt độ, RAM, ổ cứng và thời tiết; ngoài ra dữ liệu tự cập nhật định kỳ khoảng 15 phút. Nên để app chạy nền không bị hạn chế pin để widget luôn cập nhật.", 13f, false, MUTED))
        return scroll(col)
    }

    // =============== TAB TÙY CHỈNH ===============
    private fun renderPreview() {
        val f = frame ?: return
        f.removeAllViews()
        val pv = S.i(this, "pv", 0)
        try { f.addView(Render.build(this, pv).apply(this, f)) } catch (e: Exception) { }
    }

    private fun customizePage(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pc = card(page, "Xem trước trực tiếp")
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipViews = ArrayList<TextView>()
        fun paintChips() { chipViews.forEachIndexed { k, v -> val on = S.i(this, "pv", 0) == k
            v.setTextColor(if (on) ACC else INK); v.background = rounded(if (on) SEL else CARD, 18, if (on) ACC else LINE) } }
        arrayOf("Chuẩn · Sáng", "Chuẩn · Tối", "Liquid Glass").forEachIndexed { k, s ->
            val v = tv(s, 12f, true).apply { gravity = Gravity.CENTER; setPadding(dp(6), dp(8), dp(6), dp(8)) }
            v.setOnClickListener { S.p(this).edit().putInt("pv", k).apply(); paintChips(); renderPreview() }
            chipViews.add(v); chips.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        pc.addView(chips); paintChips()
        val fr = FrameLayout(this).apply { background = rounded(0xFF4A74B0.toInt(), 14); setPadding(dp(4), dp(4), dp(4), dp(4)) }
        pc.addView(fr, LinearLayout.LayoutParams(-1, dp(170)).apply { topMargin = dp(10) })
        frame = fr
        pc.addView(tv("Mọi thay đổi áp dụng ngay lên widget ngoài màn hình chính.", 11f, false, MUTED).apply { setPadding(0, dp(6), 0, 0) })
        renderPreview()

        val col = colLayout()
        // --- kiểu hiển thị ---
        val kc = card(col, "Kiểu hiển thị widget")
        val modes = arrayOf("Kiểu mới · Thông số hệ thống" to "Pin, nhiệt độ máy, RAM, ổ cứng cập nhật realtime",
            "Kiểu cổ điển · Thời tiết" to "Thời tiết kèm vị trí và nhiệt độ", "Kiểu đơn giản · Thời tiết" to "Tên thương hiệu, nhiệt độ, vị trí và dự báo gọn")
        val mviews = ArrayList<LinearLayout>()
        val note = tv("", 12f, false, MUTED)
        lateinit var statsCard: LinearLayout
        fun paintModes() {
            val cur = S.i(this, "mode", 0)
            mviews.forEachIndexed { k, v -> v.background = rounded(if (cur == k) SEL else CARD, 16, if (cur == k) ACC else LINE) }
            note.text = "Đang chỉnh cho kiểu: ${arrayOf("Mới", "Cổ điển", "Đơn giản")[cur]}. Chỉ hiện các mục dùng được ở kiểu này."
            statsCard.visibility = if (cur == 0) View.VISIBLE else View.GONE
        }
        modes.forEachIndexed { k, m ->
            val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
            v.addView(tv(m.first, 15f, true)); v.addView(tv(m.second, 12f, false, MUTED))
            v.setOnClickListener { S.p(this).edit().putInt("mode", k).apply(); paintModes(); changed() }
            mviews.add(v); kc.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        kc.addView(note)

        val cc = card(col, "Đồng hồ")
        sw(cc, "Hiện số giây (chạy mỗi giây, tốn pin hơn)", "sec", false)

        val tc = card(col, "Tùy biến hiển thị")
        tc.addView(tv("Ngày và lịch", 13f, true, MUTED))
        sw(tc, "Ngày dương lịch", "solar", true); sw(tc, "Âm lịch", "lunar", true); sw(tc, "Lịch tuần (7 ngày phía dưới)", "week", true)

        statsCard = card(col, "Bảng số liệu máy")
        sw(statsCard, "Dòng tên + biểu tượng", "nameRow", true); sw(statsCard, "Pin", "bt", true); sw(statsCard, "Nhiệt độ máy", "tp", true)
        sw(statsCard, "RAM", "rm", true); sw(statsCard, "Ổ cứng", "dk", true)
        paintModes()

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

        val nc = card(col, "Dòng tên & biểu tượng")
        val et = EditText(this).apply { hint = "Ví dụ: MagicOS Ultimate"; setText(S.s(this@MainActivity, "name", "")); setSingleLine()
            setTextColor(INK); setHintTextColor(MUTED) }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { S.p(this@MainActivity).edit().putString("name", s.toString()).apply(); changed() }
        })
        nc.addView(et)
        nc.addView(tv("Để trống sẽ hiện tên máy. Dòng này nằm cạnh biểu tượng ở góc trên của cụm thông số.", 12f, false, MUTED))
        nc.addView(tv("Biểu tượng", 14f, true).apply { setPadding(0, dp(10), 0, dp(6)) })
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val irow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val iv = ArrayList<TextView>()
        fun paintIco() { iv.forEachIndexed { k, v -> val on = S.i(this, "ico", 1) == k; v.background = rounded(if (on) SEL else CARD, 14, if (on) ACC else LINE) } }
        Render.ICONS.forEachIndexed { k, s ->
            val v = tv(if (k == 0) "Ẩn" else s, 18f, true).apply { gravity = Gravity.CENTER; setPadding(dp(16), dp(10), dp(16), dp(10)) }
            v.setOnClickListener { S.p(this).edit().putInt("ico", k).apply(); paintIco(); changed() }
            iv.add(v); irow.addView(v, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        hs.addView(irow); nc.addView(hs); paintIco()
        colorRow(nc, "Màu biểu tượng", "cIco")
        sw(nc, "Biểu tượng nhấp nháy", "blink", false)

        val rc = card(col, "Số liệu realtime")
        val diag = tv("", 13f, false, MUTED)
        fun fillDiag() {
            val st = Render.stats(this); val lu = S.p(this).getLong("lu", 0)
            val ago = if (lu == 0L) "chưa có" else "${(System.currentTimeMillis() - lu) / 1000} giây trước"
            diag.text = "Cập nhật widget gần nhất: $ago\nNguồn số liệu: Pin ${st.bl}% (BatteryManager) · Sạc: ${if (st.chg) "có" else "không"} · Nhiệt: ${st.bt}°C\nRAM ${st.ramPct}% · Ổ cứng ${st.dkPct}%"
        }
        fillDiag(); rc.addView(diag)
        rc.addView(btn("Làm mới số liệu ngay", false) {
            thread { Render.updateAll(applicationContext, true); ui.post { fillDiag(); renderPreview(); toast("Đã làm mới") } }
        })

        col.addView(btn("Khôi phục mặc định", false) {
            AlertDialog.Builder(this).setTitle("Khôi phục mặc định?").setMessage("Đưa toàn bộ màu, kích thước, công tắc về mặc định. Vị trí thời tiết được giữ nguyên.")
                .setPositiveButton("Khôi phục") { _, _ ->
                    val keep = setOf("place", "lat", "lon", "wjson", "wt", "t", "code", "day", "hi", "lo", "pop", "sr", "ss", "gps", "asked", "lu")
                    val e = S.p(this).edit(); S.p(this).all.keys.filter { it !in keep }.forEach { e.remove(it) }; e.apply()
                    show(2); changed()
                }.setNegativeButton("Hủy", null).show()
        })

        page.addView(scroll(col), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun sw(parent: LinearLayout, label: String, key: String, def: Boolean) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        r.addView(tv(label, 15f), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(Switch(this).apply { isChecked = S.b(this@MainActivity, key, def)
            setOnCheckedChangeListener { _, on -> S.p(this@MainActivity).edit().putBoolean(key, on).apply(); changed() } })
        parent.addView(r)
    }

    private fun sl(parent: LinearLayout, label: String, key: String, min: Int, max: Int, def: Int, unit: String) {
        val cur = S.i(this, key, def)
        val t = tv("$label: $cur$unit", 14f).apply { setPadding(0, dp(6), 0, 0) }
        parent.addView(t)
        val sb = SeekBar(this)
        sb.max = max - min; sb.progress = cur - min
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                if (!u) return
                S.p(this@MainActivity).edit().putInt(key, p + min).apply(); t.text = "$label: ${p + min}$unit"; changed()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        parent.addView(sb)
    }

    private val PALETTE = intArrayOf(0xFF111827.toInt(), 0xFFFFFFFF.toInt(), 0xFF34C759.toInt(), 0xFFFF9F0A.toInt(), 0xFFFF453A.toInt(), 0xFF3B82F6.toInt(),
        0xFF8B7BFF.toInt(), 0xFF1FC8B4.toInt(), 0xFFEC4899.toInt(), 0xFFF5D547.toInt(), 0xFFD2691E.toInt(), 0xFF7DD3FC.toInt(),
        0xFF1B2030.toInt(), 0xFFF4F6FB.toInt(), 0xFF9CA3AF.toInt(), 0xFF6B7280.toInt(), 0xFF0EA5E9.toInt(), 0xFF84CC16.toInt())

    private fun hexOf(c: Int): String = if (Color.alpha(c) == 255) String.format("#%06X", c and 0xFFFFFF) else String.format("#%08X", c)

    private fun colorRow(parent: LinearLayout, label: String, key: String) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        val swatch = View(this).apply { background = rounded(Render.col(this@MainActivity, key), 10, LINE) }
        r.addView(swatch, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
        val lt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lt.addView(tv(label, 15f, true))
        val sub = tv(if (S.p(this).contains(key)) "Tùy chỉnh" else "Mặc định", 12f, false, MUTED)
        lt.addView(sub); r.addView(lt)
        r.setOnClickListener {
            pickColor(label, key) {
                swatch.background = rounded(Render.col(this, key), 10, LINE)
                sub.text = if (S.p(this).contains(key)) "Tùy chỉnh" else "Mặc định"; changed()
            }
        }
        parent.addView(r)
    }

    /** Hộp chọn màu: mã HEX, sắc độ, độ đậm, độ sáng, (độ đặc) và bảng màu có sẵn */
    private fun pickColor(label: String, key: String, done: () -> Unit) {
        val withAlpha = key in setOf("bgL", "bgD", "glass", "cToday")
        val start = Render.col(this, key)
        val tmp = FloatArray(3)
        Color.colorToHSV(start, tmp)
        var h = tmp[0]; var s = tmp[1]; var v = tmp[2]; var a = Color.alpha(start)
        var busy = false
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val prev = View(this)
        root.addView(prev, LinearLayout.LayoutParams(-1, dp(48)))
        val hex = EditText(this).apply { setSingleLine(); setTextColor(INK) }
        root.addView(hex)
        fun cur(): Int = Color.HSVToColor(a, floatArrayOf(h, s, v))
        val sbH = SeekBar(this).apply { max = 360 }; val sbS = SeekBar(this).apply { max = 100 }
        val sbV = SeekBar(this).apply { max = 100 }; val sbA = SeekBar(this).apply { max = 100 }
        fun refresh(updHex: Boolean) {
            prev.background = rounded(cur(), 12, LINE)
            if (updHex) { busy = true; hex.setText(hexOf(cur())); busy = false }
        }
        fun load(c: Int, updHex: Boolean) {
            Color.colorToHSV(c, tmp); h = tmp[0]; s = tmp[1]; v = tmp[2]; a = Color.alpha(c)
            sbH.progress = h.toInt(); sbS.progress = (s * 100).toInt(); sbV.progress = (v * 100).toInt(); sbA.progress = a * 100 / 255
            refresh(updHex)
        }
        fun slider(name: String, sb: SeekBar, onChange: (Int) -> Unit) {
            root.addView(tv(name, 14f, true, MUTED).apply { setPadding(0, dp(8), 0, 0) })
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(x: SeekBar?, p: Int, u: Boolean) { if (u) { onChange(p); refresh(true) } }
                override fun onStartTrackingTouch(x: SeekBar?) {}
                override fun onStopTrackingTouch(x: SeekBar?) {}
            })
            root.addView(sb)
        }
        slider("Sắc độ", sbH) { h = it.toFloat() }
        slider("Độ đậm", sbS) { s = it / 100f }
        slider("Độ sáng", sbV) { v = it / 100f }
        if (withAlpha) slider("Độ đặc", sbA) { a = it * 255 / 100 }
        hex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(x: CharSequence?, p: Int, q: Int, r: Int) {}
            override fun onTextChanged(x: CharSequence?, p: Int, q: Int, r: Int) {}
            override fun afterTextChanged(x: Editable?) {
                if (busy) return
                try { load(Color.parseColor(x.toString().trim()), false) } catch (e: Exception) { }
            }
        })
        val g = GridLayout(this).apply { columnCount = 6; setPadding(0, dp(12), 0, dp(4)) }
        PALETTE.forEach { c ->
            val sq = View(this).apply { background = rounded(c, 10, LINE) }
            sq.setOnClickListener { load(if (withAlpha) ((a shl 24) or (c and 0xFFFFFF)) else c, true) }
            g.addView(sq, GridLayout.LayoutParams().apply { width = dp(40); height = dp(40); setMargins(dp(3), dp(3), dp(3), dp(3)) })
        }
        root.addView(g)
        load(start, true)
        AlertDialog.Builder(this).setTitle(label).setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton("Chọn") { _, _ -> S.p(this).edit().putInt(key, cur()).apply(); done() }
            .setNeutralButton("Mặc định") { _, _ -> S.p(this).edit().remove(key).apply(); done() }
            .setNegativeButton("Hủy", null).show()
    }
}
