package com.blazebrowser.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.blazebrowser.ModeManager
import com.blazebrowser.StealthConfig

class WebViewFragment : Fragment() {

    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var initialUrl: String = ""
    private var mode: Int = ModeManager.MODE_NORMAL

    companion object {
        fun newInstance(url: String, mode: Int): WebViewFragment {
            val fragment = WebViewFragment()
            val args = Bundle()
            args.putString("url", url)
            args.putInt("mode", mode)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialUrl = it.getString("url", "https://www.google.com")
            mode = it.getInt("mode", ModeManager.MODE_NORMAL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_webview, container, false)

        val webView = view.findViewById<WebView>(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)

        this.webView = webView

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = if (mode == ModeManager.MODE_SECRET) {
            WebSettings.LOAD_NO_CACHE
        } else {
            WebSettings.LOAD_DEFAULT
        }
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        if (mode == ModeManager.MODE_SECRET) {
            settings.userAgentString = StealthConfig.getRandomUA()
            settings.setGeolocationEnabled(false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (mode == ModeManager.MODE_SECRET && StealthConfig.shouldBlock(url)) {
                    return true
                }
                view?.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar?.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                progressBar?.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar?.progress = newProgress
                progressBar?.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else if (initialUrl.isNotEmpty()) {
            webView.loadUrl(initialUrl)
        }

        return view
    }

    fun getWebView(): WebView? = webView

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }
}
