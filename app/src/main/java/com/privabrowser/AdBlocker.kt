package com.privabrowser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * AdBlocker - Dual-layer ad blocking:
 * 1. Static hosts file (bundled in assets)
 * 2. EasyList filter rules (downloaded + cached)
 */
class AdBlocker(private val context: Context) {

    private val blockedHosts = HashSet<String>(50000)
    private val filterRules = mutableListOf<FilterRule>()
    private val whitelistRules = mutableListOf<FilterRule>()

    // Known ad/tracker domains (inline fallback)
    private val hardcodedBlockList = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adnxs.com", "adsrvr.org", "advertising.com", "outbrain.com",
        "taboola.com", "scorecardresearch.com", "quantserve.com",
        "moatads.com", "criteo.com", "pubmatic.com", "rubiconproject.com",
        "openx.net", "casalemedia.com", "contextweb.com", "lijit.com",
        "sharethrough.com", "sovrn.com", "triplelift.com", "indexexchange.com",
        "amazon-adsystem.com", "ads.yahoo.com", "ads.twitter.com",
        "analytics.google.com", "googletagmanager.com", "hotjar.com",
        "mixpanel.com", "segment.io", "amplitude.com", "fullstory.com",
        "mouseflow.com", "crazyegg.com", "optimizely.com",
        "facebook.com/tr", "connect.facebook.net", "ads.facebook.com",
        "pixel.facebook.com", "mc.yandex.ru", "counter.mail.ru",
        "tracker.gg", "sp.analytics.yahoo.com", "analytics.yahoo.com",
        "ads2.msads.net", "ads1.msads.net", "adserver.yahoo.com",
        "pagead2.googlesyndication.com", "tpc.googlesyndication.com"
    )

    data class FilterRule(
        val pattern: Regex,
        val isException: Boolean = false,
        val domains: List<String> = emptyList()
    )

    suspend fun initialize() = withContext(Dispatchers.IO) {
        loadBuiltinHosts()
        loadEasyListFromAssets()
        Log.d("AdBlocker", "Initialized: ${blockedHosts.size} hosts, ${filterRules.size} filter rules")
    }

    /**
     * Load built-in hosts file from assets/hosts.txt
     * Format: 0.0.0.0 domain.com
     */
    private fun loadBuiltinHosts() {
        try {
            context.assets.open("hosts.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    val cleaned = line.trim()
                    if (cleaned.isNotEmpty() && !cleaned.startsWith("#")) {
                        val parts = cleaned.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            blockedHosts.add(parts[1].lowercase())
                        } else if (parts.size == 1 && parts[0].contains(".")) {
                            blockedHosts.add(parts[0].lowercase())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AdBlocker", "No assets/hosts.txt found, using hardcoded list")
        }
        // Add hardcoded list
        blockedHosts.addAll(hardcodedBlockList)
    }

    /**
     * Load EasyList filter rules from assets/easylist.txt
     * Supports basic ABP syntax: ||domain^, @@exception, /pattern/
     */
    private fun loadEasyListFromAssets() {
        try {
            context.assets.open("easylist.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    parseFilterRule(line)?.let { rule ->
                        if (rule.isException) whitelistRules.add(rule)
                        else filterRules.add(rule)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AdBlocker", "No easylist.txt in assets, using hosts-only blocking")
        }
    }

    private fun parseFilterRule(line: String): FilterRule? {
        val trimmed = line.trim()
        return when {
            trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") -> null
            trimmed.startsWith("##") || trimmed.startsWith("#@#") -> null // CSS rules, skip
            trimmed.startsWith("@@") -> {
                // Exception/whitelist rule
                buildRule(trimmed.substring(2), isException = true)
            }
            else -> buildRule(trimmed, isException = false)
        }
    }

    private fun buildRule(pattern: String, isException: Boolean): FilterRule? {
        return try {
            // Remove options (everything after $)
            val cleanPattern = if (pattern.contains("$")) {
                pattern.substring(0, pattern.lastIndexOf("$"))
            } else pattern

            val regexStr = cleanPattern
                .replace("||", "")           // Domain anchor
                .replace("^", "")            // Separator placeholder
                .replace(".", "\\.")          // Escape dots
                .replace("*", ".*")           // Wildcard
                .replace("?", "\\?")          // Escape ?

            if (regexStr.length < 3) return null

            FilterRule(
                pattern = Regex(regexStr, RegexOption.IGNORE_CASE),
                isException = isException
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Main blocking check - call this for every network request
     */
    fun shouldBlock(url: String): Boolean {
        if (url.isEmpty()) return false

        // Extract hostname
        val host = extractHost(url)

        // Check whitelist first
        if (isWhitelisted(url)) return false

        // 1. Check hosts list (fastest O(1) lookup)
        if (host != null && isHostBlocked(host)) return true

        // 2. Check filter rules
        if (matchesFilterRules(url)) return true

        return false
    }

    private fun isHostBlocked(host: String): Boolean {
        // Check exact match
        if (blockedHosts.contains(host)) return true

        // Check parent domains (e.g., sub.blocked.com -> blocked.com)
        val parts = host.split(".")
        for (i in 1 until parts.size - 1) {
            val parentDomain = parts.drop(i).joinToString(".")
            if (blockedHosts.contains(parentDomain)) return true
        }
        return false
    }

    private fun isWhitelisted(url: String): Boolean {
        return whitelistRules.any { it.pattern.containsMatchIn(url) }
    }

    private fun matchesFilterRules(url: String): Boolean {
        // Only check first 500 rules for performance (most important ones first)
        return filterRules.take(500).any { it.pattern.containsMatchIn(url) }
    }

    private fun extractHost(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    fun getStats(): String {
        return "Blocked hosts: ${blockedHosts.size} | Filter rules: ${filterRules.size}"
    }
}
