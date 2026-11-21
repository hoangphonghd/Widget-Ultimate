package com.magicos.widget

import android.content.Context
import java.util.Locale

object Lang {
    fun isVi(c: Context): Boolean = when (S.i(c, "lang", 0)) {
        1 -> true
        2 -> false
        else -> Locale.getDefault().language == "vi"
    }
    fun tr(c: Context, vi: String, en: String): String = if (isVi(c)) vi else en
}
