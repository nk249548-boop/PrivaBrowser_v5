package com.privabrowser

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.privabrowser.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adBlocker: AdBlocker
    private val detectedVideoUrls = mutableListOf<String>()

    companion object {
        const val HOME_URL = "https://duckduckgo.com"
        const val PERMISSION_STORAGE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adBlocker = AdBlocker(this)
        lifecycleScope.launch { adBlocker.initialize() }

        setupWebView()
        setupToolbar()
        setupBottomBar()

        val startUrl = intent.getStringExtra("url") ?: HOME_URL
        binding.webView.loadUrl(startUrl)
    }

    // ─────────────────────────────────────────
    // WEBVIEW SETUP
    // ─────────────────────────────────────────
    private fun setupWebView() {
        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
            // Privacy: disable save form data (suppressed deprecation - still functional on API 26-29)
            @Suppress("DEPRECATION")
            saveFormData = false
            @Suppress("DEPRECATION")
            savePassword = false
            // Geolocation off
            setGeolocationEnabled(false)
        }

        // Block third-party cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, false)
        }

        binding.webView.webViewClient = PrivaWebViewClient()
        binding.webView.webChromeClient = PrivaWebChromeClient()

        // Video URL detection via JavaScript interface
        binding.webView.addJavascriptInterface(VideoDetector(), "VideoDetector")

        // Download listener for direct file downloads
        binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            handleDownload(url, mimeType, contentDisposition)
        }
    }

    // ─────────────────────────────────────────
    // AD BLOCKER - WebViewClient
    // ─────────────────────────────────────────
    inner class PrivaWebViewClient : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()

            // Check ad blocker
            if (adBlocker.shouldBlock(url)) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }

            // Detect video URLs
            detectVideoUrl(url)

            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility = View.VISIBLE
            binding.urlBar.setText(url)
            detectedVideoUrls.clear()
            binding.btnDownload.visibility = View.GONE
            injectVideoDetectionScript(view)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            binding.progressBar.visibility = View.GONE
            binding.urlBar.setText(url)
            injectVideoDetectionScript(view)
        }

        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
            if (request.isForMainFrame) {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────
    // CHROME CLIENT (Progress, Title)
    // ─────────────────────────────────────────
    inner class PrivaWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.progressBar.progress = newProgress
        }
        override fun onReceivedTitle(view: WebView, title: String) {
            supportActionBar?.title = title
        }
    }

    // ─────────────────────────────────────────
    // VIDEO DETECTION
    // ─────────────────────────────────────────
    private fun detectVideoUrl(url: String) {
        val videoExtensions = listOf(".mp4", ".m3u8", ".mkv", ".webm", ".avi", ".mov", ".ts")
        if (videoExtensions.any { url.contains(it, ignoreCase = true) } &&
            !detectedVideoUrls.contains(url)) {
            detectedVideoUrls.add(url)
            runOnUiThread {
                binding.btnDownload.visibility = View.VISIBLE
                // video count shown in toast
            }
        }
    }

    private fun injectVideoDetectionScript(view: WebView) {
        val script = """
            (function() {
                function notifyVideo(url) {
                    if (url && (url.includes('.mp4') || url.includes('.m3u8') || 
                        url.includes('.webm') || url.includes('.mkv'))) {
                        VideoDetector.onVideoFound(url);
                    }
                }
                // Check existing video/source elements
                document.querySelectorAll('video, source').forEach(el => {
                    if (el.src) notifyVideo(el.src);
                    if (el.currentSrc) notifyVideo(el.currentSrc);
                });
                // Observe DOM for new video elements
                const observer = new MutationObserver(mutations => {
                    mutations.forEach(m => m.addedNodes.forEach(node => {
                        if (node.tagName === 'VIDEO' || node.tagName === 'SOURCE') {
                            notifyVideo(node.src || node.currentSrc);
                        }
                    }));
                });
                observer.observe(document.body || document.documentElement, 
                    {childList: true, subtree: true});
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    inner class VideoDetector {
        @JavascriptInterface
        fun onVideoFound(url: String) {
            runOnUiThread { detectVideoUrl(url) }
        }
    }

    // ─────────────────────────────────────────
    // DOWNLOAD HANDLER
    // ─────────────────────────────────────────
    private fun handleDownload(url: String, mimeType: String = "video/mp4", contentDisposition: String = "") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_STORAGE)
                return
            }
        }

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading via PrivaBrowser")
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PrivaBrowser/$fileName")
            addRequestHeader("User-Agent", binding.webView.settings.userAgentString)
            addRequestHeader("Referer", binding.webView.url ?: "")
        }

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // Save to playlist DB
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.videoDao().insert(
                VideoEntity(
                    title = fileName,
                    url = url,
                    localPath = "${Environment.DIRECTORY_DOWNLOADS}/PrivaBrowser/$fileName",
                    downloadId = downloadId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        Toast.makeText(this, "⬇ Downloading: $fileName", Toast.LENGTH_LONG).show()
    }

    private fun showVideoDownloadDialog() {
        if (detectedVideoUrls.isEmpty()) {
            Toast.makeText(this, "No video found on this page", Toast.LENGTH_SHORT).show()
            return
        }
        // Show picker if multiple videos found
        val titles = detectedVideoUrls.mapIndexed { i, url ->
            "Video ${i + 1}: ...${url.takeLast(40)}"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Download Video")
            .setItems(titles) { _, which ->
                handleDownload(detectedVideoUrls[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─────────────────────────────────────────
    // TOOLBAR SETUP
    // ─────────────────────────────────────────
    private fun setupToolbar() {
        binding.urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                navigateTo(binding.urlBar.text.toString())
                true
            } else false
        }

        binding.btnDownload.setOnClickListener {
            showVideoDownloadDialog()
        }

        binding.btnPlaylist.setOnClickListener {
            startActivity(Intent(this, PlaylistActivity::class.java))
        }

        // Overflow menu (3 dots)
        binding.btnOverflow.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menu.add("🏠 Home")
            popup.menu.add("🔄 Refresh")
            popup.menu.add("🧹 Clear Data")
            popup.menu.add("📋 Playlist")
            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "🏠 Home" -> startActivity(Intent(this, HomeActivity::class.java))
                    "🔄 Refresh" -> binding.webView.reload()
                    "🧹 Clear Data" -> clearBrowsingData()
                    "📋 Playlist" -> startActivity(Intent(this, PlaylistActivity::class.java))
                }
                true
            }
            popup.show()
        }
    }

    private fun setupBottomBar() {
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
    }

    private fun navigateTo(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://duckduckgo.com/?q=${Uri.encode(input)}"
        }
        binding.webView.loadUrl(url)
        hideKeyboard()
    }

    // ─────────────────────────────────────────
    // PRIVACY: Clear on exit
    // ─────────────────────────────────────────
    private fun clearBrowsingData() {
        binding.webView.clearCache(true)
        binding.webView.clearHistory()
        binding.webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        Toast.makeText(this, "🧹 Browsing data cleared", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        // Auto-clear on background (privacy mode)
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("private_mode", true)) {
            clearBrowsingData()
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.urlBar.windowToken, 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_STORAGE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted. Try downloading again.", Toast.LENGTH_SHORT).show()
        }
    }
}
