package com.blazebrowser.network

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebSettings
import android.webkit.WebView

class NetworkMaskManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blaze_network_mask", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ENABLED = "masking_enabled"
        private const val KEY_SPOOF_UA = "spoof_user_agent"
        private const val KEY_BLOCK_TRACKERS = "block_trackers"
        private const val KEY_BLOCK_ADS = "block_ads"
        private const val KEY_DISABLE_JS = "disable_javascript"
        private const val KEY_DISABLE_IMAGES = "disable_images"
        private const val KEY_PROXY_ENABLED = "proxy_enabled"
        private const val KEY_PROXY_HOST = "proxy_host"
        private const val KEY_PROXY_PORT = "proxy_port"
    }
    
    fun isMaskingEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun setMaskingEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    
    fun isSpoofUserAgent(): Boolean = prefs.getBoolean(KEY_SPOOF_UA, false)
    fun setSpoofUserAgent(enabled: Boolean) = prefs.edit().putBoolean(KEY_SPOOF_UA, enabled).apply()
    
    fun isBlockTrackers(): Boolean = prefs.getBoolean(KEY_BLOCK_TRACKERS, false)
    fun setBlockTrackers(enabled: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_TRACKERS, enabled).apply()
    
    fun isBlockAds(): Boolean = prefs.getBoolean(KEY_BLOCK_ADS, false)
    fun setBlockAds(enabled: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_ADS, enabled).apply()
    
    fun isDisableJavaScript(): Boolean = prefs.getBoolean(KEY_DISABLE_JS, false)
    fun setDisableJavaScript(enabled: Boolean) = prefs.edit().putBoolean(KEY_DISABLE_JS, enabled).apply()
    
    fun isDisableImages(): Boolean = prefs.getBoolean(KEY_DISABLE_IMAGES, false)
    fun setDisableImages(enabled: Boolean) = prefs.edit().putBoolean(KEY_DISABLE_IMAGES, enabled).apply()
    
    fun isProxyEnabled(): Boolean = prefs.getBoolean(KEY_PROXY_ENABLED, false)
    fun setProxyEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PROXY_ENABLED, enabled).apply()
    
    fun getProxyHost(): String = prefs.getString(KEY_PROXY_HOST, "") ?: ""
    fun setProxyHost(host: String) = prefs.edit().putString(KEY_PROXY_HOST, host).apply()
    
    fun getProxyPort(): Int = prefs.getInt(KEY_PROXY_PORT, 0)
    fun setProxyPort(port: Int) = prefs.edit().putInt(KEY_PROXY_PORT, port).apply()
    
    fun applyToWebView(webView: WebView) {
        val settings = webView.settings
        
        if (isSpoofUserAgent()) {
            settings.userAgentString = getRandomUserAgent()
        }
        
        settings.javaScriptEnabled = !isDisableJavaScript()
        
        if (isDisableImages()) {
            settings.blockNetworkImage = true
            settings.loadsImagesAutomatically = false
        } else {
            settings.blockNetworkImage = false
            settings.loadsImagesAutomatically = true
        }
        
        // Disable storage for privacy
        if (isMaskingEnabled()) {
            settings.domStorageEnabled = false
            settings.databaseEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
        } else {
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }
    
    private fun getRandomUserAgent(): String {
        val userAgents = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
        )
        return userAgents.random()
    }
    
    fun getTrackerBlockList(): Set<String> {
        return setOf(
            "google-analytics.com",
            "googletagmanager.com",
            "doubleclick.net",
            "facebook.net",
            "connect.facebook.net",
            "analytics.google.com",
            "ads.google.com",
            "googleadservices.com",
            "amazon-adsystem.com",
            "adsystem.amazon.com",
            "adservice.google.com",
            "adservice.google.co.uk",
            "analytics.twitter.com",
            "static.ads-twitter.com",
            "ads.linkedin.com",
            "analytics.pointdrive.linkedin.com",
            "ads.pinterest.com",
            "log.pinterest.com",
            "ads.reddit.com",
            "ads.youtube.com",
            "ads.yahoo.com",
            "analytics.yahoo.com",
            "bat.bing.com",
            "clarity.ms",
            "hotjar.com",
            "cdn.segment.com",
            "api.segment.io",
            "mixpanel.com",
            "cdn.mxpnl.com",
            "newrelic.com",
            "bam.nr-data.net"
        )
    }
    
    fun getAdBlockList(): Set<String> {
        return setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "ads.google.com",
            "pagead2.googlesyndication.com",
            "tpc.googlesyndication.com",
            "ad.doubleclick.net",
            "static.doubleclick.net",
            "m.doubleclick.net",
            "mediavisor.doubleclick.net",
            "amazon-adsystem.com",
            "adsystem.amazon.com",
            "aax.amazon-adsystem.com",
            "c.amazon-adsystem.com",
            "ads.yahoo.com",
            "analytics.yahoo.com",
            "ads.twitter.com",
            "static.ads-twitter.com",
            "ads.linkedin.com",
            "ads.pinterest.com",
            "ads.reddit.com",
            "ads.youtube.com",
            "adsense.google.com",
            "adtago.s3.amazonaws.com",
            "advice-ads.s3.amazonaws.com",
            "advertising-api-eu.amazon.com",
            "c.amazon-adsystem.com",
            "cdn.accelerator.affiliates",
            "config.uca.amazon-adsystem.com"
        )
    }
    
    fun isBlockedUrl(url: String): Boolean {
        if (!isMaskingEnabled()) return false
        
        val blockedHosts = mutableSetOf<String>()
        if (isBlockTrackers()) blockedHosts.addAll(getTrackerBlockList())
        if (isBlockAds()) blockedHosts.addAll(getAdBlockList())
        
        return blockedHosts.any { url.contains(it, ignoreCase = true) }
    }
}