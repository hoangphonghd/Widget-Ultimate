package com.magicos.widget

import java.util.Calendar

/** Lịch vạn niên: can chi, tiết khí, trực, sao, giờ hoàng đạo, sự kiện... (mang tính tham khảo) */
object Almanac {
    val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    val ANIMAL = arrayOf("Chuột", "Trâu", "Hổ", "Mèo", "Rồng", "Rắn", "Ngựa", "Dê", "Khỉ", "Gà", "Chó", "Heo")
    val TERMS = arrayOf("Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng", "Hạ chí", "Tiểu thử",
        "Đại thử", "Lập thu", "Xử thử", "Bạch lộ", "Thu phân", "Hàn lộ", "Sương giáng", "Lập đông", "Tiểu tuyết",
        "Đại tuyết", "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập")
    private val TRUC_N = arrayOf("Kiến", "Trừ", "Mãn", "Bình", "Định", "Chấp", "Phá", "Nguy", "Thành", "Thu", "Khai", "Bế")
    private val TRUC_T = arrayOf(
        "Khởi đầu, tốt cho xuất hành, khai trương; kỵ động thổ",
        "Trừ bỏ cái cũ, tốt cho dọn dẹp, chữa bệnh; kỵ cưới hỏi",
        "Đầy đủ, tốt cho cầu tài, cúng lễ; kỵ khởi công",
        "Bằng phẳng, tốt cho cưới hỏi, giao dịch, nhập học",
        "Ổn định, tốt cho cưới hỏi, ký kết, an cư",
        "Giữ gìn, tốt cho xây cất, trồng trọt; kỵ xuất hành",
        "Phá hoại, chỉ nên phá dỡ, dọn dẹp; xấu cho việc lớn",
        "Nguy hiểm, nên thận trọng, tránh leo cao, đi xa",
        "Thành công, tốt cho khai trương, cưới hỏi, nhập trạch",
        "Thu hoạch, tốt cho thu nợ, mua sắm",
        "Khai mở, tốt cho khai trương, cầu tài, động thổ",
        "Đóng kín, tốt cho đắp đê, lấp hố; kỵ khai trương, xuất hành")
    private val TRUC_R = intArrayOf(1, 0, 0, 1, 1, 0, -1, -1, 1, 1, 1, -1)
    private val STAR = arrayOf("Thanh Long", "Minh Đường", "Thiên Hình", "Chu Tước", "Kim Quỹ", "Thiên Đức",
        "Bạch Hổ", "Ngọc Đường", "Thiên Lao", "Huyền Vũ", "Tư Mệnh", "Câu Trần")
    private val STAR_GOOD = booleanArrayOf(true, true, false, false, true, true, false, true, false, false, true, false)
    private val TU = arrayOf("Giác Mộc Giao", "Cang Kim Long", "Đê Thổ Lạc", "Phòng Nhật Thố", "Tâm Nguyệt Hồ", "Vĩ Hỏa Hổ",
        "Cơ Thủy Báo", "Đẩu Mộc Giải", "Ngưu Kim Ngưu", "Nữ Thổ Bức", "Hư Nhật Thử", "Nguy Nguyệt Yến", "Thất Hỏa Trư",
        "Bích Thủy Du", "Khuê Mộc Lang", "Lâu Kim Cẩu", "Vị Thổ Trĩ", "Mão Nhật Kê", "Tất Nguyệt Ô", "Chủy Hỏa Hầu",
        "Sâm Thủy Viên", "Tỉnh Mộc Hãn", "Quỷ Kim Dương", "Liễu Thổ Chương", "Tinh Nhật Mã", "Trương Nguyệt Lộc",
        "Dực Hỏa Xà", "Chẩn Thủy Dẫn")
    private val LD_N = arrayOf("Đại An", "Lưu Liên", "Tốc Hỷ", "Xích Khẩu", "Tiểu Cát", "Không Vong")
    private val LD_T = arrayOf("Mọi việc tốt lành, cầu tài, xuất hành đều thuận", "Công việc trì trệ, khó thành, nên hoãn",
        "Có tin vui, cầu tài thuận lợi", "Dễ cãi vã, thị phi, nên cẩn trọng lời nói",
        "Nhiều may mắn, cầu tài, gặp quý nhân", "Việc dễ hỏng, mất mát, không nên khởi sự")
    private val LD_G = booleanArrayOf(true, false, true, false, true, false)
    private val HOURS = arrayOf(intArrayOf(0, 1, 3, 6, 8, 9), intArrayOf(2, 3, 5, 8, 10, 11), intArrayOf(0, 1, 4, 5, 7, 10),
        intArrayOf(0, 2, 3, 6, 7, 9), intArrayOf(2, 4, 5, 8, 9, 11), intArrayOf(1, 4, 6, 7, 10, 11))
    private val DUONG = arrayOf(1 to 13, 2 to 11, 3 to 9, 4 to 7, 5 to 5, 6 to 3, 7 to 1, 7 to 29, 8 to 27, 9 to 25, 10 to 23, 11 to 21, 12 to 19)
    private val SOLAR = arrayOf(
        Triple(1, 1, "Tết Dương lịch"), Triple(9, 1, "Ngày Học sinh sinh viên Việt Nam"), Triple(3, 2, "Ngày thành lập Đảng Cộng sản Việt Nam"),
        Triple(14, 2, "Lễ tình nhân (Valentine)"), Triple(27, 2, "Ngày Thầy thuốc Việt Nam"), Triple(8, 3, "Quốc tế Phụ nữ"),
        Triple(26, 3, "Ngày thành lập Đoàn TNCS Hồ Chí Minh"), Triple(1, 4, "Ngày Cá tháng Tư"), Triple(21, 4, "Ngày Sách và Văn hóa đọc Việt Nam"),
        Triple(22, 4, "Ngày Trái Đất"), Triple(30, 4, "Ngày Giải phóng miền Nam"), Triple(1, 5, "Quốc tế Lao động"),
        Triple(7, 5, "Chiến thắng Điện Biên Phủ"), Triple(12, 5, "Ngày Quốc tế Điều dưỡng"), Triple(15, 5, "Ngày thành lập Đội TNTP Hồ Chí Minh"),
        Triple(19, 5, "Sinh nhật Chủ tịch Hồ Chí Minh"), Triple(1, 6, "Quốc tế Thiếu nhi"), Triple(5, 6, "Ngày Môi trường thế giới"),
        Triple(21, 6, "Ngày Báo chí Cách mạng Việt Nam"), Triple(28, 6, "Ngày Gia đình Việt Nam"), Triple(11, 7, "Ngày Dân số thế giới"),
        Triple(27, 7, "Ngày Thương binh liệt sĩ"), Triple(28, 7, "Ngày thành lập Công đoàn Việt Nam"), Triple(19, 8, "Cách mạng tháng Tám"),
        Triple(2, 9, "Quốc khánh Việt Nam"), Triple(10, 10, "Ngày Giải phóng Thủ đô"), Triple(13, 10, "Ngày Doanh nhân Việt Nam"),
        Triple(20, 10, "Ngày Phụ nữ Việt Nam"), Triple(31, 10, "Halloween"), Triple(9, 11, "Ngày Pháp luật Việt Nam"),
        Triple(20, 11, "Ngày Nhà giáo Việt Nam"), Triple(23, 11, "Ngày thành lập Hội Chữ thập đỏ Việt Nam"), Triple(24, 11, "Ngày Văn hóa Việt Nam"),
        Triple(1, 12, "Ngày Thế giới phòng chống AIDS"), Triple(19, 12, "Ngày Toàn quốc kháng chiến"),
        Triple(22, 12, "Ngày thành lập Quân đội nhân dân Việt Nam"), Triple(24, 12, "Đêm Giáng sinh"), Triple(25, 12, "Lễ Giáng sinh"))
    private val LUNAR = arrayOf(
        Triple(1, 1, "Tết Nguyên đán"), Triple(2, 1, "Mùng 2 Tết"), Triple(3, 1, "Mùng 3 Tết"), Triple(15, 1, "Tết Nguyên tiêu"),
        Triple(3, 3, "Tết Hàn thực"), Triple(10, 3, "Giỗ Tổ Hùng Vương"), Triple(15, 4, "Lễ Phật Đản"), Triple(5, 5, "Tết Đoan ngọ"),
        Triple(15, 7, "Lễ Vu Lan"), Triple(15, 8, "Tết Trung thu"), Triple(9, 9, "Tết Trùng cửu"), Triple(15, 10, "Tết Hạ nguyên"),
        Triple(23, 12, "Ông Táo chầu trời"))

    fun fromJd(j: Int): IntArray {
        val a = j + 32044; val b = (4 * a + 3) / 146097; val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461; val e = c - 1461 * d / 4; val m = (5 * e + 2) / 153
        return intArrayOf(e - (153 * m + 2) / 5 + 1, m + 3 - 12 * (m / 10), 100 * b + d - 4800 + m / 10)
    }
    private fun two(x: Int) = if (x < 10) "0$x" else "$x"
    fun hourRange(h: Int): String { val s = (23 + 2 * h) % 24; return two(s) + "-" + two((s + 2) % 24) + "h" }

    class Info(val d: Int, val m: Int, val y: Int) {
        val jdn = Lunar.jd(d, m, y)
        val lunar: IntArray = Lunar.conv(d, m, y)
        val can = (jdn + 9) % 10
        val chi = (jdn + 1) % 12
        val dayName: String get() = CAN[can] + " " + CHI[chi]
        val monthName: String get() { val yc = (lunar[2] + 6) % 10; return CAN[(yc * 2 + lunar[1] + 1) % 10] + " " + CHI[(lunar[1] + 1) % 12] }
        val yearName: String get() = Lunar.yearName(lunar[2])
        val animal: String get() = ANIMAL[(lunar[2] + 8) % 12]
        val termCur: Int by lazy { Lunar.termIdx(jdn + 1) }
        private val monthChi: Int by lazy { val b = if (termCur % 2 == 1) termCur else (termCur + 23) % 24; (2 + ((b - 21 + 24) % 24) / 2) % 12 }
        private val starIdx: Int get() = (chi - ((monthChi - 2 + 12) % 6) * 2 + 12) % 12
        val hoangDao: Boolean get() = STAR_GOOD[starIdx]
        val starName: String get() = STAR[starIdx]
        private val trucIdx: Int get() = (chi - monthChi + 12) % 12
        val trucName: String get() = TRUC_N[trucIdx]
        val trucText: String get() = TRUC_T[trucIdx]
        val trucRating: Int get() = TRUC_R[trucIdx]
        val tu: String get() = "Sao " + TU[(jdn + 11) % 28]
        private val ldIdx: Int get() = (lunar[1] + lunar[0] - 2) % 6
        val ldName: String get() = LD_N[ldIdx]
        val ldText: String get() = LD_T[ldIdx]
        val ldGood: Boolean get() = LD_G[ldIdx]
        val hyThan: String get() = arrayOf("Đông Bắc", "Tây Bắc", "Tây Nam", "Chính Nam", "Đông Nam")[can % 5]
        val taiThan: String get() = arrayOf("Đông Nam", "Chính Đông", "Chính Bắc", "Tây Nam", "Chính Tây")[can / 2]
        val tuoiXung: String get() = "Tuổi " + CHI[(chi + 6) % 12] + " (" + ANIMAL[(chi + 6) % 12] + ")"
        val goodHours: IntArray get() = HOURS[chi % 6]
        fun termInfo(): String {
            val cur = termCur
            var s = jdn; while (Lunar.termIdx(s) == cur) s--
            var c = jdn; while (Lunar.termIdx(c + 1) == cur) c++
            val a = fromJd(s); val b = fromJd(c)
            return "${TERMS[cur]} (từ ${two(a[0])}/${two(a[1])})\nTiếp theo: ${TERMS[(cur + 1) % 24]} ${two(b[0])}/${two(b[1])}"
        }
        fun kyList(): List<String> {
            val r = ArrayList<String>()
            if (lunar[0] in intArrayOf(3, 7, 13, 18, 22, 27)) r.add("Tam nương")
            if (lunar[0] in intArrayOf(5, 14, 23)) r.add("Nguyệt kỵ")
            if (DUONG.any { it.first == lunar[1] && it.second == lunar[0] }) r.add("Dương Công kỵ")
            return r
        }
        fun events(): List<String> {
            val r = ArrayList<String>()
            for (e in SOLAR) if (e.first == d && e.second == m) r.add(e.third)
            if (lunar[3] == 0) for (e in LUNAR) if (e.first == lunar[0] && e.second == lunar[1]) r.add(e.third)
            val cal = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }
            val nth = (d - 1) / 7 + 1
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                if (m == 5 && nth == 2) r.add("Ngày của Mẹ")
                if (m == 6 && nth == 3) r.add("Ngày của Cha")
            }
            return r
        }
    }
}
