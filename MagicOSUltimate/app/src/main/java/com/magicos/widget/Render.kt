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
import java.util.Calendar
import java.util.Locale
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
    val ICONS = arrayOf("", "●", "★", "♥", "⚡", "🔥", "👑", "📱", "◆", "○", "🌙")

    val DEF = mapOf(
        "bgL" to 0xFFF4F6FB.toInt(), "bgD" to 0xFF1B2030.toInt(), "glass" to 0xFFFFFFFF.toInt(),
        "txL" to 0xFF111827.toInt(), "txD" to 0xFFFFFFFF.toInt(), "txG" to 0xFFFFFFFF.toInt(),
        "cBt" to 0xFF5FC768.toInt(), "cTp" to 0xFFF2A33A.toInt(), "cRm" to 0xFF7B7BF5.toInt(),
        "cDk" to 0xFF5BC8B3.toInt(), "cWarn" to 0xFFE8503E.toInt(), "cToday" to 0xFFD2691E.toInt(),
        "cIco" to 0xFF5FC768.toInt()
    )
    fun col(c: Context, k: String) = S.i(c, k, DEF[k] ?: Color.BLACK)
    fun place(c: Context) = S.s(c, "place", "Hải Phòng")

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
        for (id in m.getAppWidgetIds(ComponentName(c, StdWidget::class.java))) m.updateAppWidget(id, build(c, if (isNight(c)) 1 else 0))
        for (id in m.getAppWidgetIds(ComponentName(c, GlassWidget::class.java))) m.updateAppWidget(id, build(c, 2))
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
        con.setRequestProperty("User-Agent", "MagicOSUltimate/3.0")
        return con.inputStream.bufferedReader().readText()
    }

    // ---------- thời tiết (Open-Meteo, không cần key) ----------
    fun refreshWeather(c: Context, force: Boolean): Boolean {
        val p = S.p(c)
        if (!force && System.currentTimeMillis() - p.getLong("wt", 0) < 20 * 60000) return true
        val lat = p.getFloat("lat", 20.86f); val lon = p.getFloat("lon", 106.68f)
        return try {
            val body = httpGet("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m" +
                "&hourly=temperature_2m,precipitation_probability,weather_code" +
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
    fun wxText(code: Int) = when (code) {
        0 -> "Trời quang"; 1, 2 -> "Ít mây"; 3 -> "Nhiều mây"; 45, 48 -> "Sương mù"; in 51..57 -> "Mưa phùn"
        in 61..67 -> "Mưa"; in 80..82 -> "Mưa rào"; in 71..77, 85, 86 -> "Tuyết"; 95, 96, 99 -> "Dông"; else -> "—"
    }
    fun windDir(deg: Double): String = arrayOf("Bắc", "Đông Bắc", "Đông", "Đông Nam", "Nam", "Tây Nam", "Tây", "Tây Bắc")[((deg + 22.5) / 45).toInt() % 8]
    fun tempStr(c: Context) = if (hasWx(c)) "${S.p(c).getFloat("t", 0f).roundToInt()}°" else "--°"
    fun rangeStr(c: Context): String {
        if (!hasWx(c)) return ""
        val p = S.p(c)
        return "${p.getFloat("lo", 0f).roundToInt()}°–${p.getFloat("hi", 0f).roundToInt()}° · Mưa ${p.getInt("pop", 0)}%"
    }

    // ---------- thông số máy ----------
    class Stats(val bl: Int, val chg: Boolean, val bt: Float, val ramPct: Int, val ramUsed: Double, val ramTotal: Double,
                val dkPct: Int, val dkTotal: Long)
    fun stats(c: Context): Stats {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val lv = (i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0) * 100 / (i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100)
        val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, 0) ?: 0
        val chg = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val tp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val mi = ActivityManager.MemoryInfo()
        c.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        val s = StatFs(Environment.getDataDirectory().path)
        return Stats(lv, chg, tp, ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt(),
            (mi.totalMem - mi.availMem) / 1e9, mi.totalMem / 1e9,
            ((s.totalBytes - s.availableBytes) * 100 / s.totalBytes).toInt(), (s.totalBytes / 1e9).roundToInt().toLong())
    }

    // ---------- dựng widget ----------
    private fun t(rv: RemoteViews, id: Int, s: String, color: Int) { rv.setTextViewText(id, s); rv.setTextColor(id, color) }

    private fun bar(rv: RemoteViews, box: Int, l: Int, v: Int, p: Int, label: String, value: String,
                    pct: Int, color: Int, show: Boolean, tx: Int, sub: Int) {
        rv.setViewVisibility(box, if (show) View.VISIBLE else View.GONE)
        t(rv, l, label, sub); t(rv, v, value, tx)
        rv.setProgressBar(p, 100, pct.coerceIn(0, 100), false)
        if (Build.VERSION.SDK_INT >= 31) rv.setColorStateList(p, "setProgressTintList", ColorStateList.valueOf(color))
    }

    private fun cell(c: Context, h: String, d: String, s: String?, today: Boolean, tx: Int, sub: Int): RemoteViews {
        val r = RemoteViews(c.packageName, R.layout.w_cell)
        t(r, R.id.c0, h, sub)
        t(r, R.id.c1, d, if (today) Color.WHITE else tx)
        if (s == null) r.setViewVisibility(R.id.c2, View.GONE) else t(r, R.id.c2, s, if (today) Color.WHITE else sub)
        if (today) {
            val tc = col(c, "cToday")
            r.setViewVisibility(R.id.hl, View.VISIBLE)
            r.setImageViewResource(R.id.hl, R.drawable.shape_fill)
            r.setInt(R.id.hl, "setColorFilter", tc or 0xFF000000.toInt())
            r.setInt(R.id.hl, "setImageAlpha", Color.alpha(tc))
        }
        return r
    }

    /** look: 0 = Chuẩn sáng, 1 = Chuẩn tối, 2 = Liquid Glass */
    fun build(c: Context, look: Int): RemoteViews {
        val rv = RemoteViews(c.packageName, R.layout.w_main)
        val glass = look == 2
        val bg = col(c, when (look) { 0 -> "bgL"; 1 -> "bgD"; else -> "glass" })
        val tx = col(c, when (look) { 0 -> "txL"; 1 -> "txD"; else -> "txG" })
        val sub = (tx and 0xFFFFFF) or 0xB3000000.toInt()
        // chạm vào widget = làm mới dữ liệu
        rv.setOnClickPendingIntent(R.id.root, PendingIntent.getBroadcast(c, 2,
            Intent(c, Tick::class.java).setAction(ACTION_REFRESH), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        rv.setImageViewResource(R.id.bg, R.drawable.shape_fill)
        rv.setInt(R.id.bg, "setColorFilter", bg or 0xFF000000.toInt())
        val a0 = Color.alpha(bg)
        rv.setInt(R.id.bg, "setImageAlpha", if (glass) S.i(c, "ga", 25) * a0 / 100 else a0)
        val bw = S.i(c, "bw", 1)
        if (glass && bw > 0) {
            rv.setViewVisibility(R.id.bd, View.VISIBLE)
            rv.setImageViewResource(R.id.bd, when (bw) { 1 -> R.drawable.shape_b1; 2 -> R.drawable.shape_b2; 3 -> R.drawable.shape_b3; else -> R.drawable.shape_b4 })
            rv.setInt(R.id.bd, "setColorFilter", 0xFFFFFFFF.toInt())
            rv.setInt(R.id.bd, "setImageAlpha", 170)
        } else rv.setViewVisibility(R.id.bd, View.GONE)

        val n = Calendar.getInstance()
        val dd = n.get(Calendar.DAY_OF_MONTH); val mo = n.get(Calendar.MONTH) + 1; val yy = n.get(Calendar.YEAR)
        val l = Lunar.conv(dd, mo, yy)

        val fmt = if (S.b(c, "sec", false)) "HH:mm:ss" else "HH:mm"
        rv.setCharSequence(R.id.clk, "setFormat24Hour", fmt)
        rv.setCharSequence(R.id.clk, "setFormat12Hour", fmt)
        rv.setTextColor(R.id.clk, tx)
        rv.setTextViewTextSize(R.id.clk, TypedValue.COMPLEX_UNIT_SP, 44f * S.i(c, "cs", 100) / 100f)
        if (S.b(c, "solar", true)) { rv.setViewVisibility(R.id.dateT, View.VISIBLE); t(rv, R.id.dateT, "${WD[n.get(Calendar.DAY_OF_WEEK) - 1]}, $dd tháng $mo", tx) }
        else rv.setViewVisibility(R.id.dateT, View.GONE)
        if (S.b(c, "lunar", true)) { rv.setViewVisibility(R.id.lunarT, View.VISIBLE); t(rv, R.id.lunarT, "Âm lịch: ${l[0]} tháng ${l[1]}${if (l[3] == 1) " (nhuận)" else ""}", sub) }
        else rv.setViewVisibility(R.id.lunarT, View.GONE)

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
            bar(rv, R.id.bx_bt, R.id.bt_l, R.id.bt_v, R.id.bt_p, if (st.chg) "PIN ⚡" else "PIN", "${st.bl}%", st.bl,
                if (st.bl <= 20 && !st.chg) warn else col(c, "cBt"), S.b(c, "bt", true), tx, sub)
            val st2 = if (st.bt < 30) "Mát" else if (st.bt < 40) "Ấm" else "Nóng"
            bar(rv, R.id.bx_tp, R.id.tp_l, R.id.tp_v, R.id.tp_p, "NHIỆT ĐỘ", String.format(Locale.US, "%.1f° %s", st.bt, st2),
                ((st.bt - 20) * 100 / 40).toInt(), if (st.bt >= 42) warn else col(c, "cTp"), S.b(c, "tp", true), tx, sub)
            bar(rv, R.id.bx_rm, R.id.rm_l, R.id.rm_v, R.id.rm_p, "RAM",
                String.format(Locale.US, "%d%% %.1f/%dG", st.ramPct, st.ramUsed, st.ramTotal.roundToInt()), st.ramPct,
                if (st.ramPct >= 90) warn else col(c, "cRm"), S.b(c, "rm", true), tx, sub)
            bar(rv, R.id.bx_dk, R.id.dk_l, R.id.dk_v, R.id.dk_p, "Ổ CỨNG", "${st.dkPct}% ${st.dkTotal}G", st.dkPct,
                if (st.dkPct >= 95) warn else col(c, "cDk"), S.b(c, "dk", true), tx, sub)
        } else if (mode == 1) {
            t(rv, R.id.wLoc, "📍 $city", sub); t(rv, R.id.wTemp, tempStr(c), tx)
            rv.setTextViewText(R.id.wIco, if (wxOk) wxIcon(code, day) else "⏳")
            t(rv, R.id.wDesc, if (wxOk) wxText(code) else "Đang tải…", tx); t(rv, R.id.wRange, rangeStr(c), sub)
        } else {
            t(rv, R.id.sBrand, name, sub); t(rv, R.id.sTemp, tempStr(c), tx); t(rv, R.id.sLoc, city, sub)
            t(rv, R.id.sDesc, if (wxOk) "${wxIcon(code, day)} ${wxText(code)}" else "Đang tải…", tx)
        }

        if (S.b(c, "week", true)) {
            rv.setViewVisibility(R.id.week, View.VISIBLE)
            rv.removeAllViews(R.id.week)
            val w = n.clone() as Calendar
            w.add(Calendar.DAY_OF_MONTH, -((n.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            for (i in 0..6) {
                val wl = Lunar.conv(w.get(Calendar.DAY_OF_MONTH), w.get(Calendar.MONTH) + 1, w.get(Calendar.YEAR))
                rv.addView(R.id.week, cell(c, HD[i], "${w.get(Calendar.DAY_OF_MONTH)}", if (S.b(c, "lunar", true)) "${wl[0]}" else null,
                    w.get(Calendar.DAY_OF_MONTH) == dd, tx, sub))
                w.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else rv.setViewVisibility(R.id.week, View.GONE)
        return rv
    }
}
