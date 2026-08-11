package com.blazebrowser.ai

import android.content.Context
import android.content.SharedPreferences

class AiConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_ai", Context.MODE_PRIVATE)

    var providerId: String
        get() = prefs.getString("provider_id", "anthropic") ?: "anthropic"
        set(value) = prefs.edit().putString("provider_id", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var customEndpoint: String
        get() = prefs.getString("custom_endpoint", "") ?: ""
        set(value) = prefs.edit().putString("custom_endpoint", value).apply()

    var model: String
        get() = prefs.getString("model", "") ?: ""
        set(value) = prefs.edit().putString("model", value).apply()

    fun getEndpoint(): String {
        return if (providerId == "custom") {
            customEndpoint
        } else {
            AiProviders.providers.find { it.id == providerId }?.endpoint ?: ""
        }
    }

    fun getDefaultModel(): String {
        return AiProviders.providers.find { it.id == providerId }?.defaultModel ?: ""
    }

    fun getEffectiveModel(): String {
        return if (model.isNotBlank()) model else getDefaultModel()
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && (providerId != "custom" || customEndpoint.isNotBlank())
    }
}
