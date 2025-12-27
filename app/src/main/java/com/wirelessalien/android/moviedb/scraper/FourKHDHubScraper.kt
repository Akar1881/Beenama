package com.wirelessalien.android.moviedb.scraper

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SearchResult(
    val title: String,
    val url: String,
    val poster: String? = null,
    val year: String? = null
)

data class ContentInfo(
    val title: String,
    val poster: String? = null,
    val tags: List<String> = emptyList(),
    val year: Int? = null,
    val description: String? = null,
    val trailer: String? = null,
    val type: String = "movie",
    val downloadLinks: List<String> = emptyList(),
    val episodes: List<EpisodeInfo> = emptyList()
)

data class EpisodeInfo(
    val season: Int,
    val episode: Int,
    val downloadLinks: List<String> = emptyList()
)

data class StreamInfo(
    val name: String,
    val title: String,
    val url: String,
    val quality: Int = 1080,
    val size: String? = null,
    val fileName: String? = null,
    val type: String = "direct"
)

class FourKHDHubScraper(
    private val client: OkHttpClient,
    private val tmdbApiKey: String = "439c478a771f35c05022f9feabcca01c"
) {
    companion object {
        private const val TAG = "FourKHDHubScraper"
        private const val DOMAINS_URL = "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/domains.json"
        
        private val DEFAULT_HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Connection" to "keep-alive"
        )
        
        private val QUALITY_TAGS = listOf(
            "WEBRip", "WEB-DL", "WEB", "BluRay", "HDRip", "DVDRip", "HDTV", "CAM", "TS", "R5",
            "DVDScr", "BRRip", "BDRip", "DVD", "PDTV", "HD"
        )
        
        private val AUDIO_TAGS = listOf(
            "AAC", "AC3", "DTS", "MP3", "FLAC", "DD5", "EAC3", "Atmos"
        )
        
        private val SUB_TAGS = listOf(
            "ESub", "ESubs", "Subs", "MultiSub", "NoSub", "EnglishSub", "HindiSub"
        )
        
        private val CODEC_TAGS = listOf(
            "x264", "x265", "H264", "HEVC", "AVC"
        )
    }
    
    // Caches
    private var domainsCache: Map<String, String>? = null
    private val resolvedUrlsCache = mutableMapOf<String, List<String>>()
    private val cacheMutex = Mutex()
    
    // Base64 helper
    private fun base64Decode(str: String): String {
        return try {
            String(Base64.getDecoder().decode(str), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun base64Encode(str: String): String {
        return try {
            Base64.getEncoder().encodeToString(str.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            ""
        }
    }
    
    // Rot13 helper
    private fun rot13(str: String): String {
        return str.map { char ->
            when {
                char in 'a'..'z' -> {
                    val start = 'a'.code
                    val offset = (char.code - start + 13) % 26
                    (start + offset).toChar()
                }
                char in 'A'..'Z' -> {
                    val start = 'A'.code
                    val offset = (char.code - start + 13) % 26
                    (start + offset).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }
    
    // Decode filename
    private fun decodeFilename(filename: String?): String {
        if (filename.isNullOrEmpty()) return ""
        return try {
            var decoded = filename
            if (decoded.startsWith("UTF-8")) {
                decoded = decoded.substring(5)
            }
            URLDecoder.decode(decoded, "UTF-8")
        } catch (e: Exception) {
            filename
        }
    }
    
    // Get quality from string
    private fun getIndexQuality(str: String): Int {
        val pattern = "(\\d{3,4})[pP]".toRegex()
        val match = pattern.find(str)
        return match?.groups?.get(1)?.value?.toIntOrNull() ?: 2160
    }
    
    // Clean title
    private fun cleanTitle(title: String): String {
        val decodedTitle = decodeFilename(title)
        val parts = decodedTitle.split("[.\\-_]".toRegex())
        
        val startIndex = parts.indexOfFirst { part ->
            QUALITY_TAGS.any { tag ->
                part.contains(tag, ignoreCase = true)
            }
        }
        
        val endIndex = parts.mapIndexedNotNull { index, part ->
            val hasTag = (SUB_TAGS + AUDIO_TAGS + CODEC_TAGS).any { tag ->
                part.contains(tag, ignoreCase = true)
            }
            if (hasTag) index else null
        }.lastOrNull() ?: -1
        
        return when {
            startIndex != -1 && endIndex != -1 && endIndex >= startIndex -> 
                parts.subList(startIndex, endIndex + 1).joinToString(".")
            startIndex != -1 -> 
                parts.subList(startIndex, parts.size).joinToString(".")
            else -> 
                parts.takeLast(3).joinToString(".")
        }
    }
    
    // Normalize title
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
    
    // Calculate similarity (Levenshtein distance)
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = normalizeTitle(str1)
        val s2 = normalizeTitle(str2)
        
        if (s1 == s2) return 1.0
        if (s1.isEmpty()) return if (s2.isEmpty()) 1.0 else 0.0
        if (s2.isEmpty()) return 0.0
        
        val len1 = s1.length
        val len2 = s2.length
        val matrix = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) matrix[i][0] = i
        for (j in 0..len2) matrix[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j - 1] + cost
                )
            }
        }
        
        val maxLen = max(len1, len2).toDouble()
        return (maxLen - matrix[len1][len2]) / maxLen
    }
    
    // Find best match
    private fun findBestMatch(results: List<SearchResult>, query: String, targetYear: Int?): SearchResult? {
        if (results.isEmpty()) return null
        if (results.size == 1) return results[0]
        
        val scored = results.map { result ->
            var score = 0.0
            
            // Exact title match
            if (normalizeTitle(result.title) == normalizeTitle(query)) score += 100
            
            // Similarity score
            val similarity = calculateSimilarity(result.title, query)
            score += similarity * 50
            
            // Contains query
            if (normalizeTitle(result.title).contains(normalizeTitle(query))) score += 15
            
            // Length difference penalty
            val lengthDiff = abs(result.title.length - query.length)
            score += max(0.0, 10.0 - lengthDiff / 5.0)
            
            // Has year in title
            if (result.title.contains("(19|20)\\d{2}".toRegex())) score += 5
            
            // Year matching
            val resultYear = result.year?.let {
                "(19|20)\\d{2}".toRegex().find(it)?.value?.toIntOrNull()
            }
            
            targetYear?.let { target ->
                when {
                    resultYear == target -> score += 200
                    resultYear != null && abs(resultYear - target) <= 1 -> score += 30
                    resultYear != null -> score -= 100
                }
            }
            
            Pair(result, score)
        }.sortedByDescending { it.second }
        
        return scored.firstOrNull()?.first
    }
    
    // Make HTTP request
    private suspend fun makeRequest(url: String, headers: Map<String, String> = emptyMap()): Response {
        val requestBuilder = Request.Builder()
            .url(url)
            .headers((DEFAULT_HEADERS + headers).toHeaders())
        
        return withContext(Dispatchers.IO) {
            client.newCall(requestBuilder.build()).execute()
        }
    }
    
    // Get domains
    private suspend fun getDomains(): Map<String, String>? {
        cacheMutex.withLock {
            if (domainsCache != null) return domainsCache
            
            return try {
                val response = makeRequest(DOMAINS_URL)
                if (response.isSuccessful) {
                    val json = response.body?.string() ?: ""
                    val domains = parseJsonDomains(json)
                    domainsCache = domains
                    domains
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get domains: ${e.message}")
                null
            }
        }
    }
    
    private fun parseJsonDomains(json: String): Map<String, String> {
        return try {
            // Simple JSON parsing for domain map
            val cleaned = json.trim()
                .removePrefix("{")
                .removeSuffix("}")
                .replace("\"", "")
            
            cleaned.split(",").associate { pair ->
                val keyValue = pair.split(":")
                if (keyValue.size == 2) {
                    keyValue[0].trim() to keyValue[1].trim()
                } else {
                    "" to ""
                }
            }.filter { it.key.isNotEmpty() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    // Search content
    suspend fun searchContent(query: String): List<SearchResult> {
        val domains = getDomains() ?: return emptyList()
        val baseUrl = domains["4khdhub"] ?: return emptyList()
        
        val searchUrl = "$baseUrl/?s=${URLEncoder.encode(query, "UTF-8")}"
        
        return try {
            val response = makeRequest(searchUrl)
            if (!response.isSuccessful) return emptyList()
            
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            val results = mutableListOf<SearchResult>()
            
            // First try - movie cards
            doc.select("a").forEach { element ->
                val title = element.select("h3.movie-card-title").text().trim()
                val href = element.attr("href")
                val poster = element.select("img").attr("src").takeIf { it.isNotEmpty() }
                val year = element.select("p.movie-card-meta").text().trim()
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    val absoluteUrl = if (href.startsWith("http")) href else {
                        baseUrl + (if (href.startsWith("/")) "" else "/") + href
                    }
                    results.add(SearchResult(title, absoluteUrl, poster, year))
                }
            }
            
            // Second try - card grid
            if (results.isEmpty()) {
                doc.select("div.card-grid a").forEach { element ->
                    val title = element.select("h3").text().trim()
                    val href = element.attr("href")
                    val poster = element.select("img").attr("src").takeIf { it.isNotEmpty() }
                    
                    if (title.isNotEmpty() && href.isNotEmpty()) {
                        val absoluteUrl = if (href.startsWith("http")) href else {
                            baseUrl + (if (href.startsWith("/")) "" else "/") + href
                        }
                        results.add(SearchResult(title, absoluteUrl, poster))
                    }
                }
            }
            
            // Third try - generic links with year pattern
            if (results.isEmpty()) {
                doc.select("a[href]").forEach { element ->
                    val href = element.attr("href")
                    val title = element.text().trim()
                    
                    if (title.isNotEmpty() && href.contains("/\\d{4}/".toRegex())) {
                        val absoluteUrl = if (href.startsWith("http")) href else {
                            baseUrl + (if (href.startsWith("/")) "" else "/") + href
                        }
                        results.add(SearchResult(title, absoluteUrl))
                    }
                }
            }
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}")
            emptyList()
        }
    }
    
    // Load content page
    suspend fun loadContent(url: String): ContentInfo {
        return try {
            val response = makeRequest(url)
            if (!response.isSuccessful) throw Exception("Failed to load content")
            
            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html)
            
            val title = doc.select("h1.page-title").text().split("(")[0].trim()
            val poster = doc.select("meta[property=\"og:image\"]").attr("content").takeIf { it.isNotEmpty() }
            
            val tags = doc.select("div.mt-2 span.badge").map { it.text() }
            val year = doc.select("div.mt-2 span").first()?.text()
                ?.replace("[^0-9]".toRegex(), "")
                ?.toIntOrNull()
            
            val description = doc.select("div.content-section p.mt-4").text().trim()
            val trailer = doc.select("#trailer-btn").attr("data-trailer-url").takeIf { it.isNotEmpty() }
            val isMovie = tags.contains("Movies")
            
            // Collect download links
            val hrefsSet = mutableSetOf<String>()
            val selectors = listOf(
                "div.download-item a",
                ".download-item a",
                "a[href*=\"hubdrive\"]",
                "a[href*=\"hubcloud\"]",
                "a[href*=\"pixeldrain\"]",
                "a[href*=\"buzz\"]",
                "a[href*=\"10gbps\"]",
                "a[href*=\"drive\"]",
                "a.btn[href]",
                "a.btn",
                "a[href]"
            )
            
            selectors.forEach { selector ->
                doc.select(selector).forEach { element ->
                    val href = element.attr("href").trim()
                    if (href.isNotEmpty() && 
                        (href.contains("hubdrive", ignoreCase = true) ||
                         href.contains("hubcloud", ignoreCase = true) ||
                         href.contains("pixeldrain", ignoreCase = true) ||
                         href.contains("buzz", ignoreCase = true) ||
                         href.contains("10gbps", ignoreCase = true) ||
                         href.contains("workers.dev", ignoreCase = true) ||
                         href.contains("r2.dev", ignoreCase = true) ||
                         href.contains("id=", ignoreCase = true) ||
                         href.contains("download", ignoreCase = true) ||
                         href.contains("s3", ignoreCase = true) ||
                         href.contains("fsl", ignoreCase = true))) {
                        hrefsSet.add(href)
                    }
                }
            }
            
            val hrefs = hrefsSet.toList()
            
            if (isMovie) {
                return ContentInfo(
                    title = title,
                    poster = poster,
                    tags = tags,
                    year = year,
                    description = description,
                    trailer = trailer,
                    type = "movie",
                    downloadLinks = hrefs
                )
            }
            
            // TV series - parse episodes
            val episodesMap = mutableMapOf<String, EpisodeInfo>()
            
            doc.select("div.episodes-list div.season-item").forEach { seasonEl ->
                val seasonText = seasonEl.select("div.episode-number").text()
                val seasonMatch = "S?([1-9][0-9]*)".toRegex().find(seasonText)
                val seasonNum = seasonMatch?.groups?.get(1)?.value?.toIntOrNull()
                
                seasonEl.select("div.episode-download-item").forEach { epEl ->
                    val epText = epEl.select("div.episode-file-info span.badge-psa").text()
                    val epMatch = "Episode-0*([1-9][0-9]*)".toRegex().find(epText)
                    val episodeNum = epMatch?.groups?.get(1)?.value?.toIntOrNull()
                    
                    val epLinks = epEl.select("a").mapNotNull { 
                        it.attr("href").trim().takeIf { href -> href.isNotEmpty() }
                    }
                    
                    if (seasonNum != null && episodeNum != null && epLinks.isNotEmpty()) {
                        val key = "$seasonNum-$episodeNum"
                        val existing = episodesMap[key]
                        
                        if (existing == null) {
                            episodesMap[key] = EpisodeInfo(
                                season = seasonNum,
                                episode = episodeNum,
                                downloadLinks = epLinks.toMutableList()
                            )
                        } else {
                            val mergedLinks = (existing.downloadLinks + epLinks).distinct()
                            episodesMap[key] = existing.copy(downloadLinks = mergedLinks)
                        }
                    }
                }
            }
            
            val episodes = episodesMap.values.toList()
            
            ContentInfo(
                title = title,
                poster = poster,
                tags = tags,
                year = year,
                description = description,
                trailer = trailer,
                type = "series",
                episodes = if (episodes.isEmpty() && hrefs.isNotEmpty()) {
                    listOf(EpisodeInfo(season = 1, episode = 1, downloadLinks = hrefs))
                } else {
                    episodes
                }
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load content: ${e.message}")
            throw e
        }
    }
    
    // Extract streaming links
    suspend fun extractStreamingLinks(downloadLinks: List<String>): List<StreamInfo> {
        val tasks = downloadLinks.map { link ->
            CoroutineScope(Dispatchers.IO).async {
                processExtractorLink(link)
            }
        }
        
        val results = tasks.awaitAll()
        val flatResults = results.flatten()
        
        // Filter suspicious links
        val suspicious = listOf(
            "www-google-com.cdn.ampproject.org",
            "bloggingvector.shop",
            "cdn.ampproject.org"
        )
        
        val filtered = flatResults.filter { stream ->
            val url = stream.url.lowercase()
            !url.endsWith(".zip") && suspicious.none { url.contains(it) }
        }
        
        // Remove duplicates by URL
        val seen = mutableSetOf<String>()
        return filtered.filter { seen.add(it.url) }
    }
    
    // Process extractor link
    private suspend fun processExtractorLink(link: String): List<StreamInfo> {
        val lowerLink = link.lowercase()
        
        return when {
            lowerLink.contains("hubdrive") -> extractHubDriveLinks(link)
            lowerLink.contains("hubcloud") -> extractHubCloudLinks(link)
            lowerLink.contains("workers.dev") || lowerLink.contains("r2.dev") -> {
                val fileName = getFilenameFromUrl(link)
                listOf(
                    StreamInfo(
                        name = "4KHDHub - HubCloud - 1080p",
                        title = fileName ?: "HubCloud File",
                        url = link,
                        quality = 1080,
                        fileName = fileName
                    )
                )
            }
            lowerLink.contains("pixeldrain") -> {
                val convertedLink = if ("/u/" in link) {
                    val fileId = link.substringAfterLast("/")
                    "https://pixeldrain.net/api/file/$fileId"
                } else {
                    link
                }
                val fileName = getFilenameFromUrl(convertedLink)
                listOf(
                    StreamInfo(
                        name = "4KHDHub - Pixeldrain - 1080p",
                        title = "${fileName ?: "Pixeldrain File"}\nPixeldrain",
                        url = convertedLink,
                        quality = 1080,
                        fileName = fileName
                    )
                )
            }
            link.matches(".*\\.m(ov|p4|kv)$|.*\\.avi$".toRegex(RegexOption.IGNORE_CASE)) -> {
                val fileName = getFilenameFromUrl(link) ?: try {
                    URL(link).path.substringAfterLast("/").substringBeforeLast(".")
                        .replace("[._]".toRegex(), " ")
                } catch (e: Exception) {
                    "Direct Link"
                }
                listOf(
                    StreamInfo(
                        name = "4KHDHub Direct Link",
                        title = "$fileName\n[Direct Link]",
                        url = link,
                        quality = 1080,
                        fileName = fileName
                    )
                )
            }
            else -> emptyList()
        }
    }
    
    // Extract HubCloud links
    private suspend fun extractHubCloudLinks(url: String): List<StreamInfo> {
        return try {
            val origin = try { URL(url).protocol + "://" + URL(url).host } catch (e: Exception) { "" }
            
            val response = makeRequest(url)
            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)
            
            var href = url
            if (!url.contains("hubcloud.php")) {
                val rawHref = doc.select("#download").attr("href")
                    .takeIf { it.isNotEmpty() }
                    ?: doc.select("a[href*=\"hubcloud.php\"]").attr("href")
                    .takeIf { it.isNotEmpty() }
                    ?: doc.select(".download-btn").attr("href")
                    .takeIf { it.isNotEmpty() }
                    ?: doc.select("a[href*=\"download\"]").attr("href")
                    .takeIf { it.isNotEmpty() }
                
                if (rawHref.isNullOrEmpty()) return emptyList()
                
                href = if (rawHref.startsWith("http")) rawHref else {
                    if (rawHref.startsWith("/")) {
                        origin + rawHref
                    } else {
                        origin + "/" + rawHref
                    }
                }
            }
            
            val response2 = makeRequest(href)
            val html2 = response2.body?.string() ?: return emptyList()
            val doc2 = Jsoup.parse(html2)
            
            val tasks = mutableListOf<Deferred<StreamInfo?>>()
            
            doc2.select(".card").forEach { card ->
                val header = card.select("div.card-header").text()
                    .takeIf { it.isNotEmpty() }
                    ?: doc2.select("div.card-header").first()?.text() ?: ""
                
                val size = card.select("i#size").text()
                    .takeIf { it.isNotEmpty() }
                    ?: doc2.select("i#size").first()?.text() ?: ""
                
                val quality = getIndexQuality(header)
                val headerDetails = cleanTitle(header)
                
                val buttons = card.select("div.card-body h2 a.btn")
                    .takeIf { it.isNotEmpty() }
                    ?: card.select("a.btn, .btn, a[href]")
                
                buttons.forEach { button ->
                    val text = button.text().trim()
                    var link = button.attr("href")
                    
                    if (link.isNotEmpty()) {
                        link = if (link.startsWith("http")) link else {
                            if (link.startsWith("/")) {
                                try { URL(href).protocol + "://" + URL(href).host + link }
                                catch (e: Exception) { href + link }
                            } else {
                                href + "/" + link
                            }
                        }
                        
                        if (link.matches(".*(hubcloud|hubdrive|pixeldrain|buzz|10gbps|workers\\.dev|r2\\.dev|download|api/file).*".toRegex(RegexOption.IGNORE_CASE)) ||
                            text.contains("download", ignoreCase = true)) {
                            
                            tasks.add(CoroutineScope(Dispatchers.IO).async {
                                buildStreamInfo(text, link, headerDetails, size, quality, href)
                            })
                        }
                    }
                }
            }
            
            if (tasks.isEmpty()) {
                var buttons = doc2.select("div.card-body h2 a.btn")
                if (buttons.isEmpty()) {
                    buttons = doc2.select("a.btn")
                    if (buttons.isEmpty()) {
                        buttons = doc2.select(".btn")
                        if (buttons.isEmpty()) {
                            buttons = doc2.select("a[href]")
                        }
                    }
                }
                
                val size = doc2.select("i#size").first()?.text() ?: ""
                val header = doc2.select("div.card-header").first()?.text() ?: ""
                val quality = getIndexQuality(header)
                val headerDetails = cleanTitle(header)
                
                buttons.forEach { button ->
                    val text = button.text().trim()
                    var link = button.attr("href")
                    
                    if (link.isNotEmpty()) {
                        link = if (link.startsWith("http")) link else {
                            if (link.startsWith("/")) {
                                try { URL(href).protocol + "://" + URL(href).host + link }
                                catch (e: Exception) { href + link }
                            } else {
                                href + "/" + link
                            }
                        }
                        
                        tasks.add(CoroutineScope(Dispatchers.IO).async {
                            buildStreamInfo(text, link, headerDetails, size, quality, href)
                        })
                    }
                }
            }
            
            tasks.awaitAll().filterNotNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract HubCloud links: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun buildStreamInfo(
        buttonText: String,
        buttonLink: String,
        headerDetails: String,
        size: String,
        quality: Int,
        baseUrl: String
    ): StreamInfo? {
        val qualityLabel = if (quality > 0) " - ${quality}p" else ""
        
        var finalLink = buttonLink
        if (buttonLink.contains("pixeldrain.net/u/") || buttonLink.contains("pixeldrain.dev/u/")) {
            val fileId = buttonLink.substringAfterLast("/")
            finalLink = "https://pixeldrain.net/api/file/$fileId"
        }
        
        return when {
            buttonText.contains("BuzzServer", ignoreCase = true) -> {
                resolveBuzzServer(buttonLink).let { resolvedUrl ->
                    val fileName = getFilenameFromUrl(resolvedUrl)
                    val displayName = fileName ?: headerDetails.takeIf { it.isNotEmpty() } ?: "Unknown"
                    val titleParts = mutableListOf<String>()
                    if (displayName.isNotEmpty()) titleParts.add(displayName)
                    if (size.isNotEmpty()) titleParts.add(size)
                    
                    StreamInfo(
                        name = "4KHDHub - BuzzServer$qualityLabel",
                        title = titleParts.joinToString("\n"),
                        url = resolvedUrl,
                        quality = quality,
                        size = size.takeIf { it.isNotEmpty() },
                        fileName = fileName
                    )
                }
            }
            buttonText.contains("10Gbps", ignoreCase = true) -> {
                resolveTenGbps(buttonLink, headerDetails, size, qualityLabel, quality, baseUrl)
            }
            else -> {
                val fileName = getFilenameFromUrl(finalLink)
                val displayName = fileName ?: headerDetails.takeIf { it.isNotEmpty() } ?: "Unknown"
                val titleParts = mutableListOf<String>()
                if (displayName.isNotEmpty()) titleParts.add(displayName)
                if (size.isNotEmpty()) titleParts.add(size)
                
                val name = when {
                    buttonText.contains("FSL Server", ignoreCase = true) -> "4KHDHub - FSL Server$qualityLabel"
                    buttonText.contains("S3 Server", ignoreCase = true) -> "4KHDHub - S3 Server$qualityLabel"
                    buttonText.contains("pixeldrain", ignoreCase = true) || 
                    finalLink.contains("pixeldrain", ignoreCase = true) -> "4KHDHub - Pixeldrain$qualityLabel"
                    buttonText.contains("Download File", ignoreCase = true) -> "4KHDHub - HubCloud$qualityLabel"
                    else -> "4KHDHub - HubCloud$qualityLabel"
                }
                
                StreamInfo(
                    name = name,
                    title = titleParts.joinToString("\n"),
                    url = finalLink,
                    quality = quality,
                    size = size.takeIf { it.isNotEmpty() },
                    fileName = fileName
                )
            }
        }
    }
    
    private suspend fun resolveBuzzServer(buttonLink: String): String {
        return try {
            val dlUrl = buttonLink.removeSuffix("/") + "/download"
            val request = Request.Builder()
                .url(dlUrl)
                .header("Referer", buttonLink)
                .headers(DEFAULT_HEADERS.toHeaders())
                .build()
            
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.header("hx-redirect")
                        ?: response.header("location")
                        ?: response.request.url.toString()
                }
            }
        } catch (e: Exception) {
            buttonLink
        }
    }
    
    private suspend fun resolveTenGbps(
        initialLink: String,
        headerDetails: String,
        size: String,
        qualityLabel: String,
        quality: Int,
        baseOrigin: String
    ): StreamInfo? {
        return try {
            var current = initialLink
            var finalUrl: String? = null
            var hops = 0
            val maxHops = 6
            
            while (hops < maxHops) {
                val request = Request.Builder()
                    .url(current)
                    .headers(DEFAULT_HEADERS.toHeaders())
                    .build()
                
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val location = response.header("location")
                        if (location == null) {
                            finalUrl = current
                        } else if (location.contains("id=")) {
                            val linkParam = location.substringAfter("link=").substringBefore("&")
                            finalUrl = if (linkParam.isNotEmpty()) {
                                try { URLDecoder.decode(linkParam, "UTF-8") } catch (e: Exception) { linkParam }
                            } else {
                                null
                            }
                        } else {
                            current = if (location.startsWith("http")) location else {
                                if (location.startsWith("/")) {
                                    try { URL(current).protocol + "://" + URL(current).host + location }
                                    catch (e: Exception) { baseOrigin + location }
                                } else {
                                    baseOrigin + "/" + location
                                }
                            }
                        }
                    }
                }
                
                if (finalUrl != null) break
                hops++
            }
            
            finalUrl?.let { url ->
                val fileName = getFilenameFromUrl(url)
                val displayName = fileName ?: headerDetails.takeIf { it.isNotEmpty() } ?: "Unknown"
                val titleParts = mutableListOf<String>()
                if (displayName.isNotEmpty()) titleParts.add(displayName)
                if (size.isNotEmpty()) titleParts.add(size)
                
                StreamInfo(
                    name = "4KHDHub - 10Gbps Server$qualityLabel",
                    title = titleParts.joinToString("\n"),
                    url = url,
                    quality = quality,
                    size = size.takeIf { it.isNotEmpty() },
                    fileName = fileName
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Extract HubDrive links
    private suspend fun extractHubDriveLinks(url: String): List<StreamInfo> {
        return try {
            val response = makeRequest(url)
            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)
            
            val size = doc.select("i#size").text()
            val header = doc.select("div.card-header").text()
            val quality = getIndexQuality(header)
            val headerDetails = cleanTitle(header)
            
            val filename = headerDetails
                .replace("^4kHDHub\\.com\\s*[-_]?\\s*".toRegex(RegexOption.IGNORE_CASE), "")
                .replace("\\.[a-z0-9]{2,4}$".toRegex(RegexOption.IGNORE_CASE), "")
                .replace("[._]".toRegex(), " ")
                .trim()
            
            val primaryBtn = doc.select(".btn.btn-primary.btn-user.btn-success1.m-1").attr("href")
                .takeIf { it.isNotEmpty() }
                ?: doc.select("a.btn.btn-primary").attr("href")
                .takeIf { it.isNotEmpty() }
                ?: doc.select("a[href*=\"download\"]").attr("href")
                .takeIf { it.isNotEmpty() }
                ?: doc.select("a.btn").attr("href")
                .takeIf { it.isNotEmpty() }
            
            if (primaryBtn.isNullOrEmpty()) return emptyList()
            
            if (primaryBtn.contains("hubcloud", ignoreCase = true)) {
                return extractHubCloudLinks(primaryBtn)
            }
            
            val qualityLabel = if (quality > 0) " - ${quality}p" else ""
            val fileName = getFilenameFromUrl(primaryBtn)
            val displayName = fileName ?: filename.takeIf { it.isNotEmpty() } ?: "Unknown"
            val titleParts = mutableListOf<String>()
            if (displayName.isNotEmpty()) titleParts.add(displayName)
            if (size.isNotEmpty()) titleParts.add(size)
            
            listOf(
                StreamInfo(
                    name = "4KHDHub - HubDrive$qualityLabel",
                    title = titleParts.joinToString("\n"),
                    url = primaryBtn,
                    quality = quality,
                    size = size.takeIf { it.isNotEmpty() },
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract HubDrive links: ${e.message}")
            emptyList()
        }
    }
    
    // Get filename from URL
    private suspend fun getFilenameFromUrl(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .headers(DEFAULT_HEADERS.toHeaders())
                .build()
            
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val cd = response.header("content-disposition")
                    var filename: String? = null
                    
                    if (cd != null) {
                        val pattern = "filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)".toRegex(RegexOption.IGNORE_CASE)
                        val match = pattern.find(cd)
                        filename = match?.groups?.get(1)?.value?.replace("[\"\']".toRegex(), "")
                    }
                    
                    if (filename.isNullOrEmpty()) {
                        try {
                            val urlObj = URL(url)
                            filename = urlObj.path.substringAfterLast("/")
                            if (filename.contains(".")) {
                                filename = filename.substringBeforeLast(".")
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    decodeFilename(filename)
                }
            }
        } catch (e: Exception) {
            try {
                val urlObj = URL(url)
                val filename = urlObj.path.substringAfterLast("/")
                if (filename.contains(".")) {
                    decodeFilename(filename.substringBeforeLast("."))
                } else {
                    decodeFilename(filename)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // Main function to get streams
    suspend fun getStreams(
        tmdbId: String,
        type: String = "movie",
        season: Int? = null,
        episode: Int? = null
    ): List<StreamInfo> {
        val cacheKey = "4khdhub_resolved_urls_v1_${tmdbId}_$type${season?.let { "_s${it}e${episode ?: ""}" } ?: ""}"
        
        val cached = cacheMutex.withLock {
            resolvedUrlsCache[cacheKey]
        }
        
        if (cached != null && cached.isNotEmpty()) {
            return extractStreamingLinks(cached)
        }
        
        // TMDB integration would go here
        // For now, we'll skip TMDB and work with direct search
        
        return emptyList()
    }
    
    // Alternative search function
    suspend fun search(query: String): List<SearchResult> {
        return searchContent(query)
    }
    
    // Clear cache
    fun clearCache() {
        cacheMutex.withLock {
            domainsCache = null
            resolvedUrlsCache.clear()
        }
    }
    
    // Validate video URL
    suspend fun validateVideoUrl(url: String): Boolean {
        Log.d(TAG, "Validating URL: ${url.take(100)}...")
        
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
                .headers(DEFAULT_HEADERS.toHeaders())
                .build()
            
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val isValid = response.isSuccessful || response.code == 206
                    if (isValid) {
                        Log.d(TAG, "✓ URL validation successful (${response.code})")
                    } else {
                        Log.d(TAG, "✗ URL validation failed with status: ${response.code}")
                    }
                    isValid
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ URL validation failed: ${e.message}")
            false
        }
    }
}