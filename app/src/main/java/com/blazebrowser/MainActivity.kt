package com.blazebrowser

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton

    private val homeUrl = "https://www.google.com"

    private var searchEngine: String = "google"
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "BlazeBrowserPrefs"
        private const val KEY_SEARCH_ENGINE = "search_engine"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        searchEngine = prefs.getString(KEY_SEARCH_ENGINE, "google") ?: "google"

        initViews()
        setupWebView()
        setupListeners()
        loadUrl(homeUrl)
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                urlBar.setText(url)
                btnBack.isEnabled = webView.canGoBack()
                btnForward.isEnabled = webView.canGoForward()
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
            }
        }
    }

    private fun setupListeners() {
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == KeyEvent.KEYCODE_ENTER) {
                val url = urlBar.text.toString().trim()
                loadUrl(url)
                urlBar.clearFocus()
                true
            } else {
                false
            }
        }

        btnBack.setOnClickListener { webView.goBack() }
        btnForward.setOnClickListener { webView.goForward() }
        btnRefresh.setOnClickListener { webView.reload() }
        btnHome.setOnClickListener { loadUrl(homeUrl) }
    }

    private fun loadUrl(input: String) {
        var url = input.trim()
        if (url.isEmpty()) return

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://$url"
            } else {
                url = buildSearchUrl(url)
            }
        }
        webView.loadUrl(url)
        progressBar.visibility = View.VISIBLE
    }

    private fun buildSearchUrl(query: String): String {
        val encodedQuery = query.replace(" ", "+")
        return when (searchEngine) {
            "duckduckgo" -> "https://duckduckgo.com/?q=$encodedQuery"
            "bing" -> "https://www.bing.com/search?q=$encodedQuery"
            "brave" -> "https://search.brave.com/search?q=$encodedQuery"
            "startpage" -> "https://www.startpage.com/sp/search?q=$encodedQuery"
            "kagi" -> "https://kagi.com/search?q=$encodedQuery"
            else -> "https://www.google.com/search?q=$encodedQuery"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        // Set the correct search engine as checked
        val searchEngineItem = menu?.findItem(R.id.menuSearchEngine)
        val subMenu = searchEngineItem?.subMenu
        val checkedId = when (searchEngine) {
            "duckduckgo" -> R.id.searchDuckDuckGo
            "bing" -> R.id.searchBing
            "brave" -> R.id.searchBrave
            "startpage" -> R.id.searchStartpage
            "kagi" -> R.id.searchKagi
            else -> R.id.searchGoogle
        }
        subMenu?.findItem(checkedId)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.searchGoogle -> {
                setSearchEngine("google")
                item.isChecked = true
                true
            }
            R.id.searchDuckDuckGo -> {
                setSearchEngine("duckduckgo")
                item.isChecked = true
                true
            }
            R.id.searchBing -> {
                setSearchEngine("bing")
                item.isChecked = true
                true
            }
            R.id.searchBrave -> {
                setSearchEngine("brave")
                item.isChecked = true
                true
            }
            R.id.searchStartpage -> {
                setSearchEngine("startpage")
                item.isChecked = true
                true
            }
            R.id.searchKagi -> {
                setSearchEngine("kagi")
                item.isChecked = true
                true
            }
            R.id.menuSettings -> {
                // Placeholder for future settings
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setSearchEngine(engine: String) {
        searchEngine = engine
        prefs.edit().putString(KEY_SEARCH_ENGINE, engine).apply()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}