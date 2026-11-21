package com.magicos.widget

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

object Notes {
    class Note(val id: Long, val y: Int, val m: Int, val d: Int, val text: String, val at: Long)

    fun all(c: Context): MutableList<Note> {
        val out = ArrayList<Note>()
        try {
            val a = JSONArray(S.s(c, "notes", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(Note(o.getLong("id"), o.getInt("y"), o.getInt("m"), o.getInt("d"), o.getString("t"), o.optLong("at", 0)))
            }
        } catch (e: Exception) { }
        return out
    }

    private fun save(c: Context, l: List<Note>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("y", it.y).put("m", it.m).put("d", it.d).put("t", it.text).put("at", it.at)) }
        S.p(c).edit().putString("notes", a.toString()).apply()
    }

    fun forDay(c: Context, y: Int, m: Int, d: Int): List<Note> =
        all(c).filter { it.y == y && it.m == m && it.d == d }.sortedBy { it.at }

    fun hasNote(c: Context, y: Int, m: Int, d: Int): Boolean = all(c).any { it.y == y && it.m == m && it.d == d }

    fun add(c: Context, n: Note) {
        val l = all(c); l.add(n); save(c, l)
        if (n.at > System.currentTimeMillis()) schedule(c, n)
    }

    fun delete(c: Context, id: Long) {
        val l = all(c); val n = l.find { it.id == id }
        if (n != null) { cancel(c, n); l.remove(n); save(c, l) }
    }

    private fun pi(c: Context, n: Note): PendingIntent = PendingIntent.getBroadcast(c, (n.id % 1000000000L).toInt(),
        Intent(c, Reminder::class.java).putExtra("t", n.text).putExtra("id", n.id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun schedule(c: Context, n: Note) {
        try { c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.at, pi(c, n)) } catch (e: Exception) { }
    }

    private fun cancel(c: Context, n: Note) {
        try { c.getSystemService(AlarmManager::class.java).cancel(pi(c, n)) } catch (e: Exception) { }
    }

    fun rescheduleAll(c: Context) {
        val now = System.currentTimeMillis()
        all(c).filter { it.at > now }.forEach { schedule(c, it) }
    }
}

class Reminder : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val text = i.getStringExtra("t") ?: return
        val id = i.getLongExtra("id", 0L)
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("notes", "Ghi chú & nhắc nhở", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "notes").setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("MagicOS Ultimate").setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build()
        try { nm.notify((id % 1000000000L).toInt(), n) } catch (e: Exception) { }
    }
}
