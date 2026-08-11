package com.blazebrowser.data

import android.content.Context
import android.content.SharedPreferences

class ModeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_mode", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_MODE = "current_mode"
        const val MODE_NORMAL = "normal"
        const val MODE_SECRET = "secret"
    }
    
    fun getCurrentMode(): String = prefs.getString(KEY_MODE, MODE_NORMAL) ?: MODE_NORMAL
    fun setMode(mode: String) = prefs.edit().putString(KEY_MODE, mode).apply()
    fun isSecret(): Boolean = getCurrentMode() == MODE_SECRET
    fun isNormal(): Boolean = getCurrentMode() == MODE_NORMAL
}