package com.blazebrowser.data

import android.content.Context
import android.content.SharedPreferences

class SearchEngineManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_engines", Context.MODE_PRIVATE)
    
    data class SearchEngine(val name: String, val urlTemplate: String, val isBuiltIn: Boolean = false)
    
    private val builtInEngines = listOf(
        SearchEngine("Google", "https://www.google.com/search?q={query}", true),
        SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q={query}", true),
        SearchEngine("Bing", "https://www.bing.com/search?q={query}", true),
        SearchEngine("Brave", "https://search.brave.com/search?q={query}", true),
        SearchEngine("Startpage", "https://www.startpage.com/sp/search?q={query}", true),
        SearchEngine("Kagi", "https://kagi.com/search?q={query}", true)
    )
    
    fun getAllEngines(): List<SearchEngine> {
        val customJson = prefs.getString("custom_engines", "[]") ?: "[]"
        val custom = parseCustomEngines(customJson)
        return builtInEngines + custom
    }
    
    fun getEngine(name: String): SearchEngine? = getAllEngines().find { it.name.equals(name, true) }
    
    fun getDefaultEngine(): String = prefs.getString("default_engine", "Google") ?: "Google"
    
    fun setDefaultEngine(name: String) = prefs.edit().putString("default_engine", name).apply()
    
    fun addCustomEngine(name: String, urlTemplate: String) {
        if (!urlTemplate.contains("{query}")) return
        val custom = parseCustomEngines(prefs.getString("custom_engines", "[]") ?: "[]").toMutableList()
        custom.add(SearchEngine(name, urlTemplate, false))
        saveCustomEngines(custom)
    }
    
    private fun parseCustomEngines(json: String): List<SearchEngine> {
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                SearchEngine(obj.getString("name"), obj.getString("urlTemplate"), false)
            }
        } catch (e: Exception) { emptyList() }
    }
    
    private fun saveCustomEngines(list: List<SearchEngine>) {
        val array = org.json.JSONArray()
        list.filter { !it.isBuiltIn }.forEach {
            val obj = org.json.JSONObject()
            obj.put("name", it.name)
            obj.put("urlTemplate", it.urlTemplate)
            array.put(obj)
        }
        prefs.edit().putString("custom_engines", array.toString()).apply()
    }
    
    fun buildSearchUrl(engineName: String, query: String): String {
        val engine = getEngine(engineName) ?: builtInEngines.first()
        return engine.urlTemplate.replace("{query}", query.replace(" ", "+"))
    }
}