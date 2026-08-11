package com.blazebrowser.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val title: String, val url: String, val timestamp: Long)

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_bookmarks", Context.MODE_PRIVATE)
    
    fun getBookmarks(): List<Bookmark> {
        val json = prefs.getString("bookmarks", "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map {
            val obj = array.getJSONObject(it)
            Bookmark(obj.getString("title"), obj.getString("url"), obj.getLong("timestamp"))
        }
    }
    
    fun addBookmark(title: String, url: String) {
        val list = getBookmarks().toMutableList()
        if (list.none { it.url == url }) {
            list.add(0, Bookmark(title, url, System.currentTimeMillis()))
            save(list)
        }
    }
    
    fun removeBookmark(url: String) {
        save(getBookmarks().filter { it.url != url })
    }
    
    fun isBookmarked(url: String): Boolean = getBookmarks().any { it.url == url }
    
    private fun save(list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("title", it.title)
            obj.put("url", it.url)
            obj.put("timestamp", it.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("bookmarks", array.toString()).apply()
    }
}