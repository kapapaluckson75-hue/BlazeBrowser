package com.blazebrowser

import android.content.Context
import android.content.SharedPreferences
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ModeManager {
    private const val PREFS_NAME = "BlazeBrowserPrefs"
    private const val KEY_MODE = "browser_mode"

    const val MODE_NORMAL = 0
    const val MODE_SECRET = 1

    fun getCurrentMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_MODE, MODE_NORMAL)
    }

    fun setMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MODE, mode).apply()
    }

    fun isSecretMode(context: Context): Boolean = getCurrentMode(context) == MODE_SECRET
    fun isNormalMode(context: Context): Boolean = getCurrentMode(context) == MODE_NORMAL
}

object StealthConfig {
    // User agents to rotate through
    val USER_AGENTS = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    )

    // Blocked tracker patterns
    val TRACKER_PATTERNS = listOf(
        "google-analytics.com",
        "doubleclick.net",
        "googletagmanager.com",
        "facebook.com/tr",
        "amazon-adsystem.com",
        "adsystem.amazon.com",
        "tracking.",
        "analytics.",
        "pixel.",
        "ads.",
        "metrics.",
        "telemetry.",
        "fingerprint",
        "hotjar.com",
        "clarity.ms",
        "mixpanel.com",
        "segment.com",
        "amplitude.com"
    )

    fun getRandomUA(): String {
        val random = SecureRandom()
        return USER_AGENTS[random.nextInt(USER_AGENTS.size)]
    }

    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return TRACKER_PATTERNS.any { lowerUrl.contains(it) }
    }

    fun enableProxy(context: Context) {
        // Use WebView's built-in proxy to route through a random Tor-like proxy
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val proxyUrl = "127.0.0.1:9050" // SOCKS proxy - if available
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .addDirect() // Fallback to direct
                .build()
            CoroutineScope(Dispatchers.Main).launch {
                ProxyController.getInstance().setProxyOverride(proxyConfig, {
                    // Proxy set
                }, {
                    // Error handler
                })
            }
        }
    }

    fun disableProxy(context: Context) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            CoroutineScope(Dispatchers.Main).launch {
                ProxyController.getInstance().clearProxyOverride({
                    // Proxy cleared
                }, {
                    // Error handler
                })
            }
        }
    }
}
