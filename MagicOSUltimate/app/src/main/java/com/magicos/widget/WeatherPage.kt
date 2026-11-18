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
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
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
        col.addView(a.btn("Dùng vị trí của máy", true) { useGps(false) })
        col.addView(a.btn("Chọn thành phố khác", false) { pickCity() })
        col.addView(a.btn("Cập nhật thời tiết", false) { reload(true) })
        col.addView(a.tv("Vị trí lấy từ định vị của máy khi bạn mở app (hoặc thành phố bạn chọn). Dữ liệu dự báo: Open-Meteo.", 12f, false, a.MUTED)
            .apply { gravity = Gravity.CENTER; setPadding(a.dp(20), a.dp(8), a.dp(20), a.dp(8)) })
        draw()
        val p = S.p(a)
        if (!p.getBoolean("asked", false)) { p.edit().putBoolean("asked", true).apply(); useGps(true) }
        else if (p.getBoolean("gps", false) && hasPerm()) useGps(true)
        else reload(false)
        return a.scroll(col)
    }

    fun onPermission(granted: Boolean) { if (granted) useGps(false) else a.toast("Chưa cấp quyền vị trí. Bạn vẫn có thể chọn thành phố.") }

    private fun hasPerm() = a.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // ---------- tải dữ liệu ----------
    private fun reload(force: Boolean) {
        thread {
            val ok = Render.refreshWeather(a.applicationContext, force)
            Render.updateAll(a.applicationContext, false)
            a.ui.post { draw(); if (!ok) a.toast("Không tải được thời tiết, kiểm tra mạng") }
        }
    }

    // ---------- vị trí máy ----------
    private fun useGps(silent: Boolean) {
        if (!hasPerm()) {
            a.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERM)
            return
        }
        if (!silent) a.toast("Đang lấy vị trí…")
        locate({ lat, lon ->
            thread {
                val name = placeName(lat, lon)
                S.p(a).edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).putString("place", name)
                    .putBoolean("gps", true).putLong("wt", 0).apply()
                val ok = Render.refreshWeather(a.applicationContext, true)
                Render.updateAll(a.applicationContext, false)
                a.ui.post { draw(); if (!ok) a.toast("Không tải được thời tiết, kiểm tra mạng") }
            }
        }, { if (!silent) a.toast("Không lấy được vị trí. Hãy bật định vị (GPS) rồi thử lại.") })
    }

    private fun locate(ok: (Double, Double) -> Unit, fail: () -> Unit) {
        val lm = a.getSystemService(LocationManager::class.java)
        var best: Location? = null
        try {
            for (p in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.time > best!!.time)) best = l
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
            if (!done) { done = true; try { lm.removeUpdates(ls) } catch (e: SecurityException) { }
                if (b != null) ok(b.latitude, b.longitude) else fail() }
        }, 15000)
    }

    private fun placeName(lat: Double, lon: Double): String {
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
        try {
            val j = JSONObject(Render.httpGet("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=vi"))
            val parts = listOf(j.optString("locality"), j.optString("city"), j.optString("principalSubdivision")).filter { it.isNotBlank() }.distinct()
            if (parts.isNotEmpty()) return parts.take(2).joinToString(", ")
        } catch (e: Exception) { }
        return String.format(Locale.US, "%.2f, %.2f", lat, lon)
    }

    // ---------- chọn thành phố ----------
    private fun pickCity() {
        val root = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(18), a.dp(8), a.dp(18), 0) }
        val et = EditText(a).apply { hint = "Nhập tên thành phố, ví dụ: Đà Nẵng"; setSingleLine(); imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT }
        val lv = ListView(a)
        val names = ArrayList<String>(); val pts = ArrayList<Triple<String, Double, Double>>()
        val ad = ArrayAdapter(a, android.R.layout.simple_list_item_1, names)
        lv.adapter = ad
        val st = a.tv("", 13f, false, a.MUTED)
        fun search() {
            val q = et.text.toString().trim(); if (q.isEmpty()) return
            st.text = "Đang tìm…"
            thread {
                val res = ArrayList<Triple<String, Double, Double>>()
                try {
                    val j = JSONObject(Render.httpGet("https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(q, "UTF-8") + "&count=8&language=vi&format=json"))
                    val arr = j.optJSONArray("results")
                    if (arr != null) for (k in 0 until arr.length()) {
                        val o = arr.getJSONObject(k)
                        val nm = listOf(o.optString("name"), o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", ")
                        res.add(Triple(nm, o.getDouble("latitude"), o.getDouble("longitude")))
                    }
                } catch (e: Exception) { }
                a.ui.post { names.clear(); pts.clear(); res.forEach { names.add(it.first); pts.add(it) }; ad.notifyDataSetChanged()
                    st.text = if (res.isEmpty()) "Không tìm thấy kết quả" else "Chọn một kết quả:" }
            }
        }
        et.setOnEditorActionListener { _, _, _ -> search(); true }
        root.addView(et)
        root.addView(a.btn("Tìm", true) { search() })
        root.addView(st)
        root.addView(lv, LinearLayout.LayoutParams(-1, a.dp(240)))
        val dlg = AlertDialog.Builder(a).setTitle("Chọn thành phố").setView(root).setNegativeButton("Đóng", null).create()
        lv.setOnItemClickListener { _, _, pos, _ ->
            val t = pts[pos]
            S.p(a).edit().putFloat("lat", t.second.toFloat()).putFloat("lon", t.third.toFloat()).putString("place", t.first)
                .putBoolean("gps", false).putLong("wt", 0).apply()
            dlg.dismiss(); draw(); reload(true)
        }
        dlg.show()
    }

    // ---------- hiển thị ----------
    private fun draw() {
        box.removeAllViews()
        val json = S.p(a).getString("wjson", null)
        val place = Render.place(a)
        if (json == null) {
            val c = a.card(box, "Thời tiết")
            c.addView(a.tv("📍 $place", 16f, true)); c.addView(a.tv("Đang tải dữ liệu thời tiết…", 14f, false, a.MUTED))
            return
        }
        try { drawData(JSONObject(json), place) } catch (e: Exception) { a.card(box, "Thời tiết").addView(a.tv("Dữ liệu lỗi, hãy bấm Cập nhật thời tiết.", 14f, false, a.MUTED)) }
    }

    private fun drawData(j: JSONObject, place: String) {
        val cur = j.getJSONObject("current"); val hr = j.getJSONObject("hourly"); val dy = j.getJSONObject("daily")
        val code = cur.getInt("weather_code"); val isDay = cur.optInt("is_day", 1) == 1
        val t = cur.getDouble("temperature_2m").roundToInt()
        val feel = cur.getDouble("apparent_temperature").roundToInt()
        val hi = dy.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(); val lo = dy.getJSONArray("temperature_2m_min").getDouble(0).roundToInt()
        val upd = SimpleDateFormat("HH:mm", Locale.US).format(Date(S.p(a).getLong("wt", System.currentTimeMillis())))

        // --- thẻ chính ---
        val hero = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; setPadding(a.dp(20), a.dp(18), a.dp(20), a.dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, if (a.isDark) intArrayOf(0xFF2A47B8.toInt(), 0xFF35378F.toInt()) else intArrayOf(0xFF4568F0.toInt(), 0xFF5B4BD6.toInt())).apply { cornerRadius = a.dp(26).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(12), a.dp(6), a.dp(12), a.dp(6)) } }
        val top = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(a.tv("📍 $place", 17f, true, Color.WHITE).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(a.tv("⟳ $upd", 13f, true, Color.WHITE).apply { setPadding(a.dp(12), a.dp(6), a.dp(12), a.dp(6)); background = a.rounded(0x33FFFFFF, 18) })
        hero.addView(top)
        val mid = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        mid.addView(a.tv("$t°", 84f, false, Color.WHITE), LinearLayout.LayoutParams(0, -2, 1f))
        mid.addView(a.tv(Render.wxIcon(code, isDay), 76f))
        hero.addView(mid)
        hero.addView(a.tv(Render.wxText(code), 24f, true, Color.WHITE))
        hero.addView(a.tv("Cảm giác như $feel°", 15f, false, 0xE6FFFFFF.toInt()).apply { setPadding(0, a.dp(6), 0, 0) })
        hero.addView(a.tv("Thấp – cao hôm nay: $lo°–$hi°", 15f, false, 0xE6FFFFFF.toInt()))
        box.addView(hero)

        // --- chi tiết ---
        val dc = a.card(box, "Chi tiết")
        val tiles = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        fun tile(ic: String, v: String, l: String) {
            val x = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(a.dp(4), a.dp(14), a.dp(4), a.dp(14)); background = a.rounded(a.TILE, 18) }
            x.addView(a.tv(ic, 24f)); x.addView(a.tv(v, 20f, true)); x.addView(a.tv(l, 12f, false, a.MUTED))
            tiles.addView(x, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(a.dp(4), 0, a.dp(4), 0) })
        }
        tile("💧", "${cur.getDouble("relative_humidity_2m").roundToInt()}%", "Độ ẩm")
        tile("💨", "${cur.getDouble("wind_speed_10m").roundToInt()} km/h", "Gió " + Render.windDir(cur.getDouble("wind_direction_10m")))
        tile("🌧️", "${dy.getJSONArray("precipitation_probability_max").optInt(0, 0)}%", "Khả năng mưa")
        dc.addView(tiles)

        // --- theo giờ ---
        val hc = a.card(box, "Dự báo theo giờ")
        val times = hr.getJSONArray("time"); val ht = hr.getJSONArray("temperature_2m")
        val hp = hr.getJSONArray("precipitation_probability"); val hw = hr.getJSONArray("weather_code")
        val key = cur.getString("time").substring(0, 13)
        var st = 0
        for (k in 0 until times.length()) if (times.getString(k).startsWith(key)) { st = k; break }
        val hs = HorizontalScrollView(a).apply { isHorizontalScrollBarEnabled = false }
        val hrow = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        for (k in st until minOf(st + 13, times.length())) {
            val hour = times.getString(k).substring(11, 13).toInt()
            val x = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12))
                background = if (k == st) a.rounded(a.SEL, 18) else null }
            x.addView(a.tv(if (k == st) "Bây giờ" else times.getString(k).substring(11, 16), 14f, k == st, if (k == st) a.ACC else a.MUTED))
            x.addView(a.tv(Render.wxIcon(hw.getInt(k), hour in 6..17), 28f).apply { setPadding(0, a.dp(6), 0, a.dp(6)) })
            x.addView(a.tv("${ht.getDouble(k).roundToInt()}°", 18f, true))
            x.addView(a.tv("${hp.optInt(k, 0)}%", 13f, false, a.ACC))
            hrow.addView(x, LinearLayout.LayoutParams(-2, -2).apply { setMargins(a.dp(2), 0, a.dp(2), 0) })
        }
        hs.addView(hrow); hc.addView(hs)

        // --- 7 ngày ---
        val dd = a.card(box, "Dự báo 7 ngày")
        val dts = dy.getJSONArray("time"); val dmx = dy.getJSONArray("temperature_2m_max"); val dmn = dy.getJSONArray("temperature_2m_min")
        val dwc = dy.getJSONArray("weather_code"); val dpp = dy.getJSONArray("precipitation_probability_max")
        var wmin = 100.0; var wmax = -100.0
        for (k in 0 until dts.length()) { wmin = minOf(wmin, dmn.getDouble(k)); wmax = maxOf(wmax, dmx.getDouble(k)) }
        for (k in 0 until dts.length()) {
            val ds = dts.getString(k); val y = ds.substring(0, 4).toInt(); val mo = ds.substring(5, 7).toInt(); val d = ds.substring(8, 10).toInt()
            val wdn = Render.WD[Calendar.getInstance().apply { clear(); set(y, mo - 1, d) }.get(Calendar.DAY_OF_WEEK) - 1]
            val r = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, a.dp(8), 0, a.dp(8)) }
            val c1 = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            c1.addView(a.tv(if (k == 0) "Hôm nay" else wdn, 16f, true)); c1.addView(a.tv("$d/$mo", 13f, false, a.MUTED))
            r.addView(c1, LinearLayout.LayoutParams(a.dp(92), -2))
            r.addView(a.tv(Render.wxIcon(dwc.getInt(k)), 26f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(a.dp(40), -2))
            r.addView(a.tv("${dpp.optInt(k, 0)}%", 14f, true, a.ACC).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(a.dp(48), -2))
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
