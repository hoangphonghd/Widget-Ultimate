package com.magicos.widget

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : UiBase() {
    private lateinit var holder: FrameLayout
    private lateinit var nav: LiquidNav
    private var page = 3
    private var frame: FrameLayout? = null
    private var weather: WeatherPage? = null
    private val UPDATE_URL = ""   // dán link file update.json khi phát hành
    private val pushWidgets = Runnable { thread { Render.updateAll(applicationContext, false) } }
    private fun changed() { renderPreview(); ui.removeCallbacks(pushWidgets); ui.postDelayed(pushWidgets, 400) }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setupBars()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); fitsSystemWindows = true }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                if (isDark) intArrayOf(0xFF1F3A9E.toInt(), 0xFF352A8A.toInt()) else intArrayOf(0xFF3B6FF5.toInt(), 0xFF5B3FD6.toInt())).apply { cornerRadius = dp(28).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(8), dp(12), dp(4)) }
        }
        val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ht.addView(tv("MagicOS Ultimate", 24f, true, Color.WHITE))
        ht.addView(tv(tr("Đồng hồ • Lịch Việt • Thời tiết • Số liệu realtime", "Clock • Vietnamese calendar • Weather • Live stats"), 12f, false, 0xFFE0E7FF.toInt()))
        head.addView(ht, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv("v4.0", 13f, true, Color.WHITE).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(0x33FFFFFF, 20) })
        root.addView(head)
        holder = FrameLayout(this)
        root.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f))
        val items = listOf("🗓️\n" + tr("Lịch", "Calendar"), "⛅\n" + tr("Thời tiết", "Weather"), "🎨\n" + tr("Tùy chỉnh", "Customize"),
            "🧩\n" + tr("Tiện ích", "Widgets"), "ℹ️\n" + tr("Phiên bản", "Version"))
        nav = LiquidNav(this, items) { show(it) }.apply { dark = isDark; accent = ACC; muted = MUTED }
        root.addView(FrameLayout(this).apply { setPadding(dp(14), dp(4), dp(14), dp(8)); addView(nav) })
        setContentView(root)
        show(3)
        thread { Render.updateAll(applicationContext, false); Notes.rescheduleAll(applicationContext) }
    }

    override fun onResume() { super.onResume(); if (page == 3) show(3) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77) weather?.onPermission(grantResults.any { it == PackageManager.PERMISSION_GRANTED })
    }

    /** Thanh trạng thái / điều hướng cùng màu nền sáng-tối của hệ thống */
    private fun setupBars() {
        window.statusBarColor = BG
        window.navigationBarColor = BG
        val ls = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val ln = if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        var f = window.decorView.systemUiVisibility
        f = if (isDark) f and (ls or ln).inv() else f or ls or ln
        window.decorView.systemUiVisibility = f
    }

    private fun show(i: Int) {
        page = i
        nav.select(i)
        holder.removeAllViews(); frame = null; weather = null
        holder.addView(when (i) {
            0 -> CalendarPage(this).build()
            1 -> WeatherPage(this).also { weather = it }.build()
            2 -> customizePage()
            3 -> widgetPage()
            else -> versionPage()
        })
    }

    private fun previewInto(f: FrameLayout, look: Int, hDp: Float) {
        f.removeAllViews()
        val wd = if (f.width > 0) f.width / resources.displayMetrics.density else 320f
        try { f.addView(Render.build(this, look, wd, hDp).apply(this, f)) } catch (e: Exception) { }
    }

    // =============== TAB TIỆN ÍCH ===============
    private class Kind(val chip: String, val desc: String, val cls: Class<*>, val look: Int)

    private fun widgetPage(): View {
        val col = colLayout()
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                if (isDark) intArrayOf(0xFF2A3AA8.toInt(), 0xFF4234B0.toInt()) else intArrayOf(0xFF4A6CF7.toInt(), 0xFF6B4FE8.toInt())).apply { cornerRadius = dp(26).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(6), dp(12), dp(6)) }
        }
        hero.addView(tv(tr("THÊM TIỆN ÍCH", "ADD WIDGET"), 12f, true, 0xCCFFFFFF.toInt()))
        hero.addView(tv(tr("Đưa tiện ích ra màn hình chính", "Put a widget on your home screen"), 22f, true, Color.WHITE).apply { setPadding(0, dp(4), 0, dp(6)) })
        hero.addView(tv(tr("Chọn kiểu bên dưới và chạm một lần. Màn hình chính sẽ hỏi vị trí đặt tiện ích — không cần nhấn giữ hay tìm trong danh sách.",
            "Pick a style below and tap once. Your home screen will ask where to place it."), 14f, false, 0xE6FFFFFF.toInt()))
        col.addView(hero)

        // Các kiểu tiện ích miễn phí (các kiểu mới sẽ thêm vào danh sách này)
        val kinds = listOf(
            Kind(tr("KÍNH LỎNG", "LIQUID GLASS"), tr("Nền kính mờ trong suốt theo hình nền", "Translucent glass over your wallpaper"), GlassWidget::class.java, 2),
            Kind(tr("MẶC ĐỊNH", "DEFAULT"), tr("Nền chuẩn, tự đổi sáng / tối theo hệ thống", "Standard, follows the system light/dark mode"), StdWidget::class.java, -1))
        val mgr = AppWidgetManager.getInstance(this)
        kinds.forEach { k ->
            val c = card(col)
            c.addView(tv(k.chip, 13f, true, ACC).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(SEL, 14) })
            c.addView(tv(k.desc, 15f).apply { setPadding(0, dp(10), 0, dp(10)) })
            val f = FrameLayout(this)
            c.addView(f, LinearLayout.LayoutParams(-1, dp(150)))
            val look = if (k.look >= 0) k.look else if (isDark) 1 else 0
            f.post { previewInto(f, look, 150f) }
            val n = mgr.getAppWidgetIds(ComponentName(this, k.cls)).size
            c.addView(tv(if (n > 0) tr("✓ Đã có $n tiện ích trên màn hình", "✓ $n on your home screen") else tr("Chưa thêm tiện ích này", "Not added yet"), 14f, false, if (n > 0) GOOD else MUTED).apply { setPadding(0, dp(10), 0, 0) })
            c.addView(btn(tr("＋  Thêm tiện ích ra màn hình chính", "＋  Add to home screen"), true) {
                if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(this, k.cls), null, null)
                else toast(tr("Launcher không hỗ trợ thêm nhanh. Hãy nhấn giữ màn hình chính → Tiện ích → MagicOS Ultimate", "Your launcher can't pin directly. Long-press the home screen → Widgets → MagicOS Ultimate"))
            }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(10), 0, 0) })
        }

        col.addView(tv(tr("TIỆN ÍCH CAO CẤP", "PREMIUM WIDGETS"), 12f, true, MUTED).apply { letterSpacing = 0.08f; setPadding(dp(20), dp(14), dp(20), dp(2)) })
        val vips = listOf(Triple("VIP 1", "4×2", 150), Triple("VIP 2", "4×2", 150), Triple("VIP 3", "4×2", 150), Triple("VIP 4", "4×3", 220), Triple("VIP 5", "4×4", 290))
        vips.forEach { v ->
            val c = card(col)
            c.addView(tv("${v.first} · ${v.second}", 13f, true, 0xFFD97706.toInt()).apply { setPadding(dp(12), dp(6), dp(12), dp(6)); background = rounded(if (isDark) 0xFF3A2A10.toInt() else 0xFFFFF1D6.toInt(), 14) })
            val f = FrameLayout(this).apply { background = rounded(TILE, 18, LINE) }
            f.addView(ctv(tr("CHƯA MỞ KHOÁ", "LOCKED"), 22f, true, 0xFFE53935.toInt()).apply { letterSpacing = 0.1f },
                FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.CENTER })
            c.addView(f, LinearLayout.LayoutParams(-1, dp(v.third)).apply { topMargin = dp(10) })
            c.addView(tv(tr("Ảnh minh họa và giá sẽ cập nhật khi ra mắt", "Preview and price will appear at launch"), 13f, false, MUTED).apply { setPadding(0, dp(10), 0, 0) })
            c.addView(btn(tr("🔒  Mở khoá (sắp ra mắt)", "🔒  Unlock (coming soon)"), false) { showPay() }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(8), 0, 0) })
        }
        val hc = card(col, tr("Không thấy hộp thoại thêm tiện ích?", "No add-widget dialog?"))
        hc.addView(tv(tr("Một số launcher không hỗ trợ thêm nhanh. Khi đó: nhấn giữ vào chỗ trống trên màn hình chính → chọn Tiện ích → tìm \"MagicOS Ultimate\" rồi kéo ra màn hình. Chạm vào tiện ích để làm mới dữ liệu.",
            "Some launchers can't pin directly. Long-press an empty spot on the home screen → Widgets → find \"MagicOS Ultimate\" and drag it out. Tap a widget to refresh its data."), 14f))
        return scroll(col)
    }

    // =============== TAB PHIÊN BẢN ===============
    @Suppress("DEPRECATION")
    private fun versionInfo(): String = try { val pi = packageManager.getPackageInfo(packageName, 0); "${pi.versionName} (${pi.versionCode})" } catch (e: Exception) { "4.0" }

    private fun versionPage(): View {
        val col = colLayout()
        val lc = card(col, tr("Ngôn ngữ", "Language"))
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cv = ArrayList<TextView>()
        fun paint() { cv.forEachIndexed { k, v -> val on = S.i(this, "lang", 0) == k; v.setTextColor(if (on) ACC else INK); v.background = rounded(if (on) SEL else CARD, 16, if (on) ACC else LINE) } }
        arrayOf(tr("Tự động", "Auto"), "Tiếng Việt", "English").forEachIndexed { k, s ->
            val v = tv(s, 14f, true).apply { gravity = Gravity.CENTER; setPadding(dp(6), dp(10), dp(6), dp(10)) }
            v.setOnClickListener { if (S.i(this, "lang", 0) != k) { S.p(this).edit().putInt("lang", k).apply(); thread { Render.updateAll(applicationContext, false) }; recreate() } }
            cv.add(v); chips.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        lc.addView(chips); paint()
        lc.addView(tv(tr("Menu và các nhãn chính đổi theo ngôn ngữ. Nội dung lịch vạn niên luôn bằng tiếng Việt.", "Menus and main labels follow the language. Almanac content is always Vietnamese."), 12f, false, MUTED).apply { setPadding(0, dp(8), 0, 0) })

        val uc = card(col, tr("Cập nhật ứng dụng", "App update"))
        uc.addView(tv(tr("Phiên bản hiện tại: ", "Current version: ") + versionInfo(), 15f, true))
        uc.addView(btn(tr("Kiểm tra cập nhật", "Check for updates"), true) { checkUpdate() }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(10), 0, 0) })

        val pc = card(col, tr("Tiện ích cao cấp", "Premium widgets"))
        pc.addView(tv(tr("Bản hiện tại miễn phí. Các tiện ích cao cấp sẽ có ở phiên bản sau, bạn có thể xem trước giao diện thanh toán bên dưới.", "This version is free. Premium widgets arrive later; preview the payment window below."), 14f))
        pc.addView(btn(tr("Xem cửa sổ thanh toán (mẫu)", "Preview payment window"), false) { showPay() }.also { (it.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(10), 0, 0) })

        val hc = card(col, tr("Cách thêm tiện ích", "How to add a widget"))
        hc.addView(tv(tr("1. Mở tab Tiện ích, bấm \"Thêm tiện ích ra màn hình chính\"\n2. Hoặc nhấn giữ vào khoảng trống trên màn hình chính\n3. Chọn MagicOS Ultimate, kích thước 4×2, rồi kéo ra màn hình",
            "1. Open the Widgets tab and tap \"Add to home screen\"\n2. Or long-press an empty spot on the home screen\n3. Pick MagicOS Ultimate, size 4×2, and drag it out"), 15f))
        val ac = card(col, tr("Tác giả", "Author"))
        ac.addView(tv("Designer by MagicOS Ultimate", 18f, true))
        ac.addView(tv("MagicOS Ultimate · v" + versionInfo(), 13f, false, MUTED))
        val vc = card(col, tr("Ghi chú", "Notes"))
        vc.addView(tv(tr("Chạm vào tiện ích để làm mới pin, nhiệt độ, RAM, ổ cứng và thời tiết; dữ liệu cũng tự cập nhật khoảng 15 phút. Nên cho app chạy nền không bị hạn chế pin.",
            "Tap a widget to refresh battery, temperature, RAM, storage and weather; data also refreshes about every 15 minutes. Allow background activity for best results."), 13f, false, MUTED))
        return scroll(col)
    }

    private fun checkUpdate() {
        if (UPDATE_URL.isBlank()) { toast(tr("Chưa cấu hình link cập nhật (sẽ bật khi phát hành).", "Update link not configured yet.")); return }
        thread {
            try {
                val j = JSONObject(Render.httpGet(UPDATE_URL))
                @Suppress("DEPRECATION") val cur = packageManager.getPackageInfo(packageName, 0).versionCode
                val vc = j.getInt("versionCode")
                ui.post {
                    if (vc > cur) AlertDialog.Builder(this).setTitle(tr("Có bản mới ", "New version ") + j.optString("versionName"))
                        .setMessage(j.optString("notes")).setPositiveButton(tr("Tải về", "Download")) { _, _ -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(j.getString("url")))) }
                        .setNegativeButton(tr("Để sau", "Later"), null).show()
                    else toast(tr("Bạn đang dùng bản mới nhất", "You're up to date"))
                }
            } catch (e: Exception) { ui.post { toast(tr("Không kiểm tra được, thử lại sau", "Could not check, try again later")) } }
        }
    }

    /** Cửa sổ thanh toán mẫu: MoMo / ZaloPay / Ngân hàng / PayPal (ảnh QR đặt vào res/drawable) */
    private fun showPay() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(8), dp(18), dp(4)) }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val names = arrayOf("MoMo", "ZaloPay", tr("Ngân hàng", "Bank"), "PayPal")
        val qr = arrayOf("qr_momo", "qr_zalopay", "qr_bank", "")
        val tv2 = ArrayList<TextView>()
        fun sel(i: Int) {
            tv2.forEachIndexed { k, v -> v.setTextColor(if (k == i) ACC else MUTED); v.background = if (k == i) rounded(SEL, 14) else null }
            body.removeAllViews()
            val id = if (qr[i].isEmpty()) 0 else resources.getIdentifier(qr[i], "drawable", packageName)
            if (i == 3) body.addView(tv(tr("Thanh toán quốc tế qua PayPal sẽ có khi mở bán.", "International payment via PayPal will be available at launch."), 14f).apply { setPadding(0, dp(24), 0, dp(24)) })
            else if (id != 0) body.addView(ImageView(this).apply { setImageResource(id) }, LinearLayout.LayoutParams(dp(220), dp(220)))
            else body.addView(ctv("QR\n" + names[i], 18f, true, MUTED).apply { background = rounded(TILE, 16, LINE); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(220), dp(220)))
            body.addView(tv(tr("Số tiền: (sẽ công bố)", "Amount: (to be announced)"), 15f, true).apply { setPadding(0, dp(10), 0, 0) })
            body.addView(tv(tr("Nội dung chuyển khoản: MGO-XXXX", "Transfer note: MGO-XXXX"), 13f, false, MUTED))
        }
        names.forEachIndexed { k, s ->
            val v = tv(s, 13f, true).apply { gravity = Gravity.CENTER; setPadding(dp(4), dp(8), dp(4), dp(8)) }
            v.setOnClickListener { sel(k) }
            tv2.add(v); tabs.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(tabs); root.addView(body)
        sel(0)
        AlertDialog.Builder(this).setTitle(tr("Mở khoá tiện ích", "Unlock widget")).setView(ScrollView(this).apply { addView(root) })
            .setPositiveButton(tr("Tôi đã chuyển khoản", "I have paid")) { _, _ -> toast(tr("Gửi ảnh biên lai cho tác giả để nhận mã/tệp (tính năng sẽ có ở bản bán).", "Send the receipt to the author to get your file (available at launch).")) }
            .setNegativeButton(tr("Đóng", "Close"), null).show()
    }

    // =============== TAB TÙY CHỈNH ===============
    private fun renderPreview() {
        val f = frame ?: return
        previewInto(f, S.i(this, "pv", 0), 150f)
    }

    private fun customizePage(): View {
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pc = card(page, tr("Xem trước trực tiếp", "Live preview"))
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val chipViews = ArrayList<TextView>()
        fun paintChips() { chipViews.forEachIndexed { k, v -> val on = S.i(this, "pv", 0) == k
            v.setTextColor(if (on) ACC else INK); v.background = rounded(if (on) SEL else CARD, 18, if (on) ACC else LINE) } }
        arrayOf(tr("Sáng", "Light"), tr("Tối", "Dark"), tr("Kính lỏng", "Liquid glass")).forEachIndexed { k, s ->
            val v = tv(s, 13f, true).apply { gravity = Gravity.CENTER; setPadding(dp(6), dp(8), dp(6), dp(8)) }
            v.setOnClickListener { S.p(this).edit().putInt("pv", k).apply(); paintChips(); renderPreview() }
            chipViews.add(v); chips.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        pc.addView(chips); paintChips()
        val fr = FrameLayout(this)
        pc.addView(fr, LinearLayout.LayoutParams(-1, dp(150)).apply { topMargin = dp(10) })
        frame = fr
        fr.post { renderPreview() }
        pc.addView(tv(tr("Mọi thay đổi áp dụng ngay lên tiện ích ngoài màn hình chính. Chạm vào tiện ích để làm mới dữ liệu.", "Changes apply to your home-screen widgets instantly. Tap a widget to refresh its data."), 11f, false, MUTED).apply { setPadding(0, dp(6), 0, 0) })

        val col = colLayout()
        val kc = card(col, tr("Kiểu hiển thị tiện ích", "Widget style"))
        val modes = arrayOf(tr("Kiểu mới · Thông số hệ thống", "New · System stats") to tr("Pin, nhiệt độ máy, RAM, ổ cứng cập nhật realtime", "Battery, temperature, RAM, storage"),
            tr("Kiểu cổ điển · Thời tiết", "Classic · Weather") to tr("Thời tiết kèm vị trí và nhiệt độ", "Weather with place and temperature"),
            tr("Kiểu đơn giản · Thời tiết", "Simple · Weather") to tr("Tên, nhiệt độ, vị trí và dự báo gọn", "Name, temperature, place and short forecast"))
        val mviews = ArrayList<LinearLayout>()
        val note = tv("", 12f, false, MUTED)
        lateinit var statsCard: LinearLayout
        fun paintModes() {
            val cur = S.i(this, "mode", 0)
            mviews.forEachIndexed { k, v -> v.background = rounded(if (cur == k) SEL else CARD, 16, if (cur == k) ACC else LINE) }
            note.text = tr("Đang chỉnh cho kiểu: ${arrayOf("Mới", "Cổ điển", "Đơn giản")[cur]}. Chỉ hiện các mục dùng được ở kiểu này.", "Editing style: ${arrayOf("New", "Classic", "Simple")[cur]}. Only options that apply are shown.")
            statsCard.visibility = if (cur == 0) View.VISIBLE else View.GONE
        }
        modes.forEachIndexed { k, m ->
            val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
            v.addView(tv(m.first, 15f, true)); v.addView(tv(m.second, 12f, false, MUTED))
            v.setOnClickListener { S.p(this).edit().putInt("mode", k).apply(); paintModes(); changed() }
            mviews.add(v); kc.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        kc.addView(note)

        val tc = card(col, tr("Tùy biến hiển thị", "Display options"))
        sw(tc, tr("Hiện số giây (chạy mỗi giây, tốn pin hơn)", "Show seconds (updates every second, uses more battery)"), "sec", false)
        tc.addView(tv(tr("Ngày và lịch", "Date and calendar"), 13f, true, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        sw(tc, tr("Ngày dương lịch", "Solar date"), "solar", true); sw(tc, tr("Âm lịch", "Lunar date"), "lunar", true)
        lateinit var swW7: Switch
        val swWeek = sw(tc, tr("Lịch tuần (7 ngày phía dưới)", "Weekly calendar (bottom row)"), "week", true) { on -> if (on) swW7.isChecked = false }
        swW7 = sw(tc, tr("Thời tiết 7 ngày (thay cho lịch tuần)", "7-day weather (replaces weekly calendar)"), "week7", false) { on -> if (on) swWeek.isChecked = false }

        statsCard = card(col, tr("Bảng số liệu máy", "Device stats"))
        sw(statsCard, tr("Dòng tên + biểu tượng", "Name + icon row"), "nameRow", true); sw(statsCard, tr("Pin", "Battery"), "bt", true); sw(statsCard, tr("Nhiệt độ máy", "Temperature"), "tp", true)
        sw(statsCard, "RAM", "rm", true); sw(statsCard, tr("Ổ cứng", "Storage"), "dk", true)
        paintModes()

        val sc = card(col, tr("Kích thước", "Size"))
        sl(sc, tr("Cỡ đồng hồ", "Clock size"), "cs", 50, 150, 100, "%")
        sl(sc, tr("Bo góc", "Corner radius"), "cr", 0, 40, 24, " dp")
        sl(sc, tr("Độ rộng viền (Kính lỏng)", "Border width (Liquid glass)"), "bw", 0, 4, 1, " dp")

        val bc = card(col, tr("Nền tiện ích", "Widget background"))
        colorRow(bc, tr("Nền bản Sáng", "Light background"), "bgL"); colorRow(bc, tr("Nền bản Tối", "Dark background"), "bgD")
        bc.addView(tv(tr("Bản Mặc định tự đổi giữa nền sáng và tối theo Dark mode của hệ thống.", "The Default widget switches between light and dark with the system."), 12f, false, MUTED))

        val gc = card(col, tr("Kính lỏng", "Liquid glass"))
        sl(gc, tr("Độ đậm mặt kính", "Glass tint"), "ga", 0, 100, 30, "%")
        sl(gc, tr("Độ mờ mặt kính", "Glass frost"), "gf", 0, 100, 40, "%")
        sl(gc, tr("Độ nổi khối mặt kính", "Glass depth"), "gd", 0, 100, 50, "%")
        colorRow(gc, tr("Màu phủ kính", "Glass tint color"), "glass")
        gc.addView(tv(tr("Tiện ích không làm mờ được hình nền phía sau, nên độ mờ là lớp kính nhám mô phỏng.", "Widgets cannot blur the wallpaper behind them, so frost is a simulated frosted layer."), 12f, false, MUTED).apply { setPadding(0, dp(6), 0, 0) })

        val xc = card(col, tr("Màu chữ", "Text colors"))
        colorRow(xc, tr("Chữ · Sáng", "Text · Light"), "txL"); colorRow(xc, tr("Chữ · Tối", "Text · Dark"), "txD"); colorRow(xc, tr("Chữ · Kính lỏng", "Text · Liquid glass"), "txG")

        val mc = card(col, tr("Màu các thông số", "Stat colors"))
        colorRow(mc, tr("Pin", "Battery"), "cBt"); colorRow(mc, tr("Nhiệt độ máy", "Temperature"), "cTp"); colorRow(mc, "RAM", "cRm")
        colorRow(mc, tr("Ổ cứng", "Storage"), "cDk"); colorRow(mc, tr("Cảnh báo (pin yếu, máy nóng…)", "Warning (low battery, hot…)"), "cWarn")

        val lc = card(col, tr("Lịch", "Calendar")); colorRow(lc, tr("Màu nổi ngày hôm nay", "Today highlight"), "cToday")

        val nc = card(col, tr("Dòng tên & biểu tượng", "Name & icon"))
        val et = EditText(this).apply { hint = "MagicOS Ultimate"; setText(S.s(this@MainActivity, "name", "")); setSingleLine()
            setTextColor(INK); setHintTextColor(MUTED) }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { S.p(this@MainActivity).edit().putString("name", s.toString()).apply(); changed() }
        })
        nc.addView(et)
        nc.addView(tv(tr("Ví dụ: MagicOS Ultimate. Để trống sẽ hiện tên máy.", "Example: MagicOS Ultimate. Leave empty to show the device name."), 12f, false, MUTED))
        nc.addView(tv(tr("Biểu tượng", "Icon"), 14f, true).apply { setPadding(0, dp(10), 0, dp(6)) })
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val irow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val iv = ArrayList<TextView>()
        fun paintIco() { iv.forEachIndexed { k, v -> val on = S.i(this, "ico", 1) == k; v.background = rounded(if (on) SEL else CARD, 14, if (on) ACC else LINE) } }
        Render.ICONS.forEachIndexed { k, s ->
            val v = tv(if (k == 0) tr("Ẩn", "Hide") else s, 18f, true).apply { gravity = Gravity.CENTER; setPadding(dp(16), dp(10), dp(16), dp(10)) }
            v.setOnClickListener { S.p(this).edit().putInt("ico", k).apply(); paintIco(); changed() }
            iv.add(v); irow.addView(v, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        hs.addView(irow); nc.addView(hs); paintIco()
        colorRow(nc, tr("Màu biểu tượng", "Icon color"), "cIco")
        sw(nc, tr("Biểu tượng nhấp nháy", "Blinking icon"), "blink", false)

        val rc = card(col, tr("Số liệu realtime", "Live data"))
        val diag = tv("", 13f, false, MUTED)
        fun fillDiag() {
            val st = Render.stats(this); val lu = S.p(this).getLong("lu", 0)
            val ago = if (lu == 0L) tr("chưa có", "none") else "${(System.currentTimeMillis() - lu) / 1000} " + tr("giây trước", "s ago")
            diag.text = tr("Cập nhật widget gần nhất: $ago\nPin ${st.bl}% (${if (st.chg) "đang sạc" else "không sạc"}) · ${st.mah}mAh · Nhiệt ${st.bt}°C\nRAM ${st.ramPct}% · Ổ cứng ${st.dkPct}%",
                "Last widget update: $ago\nBattery ${st.bl}% (${if (st.chg) "charging" else "not charging"}) · ${st.mah}mAh · ${st.bt}°C\nRAM ${st.ramPct}% · Storage ${st.dkPct}%")
        }
        fillDiag(); rc.addView(diag)
        rc.addView(btn(tr("Làm mới số liệu ngay", "Refresh data now"), false) {
            thread { Render.updateAll(applicationContext, true); ui.post { fillDiag(); renderPreview(); toast(tr("Đã làm mới", "Refreshed")) } }
        })

        col.addView(btn(tr("Khôi phục mặc định", "Restore defaults"), false) {
            AlertDialog.Builder(this).setTitle(tr("Khôi phục mặc định?", "Restore defaults?")).setMessage(tr("Đưa toàn bộ màu, kích thước, công tắc về mặc định. Vị trí thời tiết, ghi chú và ngôn ngữ được giữ nguyên.", "Resets colors, sizes and switches. Weather place, notes and language are kept."))
                .setPositiveButton(tr("Khôi phục", "Restore")) { _, _ ->
                    val keep = setOf("place", "placeCustom", "lat", "lon", "wjson", "wt", "t", "code", "day", "hi", "lo", "pop", "sr", "ss", "gps", "asked", "lu", "notes", "lang", "mah")
                    val e = S.p(this).edit(); S.p(this).all.keys.filter { it !in keep }.forEach { e.remove(it) }; e.apply()
                    show(2); changed()
                }.setNegativeButton(tr("Hủy", "Cancel"), null).show()
        })

        page.addView(scroll(col), LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun sw(parent: LinearLayout, label: String, key: String, def: Boolean, extra: ((Boolean) -> Unit)? = null): Switch {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        r.addView(tv(label, 15f), LinearLayout.LayoutParams(0, -2, 1f))
        val s = Switch(this).apply { isChecked = S.b(this@MainActivity, key, def) }
        s.setOnCheckedChangeListener { _, on ->
            S.p(this@MainActivity).edit().putBoolean(key, on).apply()
            if (extra != null) extra(on)
            changed()
        }
        r.addView(s)
        parent.addView(r)
        return s
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
        0xFF000000.toInt(), 0xFFF4F6FB.toInt(), 0xFF9CA3AF.toInt(), 0xFF6B7280.toInt(), 0xFF0EA5E9.toInt(), 0xFF84CC16.toInt())

    private fun hexOf(c: Int): String = if (Color.alpha(c) == 255) String.format("#%06X", c and 0xFFFFFF) else String.format("#%08X", c)

    private fun colorRow(parent: LinearLayout, label: String, key: String) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        val swatch = View(this).apply { background = rounded(Render.col(this@MainActivity, key), 10, LINE) }
        r.addView(swatch, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(14) })
        val lt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lt.addView(tv(label, 15f, true))
        val sub = tv(if (S.p(this).contains(key)) tr("Tùy chỉnh", "Custom") else tr("Mặc định", "Default"), 12f, false, MUTED)
        lt.addView(sub); r.addView(lt)
        r.setOnClickListener {
            pickColor(label, key) {
                swatch.background = rounded(Render.col(this, key), 10, LINE)
                sub.text = if (S.p(this).contains(key)) tr("Tùy chỉnh", "Custom") else tr("Mặc định", "Default"); changed()
            }
        }
        parent.addView(r)
    }

    /** Hộp chọn màu: mã HEX, sắc độ, độ đậm, độ sáng, (độ đặc) và bảng màu */
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
        slider(tr("Sắc độ", "Hue"), sbH) { h = it.toFloat() }
        slider(tr("Độ đậm", "Saturation"), sbS) { s = it / 100f }
        slider(tr("Độ sáng", "Brightness"), sbV) { v = it / 100f }
        if (withAlpha) slider(tr("Độ đặc", "Opacity"), sbA) { a = it * 255 / 100 }
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
            .setPositiveButton(tr("Chọn", "OK")) { _, _ -> S.p(this).edit().putInt(key, cur()).apply(); done() }
            .setNeutralButton(tr("Mặc định", "Default")) { _, _ -> S.p(this).edit().remove(key).apply(); done() }
            .setNegativeButton(tr("Hủy", "Cancel"), null).show()
    }
}
