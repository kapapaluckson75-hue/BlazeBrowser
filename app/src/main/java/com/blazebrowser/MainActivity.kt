package com.blazebrowser

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blazebrowser.ai.AiConfig
import com.blazebrowser.data.Bookmark
import com.blazebrowser.data.BookmarkManager
import com.blazebrowser.data.HistoryEntry
import com.blazebrowser.data.HistoryManager
import com.blazebrowser.data.ModeManager
import com.blazebrowser.data.SearchEngineManager
import com.blazebrowser.network.NetworkMaskManager
import com.blazebrowser.network.TempMailManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnTab: ImageButton
    private lateinit var fabNewTab: FloatingActionButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar

    private lateinit var modeManager: ModeManager
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var historyManager: HistoryManager
    private lateinit var searchEngineManager: SearchEngineManager
    private lateinit var networkMaskManager: NetworkMaskManager
    private lateinit var tempMailManager: TempMailManager
    private lateinit var aiConfig: AiConfig

    private val tabs = mutableListOf<TabData>()
    private var currentTabId = -1
    private var nextTabId = 0
    private var findInPageQuery: String? = null
    private val handler = Handler(Looper.getMainLooper())

    data class TabData(
        val id: Int,
        var webView: WebView?,
        var title: String,
        var url: String,
        var tabName: String
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        modeManager = ModeManager(this)
        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)
        searchEngineManager = SearchEngineManager(this)
        networkMaskManager = NetworkMaskManager(this)
        tempMailManager = TempMailManager(this)
        aiConfig = AiConfig(this)

        initViews()
        setupListeners()
        createNewTab("https://www.google.com", true)
    }

    private fun initViews() {
        webViewContainer = findViewById(R.id.webViewContainer)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnTab = findViewById(R.id.btnTab)
        fabNewTab = findViewById(R.id.fabNewTab)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupListeners() {
        urlBar.setOnClickListener { urlBar.selectAll() }

        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.KEYCODE_ENTER) {
                loadUrl(urlBar.text.toString().trim())
                urlBar.clearFocus()
                hideKeyboard()
                true
            } else false
        }

        btnBack.setOnClickListener { currentWebView()?.goBack() }
        btnForward.setOnClickListener { currentWebView()?.goForward() }
        btnRefresh.setOnClickListener { currentWebView()?.reload() }
        btnHome.setOnClickListener { loadUrl("https://www.google.com") }
        btnTab.setOnClickListener { showTabsDialog() }
        fabNewTab.setOnClickListener { createNewTab("https://www.google.com", true) }

        swipeRefresh.setOnRefreshListener {
            currentWebView()?.reload()
            swipeRefresh.isRefreshing = false
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val tabData = tabs.find { it.id == tab.tag as? Int }
                tabData?.let { switchToTab(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun createNewTab(url: String, switchTo: Boolean = false): TabData {
        val webView = WebView(this)
        setupWebViewSettings(webView)

        val tabData = TabData(
            id = nextTabId++,
            webView = webView,
            title = "New Tab",
            url = url,
            tabName = "Tab"
        )
        tabs.add(tabData)

        webView.webViewClient = createWebViewClient(tabData)
        webView.webChromeClient = createWebChromeClient(tabData)

        addTabToLayout(tabData)
        webView.loadUrl(url)

        if (switchTo) switchToTab(tabData)
        return tabData
    }

    private fun addTabToLayout(tabData: TabData) {
        val tab = tabLayout.newTab()
        tab.text = tabData.tabName
        tab.tag = tabData.id
        tabLayout.addTab(tab)
        tabLayout.visibility = View.VISIBLE
    }

    private fun switchToTab(tabData: TabData) {
        currentTabId = tabData.id
        webViewContainer.removeAllViews()
        tabData.webView?.let { webViewContainer.addView(it) }
        urlBar.setText(tabData.url)

        // Update tab selection
        for (i in 0 until tabLayout.tabCount) {
            if (tabLayout.getTabAt(i)?.tag == tabData.id) {
                tabLayout.getTabAt(i)?.select()
                break
            }
        }

        updateNavButtons()
    }

    private fun closeTab(tabData: TabData) {
        tabData.webView?.destroy()
        tabs.remove(tabData)

        // Remove from tab layout
        for (i in tabLayout.tabCount - 1 downTo 0) {
            if (tabLayout.getTabAt(i)?.tag == tabData.id) {
                tabLayout.removeTabAt(i)
                break
            }
        }

        if (tabs.isEmpty()) {
            tabLayout.visibility = View.GONE
            createNewTab("https://www.google.com", true)
        } else if (currentTabId == tabData.id) {
            switchToTab(tabs.last())
        }
    }

    private fun currentWebView(): WebView? = tabs.find { it.id == currentTabId }?.webView

    private fun setupWebViewSettings(webView: WebView) {
        val settings = webView.settings
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
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        networkMaskManager.applyToWebView(webView)
    }

    private fun createWebViewClient(tabData: TabData) = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false

            // Check if blocked in secret mode
            if (networkMaskManager.isMaskingEnabled() && networkMaskManager.isBlockedUrl(url)) {
                return true
            }

            view?.loadUrl(url)
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let {
                tabData.url = it
                if (tabData.id == currentTabId) {
                    urlBar.setText(it)
                    updateNavButtons()
                    progressBar.visibility = View.GONE
                }
            }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
            super.onReceivedError(view, request, error)
            progressBar.visibility = View.GONE
        }
    }

    private fun createWebChromeClient(tabData: TabData) = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (tabData.id == currentTabId) {
                progressBar.progress = newProgress
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let {
                tabData.title = it
                // Update tab text
                for (i in 0 until tabLayout.tabCount) {
                    if (tabLayout.getTabAt(i)?.tag == tabData.id) {
                        tabLayout.getTabAt(i)?.text = it.take(20)
                        break
                    }
                }
            }
        }
    }

    private fun updateNavButtons() {
        val webView = currentWebView() ?: return
        btnBack.isEnabled = webView.canGoBack()
        btnForward.isEnabled = webView.canGoForward()
    }

    private fun loadUrl(input: String) {
        var url = input.trim()
        if (url.isEmpty()) return

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://$url"
            } else {
                val engine = searchEngineManager.getDefaultEngine()
                url = searchEngineManager.buildSearchUrl(engine, url)
            }
        }
        currentWebView()?.loadUrl(url)
        progressBar.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    // ==================== MENU ====================

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Update checkboxes based on mode
        val modeItem = menu?.findItem(R.id.menuMode)
        modeItem?.setTitle(if (modeManager.isSecret()) "Mode: Secret" else "Mode: Normal")

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMode -> {
                toggleMode()
                true
            }
            R.id.menuSearchEngine -> {
                showSearchEngineDialog()
                true
            }
            R.id.menuBookmarks -> {
                showBookmarksDialog()
                true
            }
            R.id.menuHistory -> {
                showHistoryDialog()
                true
            }
            R.id.menuAddBookmark -> {
                currentWebView()?.let {
                    bookmarkManager.addBookmark(it.title ?: "", it.url ?: "")
                    Toast.makeText(this, "Bookmarked", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menuFind -> {
                showFindInPageDialog()
                true
            }
            R.id.menuTempMail -> {
                startActivity(Intent(this, TempMailActivity::class.java))
                true
            }
            R.id.menuNetworkSettings -> {
                showNetworkSettingsDialog()
                true
            }
            R.id.menuBlazeAI -> {
                if (!aiConfig.isConfigured()) {
                    startActivity(Intent(this, AiConfigActivity::class.java))
                } else {
                    startActivity(Intent(this, AiAssistantActivity::class.java))
                }
                true
            }
            R.id.menuAddSearchEngine -> {
                showAddSearchEngineDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleMode() {
        val newMode = if (modeManager.isNormal()) ModeManager.MODE_SECRET else ModeManager.MODE_NORMAL
        modeManager.setMode(newMode)

        // Reconfigure all WebViews
        tabs.forEach { tabData ->
            tabData.webView?.let { networkMaskManager.applyToWebView(it) }
        }

        recreate()
    }

    private fun showSearchEngineDialog() {
        val engines = searchEngineManager.getAllEngines()
        val names = engines.map { it.name }.toTypedArray()
        val currentDefault = searchEngineManager.getDefaultEngine()

        AlertDialog.Builder(this)
            .setTitle("Select Search Engine")
            .setSingleChoiceItems(names, names.indexOf(currentDefault)) { dialog, which ->
                searchEngineManager.setDefaultEngine(names[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddSearchEngineDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        val nameInput = EditText(this).apply { hint = "Engine name" }
        val urlInput = EditText(this).apply { hint = "URL template (use {query})" }
        layout.addView(nameInput)
        layout.addView(urlInput)

        AlertDialog.Builder(this)
            .setTitle("Add Custom Search Engine")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                val url = urlInput.text.toString()
                if (name.isNotBlank() && url.contains("{query}")) {
                    searchEngineManager.addCustomEngine(name, url)
                    Toast.makeText(this, "Added $name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBookmarksDialog() {
        val bookmarks = bookmarkManager.getBookmarks()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "No bookmarks yet", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = BookmarkAdapter(bookmarks,
            onClick = { loadUrl(it.url); (this@MainActivity).finish() },
            onLongClick = { bookmarkManager.removeBookmark(it.url); Toast.makeText(this@MainActivity, "Removed", Toast.LENGTH_SHORT).show() }
        )
        layout.addView(recyclerView)

        AlertDialog.Builder(this)
            .setTitle("Bookmarks")
            .setView(layout)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showHistoryDialog() {
        val history = historyManager.getHistory()
        if (history.isEmpty()) {
            Toast.makeText(this, "No history", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HistoryAdapter(history,
            onClick = { loadUrl(it.url) },
            onLongClick = { historyManager.clearHistory(); Toast.makeText(this@MainActivity, "Cleared", Toast.LENGTH_SHORT).show() }
        )
        layout.addView(recyclerView)

        AlertDialog.Builder(this)
            .setTitle("History")
            .setView(layout)
            .setNegativeButton("Clear") { _, _ -> historyManager.clearHistory() }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showFindInPageDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 16, 48, 16)
        }
        val input = EditText(this).apply {
            hint = "Find in page"
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Find in Page")
            .setView(layout)
            .setPositiveButton("Find") { _, _ ->
                val query = input.text.toString()
                findInPageQuery = query
                if (query.isNotEmpty()) {
                    currentWebView()?.findAllAsync(query)
                }
            }
            .setNegativeButton("Close") { _, _ ->
                currentWebView()?.clearMatches()
            }
            .show()
    }

    private fun showNetworkSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_network_settings, null)

        val checkMasking = view.findViewById<android.widget.CheckBox>(R.id.checkMasking)
        val checkSpoofUA = view.findViewById<android.widget.CheckBox>(R.id.checkSpoofUA)
        val checkBlockTrackers = view.findViewById<android.widget.CheckBox>(R.id.checkBlockTrackers)
        val checkBlockAds = view.findViewById<android.widget.CheckBox>(R.id.checkBlockAds)
        val checkDisableJS = view.findViewById<android.widget.CheckBox>(R.id.checkDisableJS)
        val checkDisableImages = view.findViewById<android.widget.CheckBox>(R.id.checkDisableImages)
        val editProxyHost = view.findViewById<EditText>(R.id.editProxyHost)
        val editProxyPort = view.findViewById<EditText>(R.id.editProxyPort)
        val checkProxy = view.findViewById<android.widget.CheckBox>(R.id.checkProxy)

        // Load current values
        checkMasking.isChecked = networkMaskManager.isMaskingEnabled()
        checkSpoofUA.isChecked = networkMaskManager.isSpoofUserAgent()
        checkBlockTrackers.isChecked = networkMaskManager.isBlockTrackers()
        checkBlockAds.isChecked = networkMaskManager.isBlockAds()
        checkDisableJS.isChecked = networkMaskManager.isDisableJavaScript()
        checkDisableImages.isChecked = networkMaskManager.isDisableImages()
        checkProxy.isChecked = networkMaskManager.isProxyEnabled()
        editProxyHost.setText(networkMaskManager.getProxyHost())
        editProxyPort.setText(networkMaskManager.getProxyPort().toString())

        AlertDialog.Builder(this)
            .setTitle("Network Masking")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                networkMaskManager.setMaskingEnabled(checkMasking.isChecked)
                networkMaskManager.setSpoofUserAgent(checkSpoofUA.isChecked)
                networkMaskManager.setBlockTrackers(checkBlockTrackers.isChecked)
                networkMaskManager.setBlockAds(checkBlockAds.isChecked)
                networkMaskManager.setDisableJavaScript(checkDisableJS.isChecked)
                networkMaskManager.setDisableImages(checkDisableImages.isChecked)
                networkMaskManager.setProxyEnabled(checkProxy.isChecked)
                networkMaskManager.setProxyHost(editProxyHost.text.toString())
                networkMaskManager.setProxyPort(editProxyPort.text.toString().toIntOrNull() ?: 0)

                // Reconfigure all WebViews
                tabs.forEach { it.webView?.let { wv -> networkMaskManager.applyToWebView(wv) } }
                Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTabsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TabListAdapter(tabs, currentTabId,
            onClick = { switchToTab(it) },
            onClose = { closeTab(it) }
        )
        layout.addView(recyclerView)

        AlertDialog.Builder(this)
            .setTitle("Tabs (${tabs.size})")
            .setView(layout)
            .setPositiveButton("New Tab") { _, _ -> createNewTab("https://www.google.com", true) }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==================== ADAPTERS ====================

    inner class BookmarkAdapter(
        private val items: List<Bookmark>,
        private val onClick: (Bookmark) -> Unit,
        private val onLongClick: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val url: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.url.text = item.url
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size
    }

    inner class HistoryAdapter(
        private val items: List<HistoryEntry>,
        private val onClick: (HistoryEntry) -> Unit,
        private val onLongClick: (HistoryEntry) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val url: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.url.text = item.url
            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size
    }

    inner class TabListAdapter(
        private val items: List<TabData>,
        private val currentId: Int,
        private val onClick: (TabData) -> Unit,
        private val onClose: (TabData) -> Unit
    ) : RecyclerView.Adapter<TabListAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tabTitle)
            val closeBtn: ImageButton = view.findViewById(R.id.btnCloseTab)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = "${item.title} ${if (item.id == currentId) "●" else ""}"
            holder.itemView.setOnClickListener { onClick(item) }
            holder.closeBtn.setOnClickListener { onClose(item) }
        }

        override fun getItemCount() = items.size
    }

    // ==================== LIFECYCLE ====================

    override fun onBackPressed() {
        val webView = currentWebView()
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        tabs.forEach { it.webView?.destroy() }
        super.onDestroy()
    }
}