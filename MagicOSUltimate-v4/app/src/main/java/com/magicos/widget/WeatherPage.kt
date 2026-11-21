package com.magicos.widget

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class WeatherPage(private val a: UiBase) {
    private lateinit var box: LinearLayout
    private val PERM = 77

    fun build(): View {
        val col = a.colLayout()
        box = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        col.addView(box)
        col.addView(a.btn(a.tr("Dùng vị trí của máy", "Use device location"), true) { useGps(false) })
        col.addView(a.btn(a.tr("Chọn thành phố khác", "Choose another place"), false) { pickCity() })
        col.addView(a.btn(a.tr("Cập nhật thời tiết", "Refresh weather"), false) { reload(true) })
        col.addView(a.ctv(a.tr("Vị trí lấy từ định vị của máy khi bạn mở app (hoặc nơi bạn chọn). Chạm vào tên địa điểm để sửa tên hiển thị. Dữ liệu: Open-Meteo, OpenStreetMap.",
            "Location comes from your device when you open the app (or the place you pick). Tap the place name to edit it. Data: Open-Meteo, OpenStreetMap."), 12f, false, a.MUTED)
            .apply { setPadding(a.dp(20), a.dp(8), a.dp(20), a.dp(8)) })
        draw()
        val p = S.p(a)
        if (!p.getBoolean("asked", false)) { p.edit().putBoolean("asked", true).apply(); useGps(true) }
        else if (p.getBoolean("gps", false) && hasPerm()) useGps(true)
        else reload(false)
        return a.scroll(col)
    }

    fun onPermission(granted: Boolean) { if (granted) useGps(false) else a.toast(a.tr("Chưa cấp quyền vị trí. Bạn vẫn có thể chọn nơi thủ công.", "Location permission denied. You can pick a place manually.")) }

    private fun hasPerm() = a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun reload(force: Boolean) {
        thread {
            val ok = Render.refreshWeather(a.applicationContext, force)
            Render.updateAll(a.applicationContext, false)
            a.ui.post { draw(); if (!ok) a.toast(a.tr("Không tải được thời tiết, kiểm tra mạng", "Could not load weather. Check your connection.")) }
        }
    }

    // ---------- vị trí máy ----------
    private fun useGps(silent: Boolean) {
        if (!hasPerm()) {
            a.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERM)
            return
        }
        if (!silent) a.toast(a.tr("Đang lấy vị trí…", "Getting location…"))
        locate({ lat, lon ->
            thread {
                val name = placeName(lat, lon)
                S.p(a).edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).putString("place", name)
                    .putBoolean("gps", true).putLong("wt", 0).apply()
                val ok = Render.refreshWeather(a.applicationContext, true)
                Render.updateAll(a.applicationContext, false)
                a.ui.post { draw(); if (!ok) a.toast(a.tr("Không tải được thời tiết, kiểm tra mạng", "Could not load weather. Check your connection.")) }
            }
        }, { if (!silent) a.toast(a.tr("Không lấy được vị trí. Hãy bật định vị (GPS) rồi thử lại.", "Could not get location. Turn on GPS and try again.")) })
    }

    private fun locate(ok: (Double, Double) -> Unit, fail: () -> Unit) {
        val lm = a.getSystemService(LocationManager::class.java)
        var best: Location? = null
        try {
            for (p in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(p)
                val bb = best
                if (l != null && (bb == null || l.time > bb.time)) best = l
            }
        } catch (e: SecurityException) { }
        val b = best
        if (b != null && System.currentTimeMillis() - b.time < 3 * 3600 * 1000) { ok(b.latitude, b.longitude); return }
        val prov = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (prov == null) { if (b != null) ok(b.latitude, b.longitude) else fail(); return }
        var done = false
        val ls = object : LocationListener {
            override fun onLocationChanged(l: Location) { if (!done) { done = true; ok(l.latitude, l.longitude) } }
            @Deprecated("old api") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        try {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(prov, ls, Looper.getMainLooper())
        } catch (e: SecurityException) { fail(); return }
        a.ui.postDelayed({
            if (!done) {
                done = true
                try { lm.removeUpdates(ls) } catch (e: SecurityException) { }
                if (b != null) ok(b.latitude, b.longitude) else fail()
            }
        }, 15000)
    }

    /** Tên địa điểm theo xã/phường: OpenStreetMap trước, rồi Geocoder của máy */
    private fun placeName(lat: Double, lon: Double): String {
        try {
            val j = JSONObject(Render.httpGet("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=14&accept-language=vi"))
            val ad = j.optJSONObject("address")
            if (ad != null) {
                val local = listOf("village", "suburb", "town", "quarter", "city_district", "hamlet", "neighbourhood").map { ad.optString(it) }.firstOrNull { it.isNotBlank() } ?: ""
                val prov = listOf("city", "state", "county").map { ad.optString(it) }.firstOrNull { it.isNotBlank() } ?: ""
                val parts = listOf(local, prov).filter { it.isNotBlank() }.distinct()
                if (parts.isNotEmpty()) return parts.joinToString(", ")
            }
        } catch (e: Exception) { }
        try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                val l = Geocoder(a, Locale("vi", "VN")).getFromLocation(lat, lon, 1)
                if (l != null && l.isNotEmpty()) {
                    val x = l[0]
                    val parts = listOfNotNull(x.subLocality ?: x.locality, x.adminArea).filter { it.isNotBlank() }.distinct()
                    if (parts.isNotEmpty()) return parts.joinToString(", ")
                }
            }
        } catch (e: Exception) { }
        return String.format(Locale.US, "%.3f, %.3f", lat, lon)
    }

    // ---------- chọn nơi khác / sửa tên ----------
    private fun pickCity() {
        val root = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(18), a.dp(8), a.dp(18), 0) }
        val et = EditText(a).apply { hint = a.tr("Ví dụ: Xã Tuệ Tĩnh, Hải Phòng", "e.g. Da Nang"); setSingleLine(); imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT; setTextColor(a.INK); setHintTextColor(a.MUTED) }
        val lv = ListView(a)
        val names = ArrayList<String>(); val pts = ArrayList<Triple<String, Double, Double>>()
        val ad = ArrayAdapter(a, android.R.layout.simple_list_item_1, names)
        lv.adapter = ad
        val st = a.tv("", 13f, false, a.MUTED)
        fun search() {
            val q = et.text.toString().trim(); if (q.isEmpty()) return
            st.text = a.tr("Đang tìm…", "Searching…")
            thread {
                val res = ArrayList<Triple<String, Double, Double>>()
                try {
                    val arr = JSONArray(Render.httpGet("https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(q, "UTF-8") + "&format=jsonv2&limit=8&accept-language=vi"))
                    for (k in 0 until arr.length()) {
                        val o = arr.getJSONObject(k)
                        val nm = o.optString("display_name").split(",").map { it.trim() }.take(3).joinToString(", ")
                        res.add(Triple(nm, o.getString("lat").toDouble(), o.getString("lon").toDouble()))
                    }
                } catch (e: Exception) { }
                a.ui.post {
                    names.clear(); pts.clear(); res.forEach { names.add(it.first); pts.add(it) }; ad.notifyDataSetChanged()
                    st.text = if (res.isEmpty()) a.tr("Không tìm thấy kết quả", "No results") else a.tr("Chọn một kết quả:", "Pick a result:")
                }
            }
        }
        et.setOnEditorActionListener { _, _, _ -> search(); true }
        root.addView(et)
        root.addView(a.btn(a.tr("Tìm", "Search"), true) { search() })
        root.addView(st)
        root.addView(lv, LinearLayout.LayoutParams(-1, a.dp(240)))
        val dlg = AlertDialog.Builder(a).setTitle(a.tr("Chọn nơi xem thời tiết", "Choose a place")).setView(root).setNegativeButton(a.tr("Đóng", "Close"), null).create()
        lv.setOnItemClickListener { _, _, pos, _ ->
            val t = pts[pos]
            S.p(a).edit().putFloat("lat", t.second.toFloat()).putFloat("lon", t.third.toFloat()).putString("place", t.first)
                .putString("placeCustom", "").putBoolean("gps", false).putLong("wt", 0).apply()
            dlg.dismiss(); draw(); reload(true)
        }
        dlg.show()
    }

    private fun editPlace() {
        val et = EditText(a).apply { setText(Render.place(a)); setSingleLine(); setTextColor(a.INK); setPadding(a.dp(24), a.dp(16), a.dp(24), a.dp(8)) }
        AlertDialog.Builder(a).setTitle(a.tr("Sửa tên địa điểm hiển thị", "Edit displayed place name")).setView(et)
            .setPositiveButton(a.tr("Lưu", "Save")) { _, _ -> S.p(a).edit().putString("placeCustom", et.text.toString().trim()).apply(); draw(); reload(false) }
            .setNeutralButton(a.tr("Tự động", "Auto")) { _, _ -> S.p(a).edit().putString("placeCustom", "").apply(); draw(); reload(false) }
            .setNegativeButton(a.tr("Hủy", "Cancel"), null).show()
    }

    // ---------- hiển thị ----------
    private fun draw() {
        box.removeAllViews()
        val json = S.p(a).getString("wjson", null)
        if (json == null) {
            val c = a.card(box, a.tr("Thời tiết", "Weather"))
            c.addView(a.tv("📍 ${Render.place(a)}", 16f, true)); c.addView(a.tv(a.tr("Đang tải dữ liệu thời tiết…", "Loading weather…"), 14f, false, a.MUTED))
            return
        }
        try { drawData(JSONObject(json)) } catch (e: Exception) {
            a.card(box, a.tr("Thời tiết", "Weather")).addView(a.tv(a.tr("Dữ liệu lỗi, hãy bấm Cập nhật thời tiết.", "Data error, tap Refresh weather."), 14f, false, a.MUTED))
        }
    }

    private fun arr(o: JSONObject, k: String): JSONArray = o.optJSONArray(k) ?: JSONArray()

    private val BFT_T = arrayOf("lặng gió, khói bay thẳng lên", "gió rất nhẹ, khói hơi nghiêng", "gió nhẹ, cảm nhận được trên mặt",
        "gió nhẹ, lá cây xào xạc, cờ bay phấp phới", "gió vừa, bụi và giấy bay", "gió khá mạnh, cây nhỏ lay động", "gió mạnh, cành lớn lay động",
        "gió rất mạnh, đi ngược gió khó khăn", "gió giật, cành cây gãy", "gió rất giật, có thể hư hại nhà cửa", "bão, cây có thể bật gốc",
        "bão lớn, thiệt hại nặng", "bão dữ dội")

    private fun bft(kmh: Double): Int {
        val l = doubleArrayOf(1.0, 6.0, 12.0, 20.0, 29.0, 39.0, 50.0, 62.0, 75.0, 89.0, 103.0, 118.0)
        for (i in l.indices) if (kmh < l[i]) return i
        return 12
    }

    private fun mins(s: String): Int = s.substring(0, 2).toInt() * 60 + s.substring(3, 5).toInt()

    private fun moonPhase(): Float {
        val n = Calendar.getInstance()
        val jdn = Lunar.jd(n.get(Calendar.DAY_OF_MONTH), n.get(Calendar.MONTH) + 1, n.get(Calendar.YEAR))
        var f = ((jdn - 2451550.26) / 29.530588853) % 1.0
        if (f < 0) f += 1.0
        return f.toFloat()
    }

    private fun moonName(f: Float): String = when {
        f < 0.03f || f > 0.97f -> "Trăng mới"
        f < 0.22f -> "Trăng lưỡi liềm đầu tháng"
        f < 0.28f -> "Thượng huyền (bán nguyệt)"
        f < 0.47f -> "Trăng khuyết đầu tháng"
        f < 0.53f -> "Trăng tròn"
        f < 0.72f -> "Trăng khuyết cuối tháng"
        f < 0.78f -> "Hạ huyền (bán nguyệt)"
        else -> "Trăng lưỡi liềm cuối tháng"
    }

    private fun vcard(icon: String, title: String): LinearLayout = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL; setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(14))
        background = a.rounded(a.CARD, 22, a.LINE)
        addView(a.tv("$icon  $title", 14f, false, a.MUTED))
    }
    private fun bigv(s: String, sp: Float = 34f): TextView = a.tv(s, sp, true).apply { setPadding(0, a.dp(6), 0, a.dp(2)) }
    private fun desc(s: String): TextView = a.tv(s, 13f, false, a.INK).apply { setPadding(0, a.dp(8), 0, 0) }
    private fun viz(type: Int, hDp: Int, cfg: (WxViz) -> Unit): WxViz {
        val v = WxViz(a, type); v.ink = a.INK; v.sub = a.MUTED; v.track = a.LINE; cfg(v)
        v.layoutParams = LinearLayout.LayoutParams(-1, a.dp(hDp)); return v
    }
    private fun pair(l: View, r: View): LinearLayout = LinearLayout(a).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(l, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(a.dp(12), a.dp(5), a.dp(5), a.dp(5)) })
        addView(r, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(a.dp(5), a.dp(5), a.dp(12), a.dp(5)) })
    }

    private fun drawData(j: JSONObject) {
        val cur = j.getJSONObject("current"); val hr = j.getJSONObject("hourly"); val dy = j.getJSONObject("daily")
        val code = cur.getInt("weather_code"); val isDay = cur.optInt("is_day", 1) == 1
        val t = cur.getDouble("temperature_2m"); val feel = cur.getDouble("apparent_temperature")
        val hi = arr(dy, "temperature_2m_max").getDouble(0).roundToInt(); val lo = arr(dy, "temperature_2m_min").getDouble(0).roundToInt()
        val upd = SimpleDateFormat("HH:mm", Locale.US).format(Date(S.p(a).getLong("wt", System.currentTimeMillis())))
        val times = arr(hr, "time")
        val key = cur.getString("time").substring(0, 13)
        var st = 0
        for (k in 0 until times.length()) if (times.getString(k).startsWith(key)) { st = k; break }

        // --- thẻ chính ---
        val hero = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(20), a.dp(18), a.dp(20), a.dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, if (a.isDark) intArrayOf(0xFF1F3A9E.toInt(), 0xFF2E2D7E.toInt()) else intArrayOf(0xFF4568F0.toInt(), 0xFF5B4BD6.toInt())).apply { cornerRadius = a.dp(26).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(12), a.dp(6), a.dp(12), a.dp(6)) } }
        val top = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(a.tv("📍 ${Render.place(a)}  ✎", 17f, true, Color.WHITE).apply { maxLines = 2; setOnClickListener { editPlace() } }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(a.tv("⟳ $upd", 13f, true, Color.WHITE).apply { setPadding(a.dp(12), a.dp(6), a.dp(12), a.dp(6)); background = a.rounded(0x33FFFFFF, 18) })
        hero.addView(top)
        val mid = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        mid.addView(a.tv("${t.roundToInt()}°", 84f, false, Color.WHITE), LinearLayout.LayoutParams(0, -2, 1f))
        mid.addView(WxIconView(a, code, isDay, 60), LinearLayout.LayoutParams(a.dp(120), a.dp(120)))
        hero.addView(mid)
        hero.addView(a.tv(Render.wxText(a, code), 24f, true, Color.WHITE))
        hero.addView(a.tv(a.tr("Cảm giác như ${feel.roundToInt()}°", "Feels like ${feel.roundToInt()}°"), 15f, false, 0xE6FFFFFF.toInt()).apply { setPadding(0, a.dp(6), 0, a.dp(2)) })
        val sp = SpannableStringBuilder()
        sp.append("▼"); sp.setSpan(ForegroundColorSpan(0xFF9DD6FF.toInt()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.append(" ${a.tr("Thấp", "Low")}: $lo°      ")
        val s2 = sp.length
        sp.append("▲"); sp.setSpan(ForegroundColorSpan(0xFFFF5A5A.toInt()), s2, s2 + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.append(" ${a.tr("Cao", "High")}: $hi°")
        hero.addView(a.tv("", 15f, false, Color.WHITE).apply { text = sp })
        box.addView(hero)

        // --- các thẻ thông tin ---
        box.addView(a.tv(a.tr("CHI TIẾT", "DETAILS"), 12f, true, a.MUTED).apply { letterSpacing = 0.08f; setPadding(a.dp(20), a.dp(12), a.dp(20), a.dp(2)) })
        val uv = arr(hr, "uv_index").optDouble(st, 0.0)
        val visKm = arr(hr, "visibility").optDouble(st, 10000.0) / 1000.0
        val press = cur.optDouble("pressure_msl", 1013.0)
        val cloud = cur.optInt("cloud_cover", 0)
        val hum = cur.getDouble("relative_humidity_2m").roundToInt()
        val kmh = cur.getDouble("wind_speed_10m"); val wdeg = cur.getDouble("wind_direction_10m")
        val pop = arr(dy, "precipitation_probability_max").optInt(0, 0)
        val sr = arr(dy, "sunrise").getString(0).substring(11, 16); val ss = arr(dy, "sunset").getString(0).substring(11, 16)
        val nowM = mins(cur.getString("time").substring(11, 16))
        val prog = (nowM - mins(sr)).toFloat() / (mins(ss) - mins(sr)).toFloat()
        val moon = moonPhase()
        val b = bft(kmh)

        val c1 = vcard("☀️", a.tr("Chỉ số UV", "UV index")).apply {
            val lvl = if (uv < 3) a.tr("Thấp", "Low") else if (uv < 6) a.tr("Trung bình", "Moderate") else if (uv < 8) a.tr("Cao", "High") else if (uv < 11) a.tr("Rất cao", "Very high") else a.tr("Cực cao", "Extreme")
            addView(bigv(lvl, 26f)); addView(a.tv("${uv.roundToInt()}", 18f, true))
            addView(viz(WxViz.UV, 28) { it.v1 = uv.toFloat() })
            addView(desc(if (uv < 3) "Gần như không có nguy cơ cháy nắng" else if (uv < 6) "Có thể bị cháy nắng nếu ở ngoài lâu" else if (uv < 8) "Nên bôi kem chống nắng, đội mũ" else if (uv < 11) "Hạn chế ra ngoài giữa trưa" else "Tránh ra ngoài nắng"))
        }
        val diff = feel - t
        val c2 = vcard("🌡️", a.tr("Cảm giác như", "Feels like")).apply {
            addView(bigv("${feel.roundToInt()}°", 40f)); addView(a.tv(a.tr("Nhiệt độ thực: ${t.roundToInt()}°", "Actual: ${t.roundToInt()}°"), 14f, true))
            addView(desc(if (diff <= -4) "Cảm giác lạnh hơn nhiều so với nhiệt độ thực" else if (diff <= -1.5) "Cảm giác lạnh hơn nhiệt độ thực" else if (diff < 1.5) "Gần bằng nhiệt độ thực" else if (diff < 4) "Cảm giác nóng hơn nhiệt độ thực" else "Cảm giác nóng hơn nhiều so với nhiệt độ thực"))
        }
        box.addView(pair(c1, c2))
        val c3 = vcard("💨", a.tr("Sức gió", "Wind")).apply {
            addView(viz(WxViz.COMPASS, 140) { it.v1 = wdeg.toFloat(); it.txt1 = "$b"; it.txt2 = "Bft" })
            addView(desc("Gió ${Render.windDir(wdeg)}, ${BFT_T[b]}"))
        }
        val c4 = vcard("🌅", a.tr("Mặt trời mọc", "Sunrise")).apply {
            addView(bigv(sr, 36f))
            addView(viz(WxViz.SUNARC, 80) { it.v1 = prog })
            addView(a.tv(a.tr("Mặt trời lặn: $ss", "Sunset: $ss"), 15f, true))
        }
        box.addView(pair(c3, c4))
        val c5 = vcard("💧", a.tr("Độ ẩm", "Humidity")).apply {
            addView(bigv("$hum%", 36f))
            addView(desc(if (hum < 30) "Không khí khô" else if (hum < 60) "Độ ẩm dễ chịu" else if (hum < 80) "Hơi ẩm" else "Khá ẩm, dễ đọng sương"))
        }
        val c6 = vcard("👁", a.tr("Tầm nhìn", "Visibility")).apply {
            addView(bigv("${visKm.roundToInt()} km", 32f))
            addView(viz(WxViz.VIS, 26) { it.v1 = (visKm / 20.0).toFloat() })
            addView(desc(if (visKm < 1) "Tầm nhìn rất kém (sương mù)" else if (visKm < 4) "Tầm nhìn kém" else if (visKm < 10) "Tầm nhìn khá tốt" else "Tầm nhìn rất tốt"))
        }
        box.addView(pair(c5, c6))
        val c7 = vcard("🧭", a.tr("Áp suất", "Pressure")).apply {
            addView(viz(WxViz.GAUGE, 140) { it.v1 = press.toFloat(); it.txt1 = "${press.roundToInt()}"; it.txt2 = "hPa" })
            addView(desc(if (press < 1000) "Áp suất thấp, có thể có mưa gió" else if (press < 1015) "Áp suất bình thường" else if (press < 1025) "Áp suất hơi cao, dễ chịu" else "Áp suất cao, trời thường quang"))
        }
        val c8 = vcard("🌙", a.tr("Pha mặt trăng", "Moon phase")).apply {
            addView(viz(WxViz.MOON, 120) { it.v1 = moon })
            addView(desc(moonName(moon)))
            addView(a.tv(a.tr("Độ sáng ${(((1 - Math.cos(2 * Math.PI * moon)) / 2) * 100).roundToInt()}%", "Illumination ${(((1 - Math.cos(2 * Math.PI * moon)) / 2) * 100).roundToInt()}%"), 13f, false, a.MUTED))
        }
        box.addView(pair(c7, c8))
        val c9 = vcard("🌧️", a.tr("Khả năng mưa", "Chance of rain")).apply {
            addView(bigv("$pop%", 36f))
            addView(desc(if (pop < 20) "Ít khả năng mưa" else if (pop < 50) "Có thể có mưa" else if (pop < 80) "Khả năng mưa cao" else "Nhiều khả năng có mưa"))
        }
        val c10 = vcard("☁️", a.tr("Lượng mây", "Cloud cover")).apply {
            addView(bigv("$cloud%", 36f))
            addView(desc(if (cloud < 20) "Trời quang" else if (cloud < 50) "Ít mây" else if (cloud < 80) "Nhiều mây" else "U ám"))
        }
        box.addView(pair(c9, c10))

        // --- theo giờ ---
        val hc = a.card(box, a.tr("Dự báo theo giờ", "Hourly forecast"))
        val ht = arr(hr, "temperature_2m"); val hp = arr(hr, "precipitation_probability"); val hw = arr(hr, "weather_code")
        val hs = HorizontalScrollView(a).apply { isHorizontalScrollBarEnabled = false }
        val hrow = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        for (k in st until minOf(st + 13, times.length())) {
            val hour = times.getString(k).substring(11, 13).toInt()
            val x = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(0, a.dp(12), 0, a.dp(12))
                background = if (k == st) a.rounded(a.SEL, 18) else null }
            x.addView(a.ctv(if (k == st) a.tr("Bây giờ", "Now") else times.getString(k).substring(11, 16), 14f, k == st, if (k == st) a.ACC else a.MUTED), LinearLayout.LayoutParams(-1, -2))
            x.addView(WxIconView(a, hw.getInt(k), hour in 6..17, 12), LinearLayout.LayoutParams(a.dp(40), a.dp(40)).apply { gravity = Gravity.CENTER_HORIZONTAL; setMargins(0, a.dp(6), 0, a.dp(6)) })
            x.addView(a.ctv("${ht.getDouble(k).roundToInt()}°", 18f, true), LinearLayout.LayoutParams(-1, -2))
            x.addView(a.ctv("${hp.optInt(k, 0)}%", 13f, false, a.ACC), LinearLayout.LayoutParams(-1, -2))
            hrow.addView(x, LinearLayout.LayoutParams(a.dp(66), -2).apply { setMargins(a.dp(2), 0, a.dp(2), 0) })
        }
        hs.addView(hrow); hc.addView(hs)

        // --- 7 ngày ---
        val dd = a.card(box, a.tr("Dự báo 7 ngày", "7-day forecast"))
        val dts = arr(dy, "time"); val dmx = arr(dy, "temperature_2m_max"); val dmn = arr(dy, "temperature_2m_min")
        val dwc = arr(dy, "weather_code"); val dpp = arr(dy, "precipitation_probability_max")
        var wmin = 100.0; var wmax = -100.0
        for (k in 0 until dts.length()) { wmin = minOf(wmin, dmn.getDouble(k)); wmax = maxOf(wmax, dmx.getDouble(k)) }
        for (k in 0 until dts.length()) {
            val ds = dts.getString(k); val y = ds.substring(0, 4).toInt(); val mo = ds.substring(5, 7).toInt(); val d = ds.substring(8, 10).toInt()
            val wdn = Render.WD[Calendar.getInstance().apply { clear(); set(y, mo - 1, d) }.get(Calendar.DAY_OF_WEEK) - 1]
            val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, a.dp(8), 0, a.dp(8)) }
            val c1 = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            c1.addView(a.tv(if (k == 0) a.tr("Hôm nay", "Today") else wdn, 16f, true)); c1.addView(a.tv("$d/$mo", 13f, false, a.MUTED))
            r.addView(c1, LinearLayout.LayoutParams(a.dp(92), -2))
            r.addView(WxIconView(a, dwc.getInt(k), true, 12), LinearLayout.LayoutParams(a.dp(40), a.dp(40)))
            r.addView(a.ctv("${dpp.optInt(k, 0)}%", 14f, true, a.ACC), LinearLayout.LayoutParams(a.dp(46), -2))
            val mn = dmn.getDouble(k); val mx = dmx.getDouble(k)
            r.addView(a.tv("${mn.roundToInt()}°", 15f, false, a.MUTED).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(a.dp(34), -2))
            val track = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; background = a.rounded(a.LINE, 4) }
            track.addView(View(a), LinearLayout.LayoutParams(0, a.dp(6), maxOf(0.0, mn - wmin).toFloat()))
            track.addView(View(a).apply { background = a.rounded(a.ACC, 4) }, LinearLayout.LayoutParams(0, a.dp(6), maxOf(0.4, mx - mn).toFloat()))
            track.addView(View(a), LinearLayout.LayoutParams(0, a.dp(6), maxOf(0.0, wmax - mx).toFloat()))
            r.addView(track, LinearLayout.LayoutParams(0, a.dp(6), 1f).apply { setMargins(a.dp(8), 0, a.dp(8), 0) })
            r.addView(a.tv("${mx.roundToInt()}°", 16f, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(a.dp(36), -2))
            dd.addView(r)
        }
    }
}
