package com.magicos.widget

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

object Lunar {
    private const val TZ = 7.0
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    fun yearName(y: Int) = CAN[(y + 6) % 10] + " " + CHI[(y + 8) % 12]
    fun hourName(h: Int) = "Giờ " + CHI[((h + 1) / 2) % 12]

    private fun jd(d: Int, m: Int, y: Int): Int {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85; val t2 = t * t; val t3 = t2 * t; val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 += -0.4068 * sin(mpr * dr) - 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 += 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 += -0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 += -0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 += 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val dt = if (t < -11) 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        else -0.000278 + 0.000265 * t + 0.000262 * t2
        return jd1 + c1 - dt
    }

    private fun sunLong(jdn: Double): Int {
        val t = (jdn - 2451545.0) / 36525; val t2 = t * t; val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.9146 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.00029 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * floor(l / (PI * 2))
        return (l / PI * 6).toInt()
    }

    private fun nmDay(k: Int) = (newMoon(k) + 0.5 + TZ / 24).toInt()
    private fun sunAt(day: Int) = sunLong(day - 0.5 - TZ / 24)

    private fun month11(yy: Int): Int {
        val off = jd(31, 12, yy) - 2415021
        val k = (off / 29.530588853).toInt()
        var nm = nmDay(k)
        if (sunAt(nm) >= 9) nm = nmDay(k - 1)
        return nm
    }

    private fun leapOffset(a11: Int): Int {
        val k = ((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var i = 1
        var arc = sunAt(nmDay(k + i))
        var last: Int
        do { last = arc; i++; arc = sunAt(nmDay(k + i)) } while (arc != last && i < 14)
        return i - 1
    }

    /** [ngày, tháng, năm âm, nhuận 0/1] */
    fun conv(dd: Int, mm: Int, yy: Int): IntArray {
        val dayNumber = jd(dd, mm, yy)
        val k = ((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = nmDay(k + 1)
        if (monthStart > dayNumber) monthStart = nmDay(k)
        var a11 = month11(yy)
        var b11 = a11
        var year: Int
        if (a11 >= monthStart) { year = yy; a11 = month11(yy - 1) } else { year = yy + 1; b11 = month11(yy + 1) }
        val day = dayNumber - monthStart + 1
        val diff = (monthStart - a11) / 29
        var leap = 0
        var month = diff + 11
        if (b11 - a11 > 365) {
            val ld = leapOffset(a11)
            if (diff >= ld) { month = diff + 10; if (diff == ld) leap = 1 }
        }
        if (month > 12) month -= 12
        if (month >= 11 && diff < 4) year -= 1
        return intArrayOf(day, month, year, leap)
    }
}
