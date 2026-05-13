package com.privabrowser

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.privabrowser.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupSearchBar()
        setupShortcuts()
        setupMenuButtons()
    }

    private fun setupSearchBar() {
        binding.homeSearchBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                launchBrowser(binding.homeSearchBar.text.toString())
                true
            } else false
        }
    }

    private fun setupMenuButtons() {
        // Top-right menu/edit button
        binding.btnMenuHome.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("📋 Playlist")
            popup.menu.add("🕶 Private Mode: ON")
            popup.menu.add("⚙ Settings")
            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "📋 Playlist" -> startActivity(Intent(this, PlaylistActivity::class.java))
                    "🕶 Private Mode: ON" -> Toast.makeText(this, "Private mode is always ON", Toast.LENGTH_SHORT).show()
                    "⚙ Settings" -> Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popup.show()
        }

        // Bottom 3-dot overflow button
        binding.btnHomeOverflow.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("📋 Playlist")
            popup.menu.add("🧹 Clear Data")
            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "📋 Playlist" -> startActivity(Intent(this, PlaylistActivity::class.java))
                    "🧹 Clear Data" -> {
                        CookieManager.getInstance().removeAllCookies(null)
                        WebStorage.getInstance().deleteAllData()
                        Toast.makeText(this, "🧹 Data cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun setupShortcuts() {
        binding.shortcutGoogle.setOnClickListener {
            launchBrowser("https://www.google.com")
        }
        binding.shortcutDdg.setOnClickListener {
            launchBrowser("https://duckduckgo.com")
        }
        binding.shortcutYoutube.setOnClickListener {
            launchBrowser("https://www.youtube.com")
        }
        binding.shortcutAdd.setOnClickListener {
            Toast.makeText(this, "Long press to add shortcut (coming soon)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchBrowser(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://duckduckgo.com/?q=${android.net.Uri.encode(input)}"
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.homeSearchBar.windowToken, 0)
    }
}
