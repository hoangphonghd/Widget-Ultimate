package com.magicos.widget

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object S {
    fun p(c: Context): SharedPreferences = c.getSharedPreferences("w", 0)
    fun i(c: Context, k: String, d: Int) = p(c).getInt(k, d)
    fun b(c: Context, k: String, d: Boolean) = p(c).getBoolean(k, d)
    fun s(c: Context, k: String, d: String) = p(c).getString(k, d) ?: d
}

object Render {
    const val ACTION_REFRESH = "com.magicos.widget.REFRESH"
    val WD = arrayOf("Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy")
    private val HD = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    private val HE = arrayOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    val ICONS = arrayOf("", "●", "★", "♥", "⚡", "🔥", "👑", "📱", "◆", "○", "🌙")

    val DEF = mapOf(
        "bgL" to 0xFFF4F6FB.toInt(), "bgD" to 0xFF111111.toInt(), "glass" to 0xFFFFFFFF.toInt(),
        "txL" to 0xFF111827.toInt(), "txD" to 0xFFFFFFFF.toInt(), "txG" to 0xFFFFFFFF.toInt(),
        "cBt" to 0xFF5FC768.toInt(), "cTp" to 0xFFF2A33A.toInt(), "cRm" to 0xFF7B7BF5.toInt(),
        "cDk" to 0xFF5BC8B3.toInt(), "cWarn" to 0xFFE8503E.toInt(), "cToday" to 0xFFD2691E.toInt(),
        "cIco" to 0xFF5FC768.toInt()
    )
    fun col(c: Context, k: String) = S.i(c, k, DEF[k] ?: Color.BLACK)
    fun place(c: Context): String = S.s(c, "placeCustom", "").ifBlank { S.s(c, "place", "Hải Phòng") }
    fun isNight(c: Context) = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // ---------- cập nhật tất cả widget ----------
    fun updateAll(c0: Context, force: Boolean) {
        val c = c0.applicationContext
        schedule(c)
        if (force) { renderAll(c); refreshWeather(c, true) } else refreshWeather(c, false)
        renderAll(c)
    }

    private fun renderAll(c: Context) {
        val m = AppWidgetManager.getInstance(c)
        fun go(cls: Class<*>, look: Int) {
            for (id in m.getAppWidgetIds(ComponentName(c, cls))) {
                val o = m.getAppWidgetOptions(id)
                val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).let { if (it > 0) it.toFloat() else 320f }
                val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).let { if (it > 0) it.toFloat() else 150f }
                m.updateAppWidget(id, build(c, look, w, h))
            }
        }
        go(StdWidget::class.java, if (isNight(c)) 1 else 0)
        go(GlassWidget::class.java, 2)
        S.p(c).edit().putLong("lu", System.currentTimeMillis()).apply()
    }

    private fun schedule(c: Context) {
        val i = Intent(c, Tick::class.java)
        val f = PendingIntent.FLAG_IMMUTABLE
        if (PendingIntent.getBroadcast(c, 1, i, f or PendingIntent.FLAG_NO_CREATE) == null) {
            val pi = PendingIntent.getBroadcast(c, 1, i, f or PendingIntent.FLAG_UPDATE_CURRENT)
            c.getSystemService(AlarmManager::class.java)
                .setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 900000, 900000, pi)
        }
    }

    fun httpGet(u: String): String {
        val con = URL(u).openConnection() as HttpURLConnection
        con.connectTimeout = 8000; con.readTimeout = 8000
        con.setRequestProperty("User-Agent", "MagicOSUltimate/4.0")
        return con.inputStream.bufferedReader().readText()
    }

    // ---------- thời tiết (Open-Meteo) ----------
    fun refreshWeather(c: Context, force: Boolean): Boolean {
        val p = S.p(c)
        if (!force && System.currentTimeMillis() - p.getLong("wt", 0) < 20 * 60000) return true
        val lat = p.getFloat("lat", 20.86f); val lon = p.getFloat("lon", 106.68f)
        return try {
            val body = httpGet("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,cloud_cover" +
                "&hourly=temperature_2m,precipitation_probability,weather_code,uv_index,visibility" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
                "&timezone=auto&forecast_days=7")
            val j = JSONObject(body)
            val cur = j.getJSONObject("current"); val d = j.getJSONObject("daily")
            p.edit().putString("wjson", body)
                .putFloat("t", cur.getDouble("temperature_2m").toFloat())
                .putInt("code", cur.getInt("weather_code"))
                .putBoolean("day", cur.optInt("is_day", 1) == 1)
                .putFloat("hi", d.getJSONArray("temperature_2m_max").getDouble(0).toFloat())
                .putFloat("lo", d.getJSONArray("temperature_2m_min").getDouble(0).toFloat())
                .putInt("pop", d.getJSONArray("precipitation_probability_max").optInt(0, 0))
                .putString("sr", d.getJSONArray("sunrise").getString(0).substring(11, 16))
                .putString("ss", d.getJSONArray("sunset").getString(0).substring(11, 16))
                .putLong("wt", System.currentTimeMillis()).apply()
            true
        } catch (e: Exception) { false }
    }

    fun hasWx(c: Context) = S.p(c).getLong("wt", 0) != 0L
    fun wxIcon(code: Int, day: Boolean = true) = when (code) {
        0 -> if (day) "☀️" else "🌙"; 1, 2 -> if (day) "🌤️" else "☁️"; 3 -> "☁️"; 45, 48 -> "🌫️"; in 51..57 -> "🌦️"
        in 61..67, in 80..82 -> "🌧️"; in 71..77, 85, 86 -> "❄️"; 95, 96, 99 -> "⛈️"; else -> "🌡️"
    }
    fun wxText(c: Context, code: Int): String {
        val vi = Lang.isVi(c)
        return when (code) {
            0 -> if (vi) "Trời quang" else "Clear"; 1, 2 -> if (vi) "Ít mây" else "Partly cloudy"; 3 -> if (vi) "Nhiều mây" else "Cloudy"
            45, 48 -> if (vi) "Sương mù" else "Fog"; in 51..57 -> if (vi) "Mưa phùn" else "Drizzle"
            in 61..67 -> if (vi) "Mưa" else "Rain"; in 80..82 -> if (vi) "Mưa rào" else "Showers"
            in 71..77, 85, 86 -> if (vi) "Tuyết" else "Snow"; 95, 96, 99 -> if (vi) "Dông" else "Thunderstorm"; else -> "—"
        }
    }
    fun windDir(deg: Double): String = arrayOf("Bắc", "Đông Bắc", "Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc")[((deg + 22.5) / 45).toInt() % 8]
    fun fmtT(v: Float): String = if (abs(v - v.roundToInt()) < 0.05f) "${v.roundToInt()}°" else String.format(Locale.US, "%.1f°", v)
    fun tempStr(c: Context) = if (hasWx(c)) "${S.p(c).getFloat("t", 0f).roundToInt()}°" else "--°"
    fun rangeStr(c: Context): String {
        if (!hasWx(c)) return ""
        val p = S.p(c)
        return "${p.getFloat("lo", 0f).roundToInt()}°–${p.getFloat("hi", 0f).roundToInt()}° · ${Lang.tr(c, "Mưa", "Rain")} ${p.getInt("pop", 0)}%"
    }

    // ---------- thông số máy ----------
    class Stats(val bl: Int, val chg: Boolean, val bt: Float, val mah: Int, val ramPct: Int, val ramUsed: Double, val ramTotal: Double,
                val dkPct: Int, val dkTotal: Int)

    private fun batteryMah(c: Context, pct: Int): Int {
        val cached = S.i(c, "mah", 0)
        if (cached > 0) return cached
        var mah = 0
        try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val o = cls.getConstructor(Context::class.java).newInstance(c)
            mah = (cls.getMethod("getBatteryCapacity").invoke(o) as Double).toInt()
        } catch (e: Throwable) { }
        if (mah <= 0 && pct >= 20) {
            try {
                val cc = c.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                if (cc > 0) mah = ((cc / 1000.0 * 100.0 / pct / 100.0).roundToInt()) * 100
            } catch (e: Throwable) { }
        }
        if (mah > 0) S.p(c).edit().putInt("mah", mah).apply()
        return mah
    }

    fun stats(c: Context): Stats {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lv = (i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0) * 100 / (i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100)
        val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
        val chg = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val tp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val mi = ActivityManager.MemoryInfo()
        c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        val s = StatFs(Environment.getDataDirectory().path)
        val gb = s.totalBytes / 1e9
        val std = intArrayOf(16, 32, 64, 128, 256, 512, 1024, 2048)
        val total = std.firstOrNull { it >= gb * 0.98 } ?: gb.roundToInt()
        return Stats(lv, chg, tp, batteryMah(c, lv), ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt(),
            (mi.totalMem - mi.availMem) / 1e9, mi.totalMem / 1e9,
            ((s.totalBytes - s.availableBytes) * 100 / s.totalBytes).toInt(), total)
    }

    // ---------- dựng widget ----------
    private fun t(rv: RemoteViews, id: Int, s: String, color: Int) { rv.setTextViewText(id, s); rv.setTextColor(id, color) }

    private fun bar(rv: RemoteViews, box: Int, ic: Int, icRes: Int, l: Int, v: Int, v2: Int, p: Int, label: String, value: String,
                    value2: String, pct: Int, color: Int, show: Boolean, tx: Int, sub: Int) {
        rv.setViewVisibility(box, if (show) View.VISIBLE else View.GONE)
        rv.setImageViewResource(ic, icRes); rv.setInt(ic, "setColorFilter", color)
        t(rv, l, label, sub); t(rv, v, value, tx); t(rv, v2, value2, sub)
        rv.setProgressBar(p, 100, pct.coerceIn(0, 100), false)
        if (Build.VERSION.SDK_INT >= 31) rv.setColorStateList(p, "setProgressTintList", ColorStateList.valueOf(color))
    }

    private fun cell(c: Context, h: String, d: String, s: String?, today: Boolean, tx: Int, sub: Int, dSp: Float = 14f): RemoteViews {
        val r = RemoteViews(c.packageName, R.layout.w_cell)
        t(r, R.id.c0, h, sub)
        t(r, R.id.c1, d, if (today) Color.WHITE else tx)
        r.setTextViewTextSize(R.id.c1, TypedValue.COMPLEX_UNIT_SP, dSp)
        if (s == null) r.setViewVisibility(R.id.c2, View.GONE) else t(r, R.id.c2, s, if (today) Color.WHITE else sub)
        if (today) {
            val tc = col(c, "cToday")
            r.setViewVisibility(R.id.hl, View.VISIBLE)
            r.setImageViewResource(R.id.hl, R.drawable.shape_pill)
            r.setInt(R.id.hl, "setColorFilter", tc or 0xFF000000.toInt())
            r.setInt(R.id.hl, "setImageAlpha", Color.alpha(tc))
        }
        return r
    }

    /** look: 0 = Sáng, 1 = Tối, 2 = Kính lỏng */
    fun build(c: Context, look: Int, wDp: Float = 320f, hDp: Float = 150f): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_main)
        val vi = Lang.isVi(c)
        val tx = col(c, when (look) { 0 -> "txL"; 1 -> "txD"; else -> "txG" })
        val sub = (tx and 0xFFFFFF) or 0xB3000000.toInt()
        rv.setOnClickPendingIntent(R.id.root, PendingIntent.getBroadcast(c, 2,
            Intent(c, Tick::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        rv.setImageViewBitmap(R.id.bg, Bg.make(c, look, wDp, hDp))

        val n = Calendar.getInstance()
        val dd = n.get(Calendar.DAY_OF_MONTH); val mo = n.get(Calendar.MONTH) + 1; val yy = n.get(Calendar.YEAR)
        val l = Lunar.conv(dd, mo, yy)

        val fmt = if (S.b(c, "sec", false)) "HH:mm:ss" else "HH:mm"
        rv.setCharSequence(R.id.clk, "setFormat24Hour", fmt)
        rv.setCharSequence(R.id.clk, "setFormat12Hour", fmt)
        rv.setTextColor(R.id.clk, tx)
        rv.setTextViewTextSize(R.id.clk, TypedValue.COMPLEX_UNIT_SP, 44f * S.i(c, "cs", 100) / 100f)
        if (S.b(c, "solar", true)) {
            rv.setViewVisibility(R.id.dateT, View.VISIBLE)
            t(rv, R.id.dateT, if (vi) "${WD[n.get(Calendar.DAY_OF_WEEK) - 1]}, $dd tháng $mo" else SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(n.time), tx)
        } else rv.setViewVisibility(R.id.dateT, View.GONE)
        if (S.b(c, "lunar", true)) {
            rv.setViewVisibility(R.id.lunarT, View.VISIBLE)
            t(rv, R.id.lunarT, if (vi) "Âm lịch: ${l[0]} tháng ${l[1]}${if (l[3] == 1) " (nhuận)" else ""}" else "Lunar: ${l[0]}/${l[1]}", sub)
        } else rv.setViewVisibility(R.id.lunarT, View.GONE)

        val mode = S.i(c, "mode", 0)
        rv.setViewVisibility(R.id.pStats, if (mode == 0) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.pWx, if (mode == 1) View.VISIBLE else View.GONE)
        rv.setViewVisibility(R.id.pSimple, if (mode == 2) View.VISIBLE else View.GONE)
        val name = S.s(c, "name", "").ifBlank { Build.MODEL }
        val city = place(c)
        val p = S.p(c)
        val code = p.getInt("code", 0); val day = p.getBoolean("day", true)
        val wxOk = hasWx(c)

        if (mode == 0) {
            val st = stats(c)
            rv.setViewVisibility(R.id.nameRow, if (S.b(c, "nameRow", true)) View.VISIBLE else View.GONE)
            val ico = S.i(c, "ico", 1); val blink = S.b(c, "blink", false)
            if (ico == 0) {
                rv.setViewVisibility(R.id.icoFlip, View.GONE); rv.setViewVisibility(R.id.icoS, View.GONE)
            } else {
                val ch = ICONS[ico]; val ic = col(c, "cIco")
                if (blink) {
                    rv.setViewVisibility(R.id.icoFlip, View.VISIBLE); rv.setViewVisibility(R.id.icoS, View.GONE)
                    t(rv, R.id.ico, ch, ic); t(rv, R.id.icoB, ch, 0x00000000)
                } else {
                    rv.setViewVisibility(R.id.icoFlip, View.GONE); rv.setViewVisibility(R.id.icoS, View.VISIBLE)
                    t(rv, R.id.icoS, ch, ic)
                }
            }
            t(rv, R.id.nm, name, sub); t(rv, R.id.rf, "⟳", sub)
            val warn = col(c, "cWarn")
            bar(rv, R.id.bx_bt, R.id.bt_i, R.drawable.ic_bat, R.id.bt_l, R.id.bt_v, R.id.bt_v2, R.id.bt_p,
                (if (vi) "PIN" else "BATTERY") + (if (st.chg) " ⚡" else ""), "${st.bl}%", if (st.mah > 0) "${st.mah}mAh" else "", st.bl,
                if (st.bl <= 20 && !st.chg) warn else col(c, "cBt"), S.b(c, "bt", true), tx, sub)
            val st2 = if (st.bt < 30) (if (vi) "Mát" else "Cool") else if (st.bt < 40) (if (vi) "Ấm" else "Warm") else (if (vi) "Nóng" else "Hot")
            bar(rv, R.id.bx_tp, R.id.tp_i, R.drawable.ic_tmp, R.id.tp_l, R.id.tp_v, R.id.tp_v2, R.id.tp_p,
                if (vi) "NHIỆT ĐỘ" else "TEMP", fmtT(st.bt), st2, ((st.bt - 20) * 100 / 40).toInt(),
                if (st.bt >= 42) warn else col(c, "cTp"), S.b(c, "tp", true), tx, sub)
            bar(rv, R.id.bx_rm, R.id.rm_i, R.drawable.ic_ram, R.id.rm_l, R.id.rm_v, R.id.rm_v2, R.id.rm_p,
                "RAM", "${st.ramPct}%", String.format(Locale.US, "%.1f/%dGB", st.ramUsed, st.ramTotal.roundToInt()), st.ramPct,
                if (st.ramPct >= 90) warn else col(c, "cRm"), S.b(c, "rm", true), tx, sub)
            bar(rv, R.id.bx_dk, R.id.dk_i, R.drawable.ic_rom, R.id.dk_l, R.id.dk_v, R.id.dk_v2, R.id.dk_p,
                if (vi) "Ổ CỨNG" else "STORAGE", "${st.dkPct}%", "${st.dkTotal}GB", st.dkPct,
                if (st.dkPct >= 95) warn else col(c, "cDk"), S.b(c, "dk", true), tx, sub)
        } else if (mode == 1) {
            t(rv, R.id.wLoc, "📍 $city", sub); t(rv, R.id.wTemp, tempStr(c), tx)
            rv.setTextViewText(R.id.wIco, if (wxOk) wxIcon(code, day) else "⏳")
            t(rv, R.id.wDesc, if (wxOk) wxText(c, code) else (if (vi) "Đang tải…" else "Loading…"), tx); t(rv, R.id.wRange, rangeStr(c), sub)
        } else {
            t(rv, R.id.sBrand, name, sub); t(rv, R.id.sTemp, tempStr(c), tx); t(rv, R.id.sLoc, city, sub)
            t(rv, R.id.sDesc, if (wxOk) "${wxIcon(code, day)} ${wxText(c, code)}" else (if (vi) "Đang tải…" else "Loading…"), tx)
        }

        rv.removeAllViews(R.id.week)
        if (S.b(c, "week7", false) && wxOk) {
            rv.setViewVisibility(R.id.week, View.VISIBLE)
            try {
                val dy = JSONObject(p.getString("wjson", "{}")!!).getJSONObject("daily")
                val ts = dy.getJSONArray("time"); val cd = dy.getJSONArray("weather_code")
                val mx = dy.getJSONArray("temperature_2m_max"); val mn = dy.getJSONArray("temperature_2m_min")
                for (i in 0 until minOf(7, ts.length())) {
                    val ds = ts.getString(i)
                    val cal = Calendar.getInstance().apply { clear(); set(ds.substring(0, 4).toInt(), ds.substring(5, 7).toInt() - 1, ds.substring(8, 10).toInt()) }
                    val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    rv.addView(R.id.week, cell(c, if (i == 0) (if (vi) "H.nay" else "Today") else (if (vi) HD[idx] else HE[idx]), wxIcon(cd.getInt(i)),
                        "${mn.getDouble(i).roundToInt()}°/${mx.getDouble(i).roundToInt()}°", i == 0, tx, sub, 15f))
                }
            } catch (e: Exception) { rv.setViewVisibility(R.id.week, View.GONE) }
        } else if (S.b(c, "week", true)) {
            rv.setViewVisibility(R.id.week, View.VISIBLE)
            val w = n.clone() as Calendar
            w.add(Calendar.DAY_OF_MONTH, -((n.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            for (i in 0..6) {
                val wl = Lunar.conv(w.get(Calendar.DAY_OF_MONTH), w.get(Calendar.MONTH) + 1, w.get(Calendar.YEAR))
                rv.addView(R.id.week, cell(c, if (vi) HD[i] else HE[i], "${w.get(Calendar.DAY_OF_MONTH)}", if (S.b(c, "lunar", true)) "${wl[0]}" else null,
                    w.get(Calendar.DAY_OF_MONTH) == dd, tx, sub))
                w.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else rv.setViewVisibility(R.id.week, View.GONE)
        return rv
    }
}
