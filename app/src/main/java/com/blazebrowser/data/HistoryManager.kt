package com.blazebrowser.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val title: String, val url: String, val timestamp: Long)

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_history", Context.MODE_PRIVATE)
    
    fun getHistory(): List<HistoryEntry> {
        val json = prefs.getString("history", "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map {
            val obj = array.getJSONObject(it)
            HistoryEntry(obj.getString("title"), obj.getString("url"), obj.getLong("timestamp"))
        }
    }
    
    fun addEntry(title: String, url: String) {
        val list = getHistory().toMutableList()
        list.removeAll { it.url == url }
        list.add(0, HistoryEntry(title, url, System.currentTimeMillis()))
        if (list.size > 500) list.removeAt(list.size - 1)
        save(list)
    }
    
    fun clearHistory() = prefs.edit().putString("history", "[]").apply()
    
    private fun save(list: List<HistoryEntry>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("history", array.toString()).apply()
    }
}