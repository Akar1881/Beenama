package com.wirelessalien.android.moviedb.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.wirelessalien.android.moviedb.databinding.ActivityWatchBinding
import com.wirelessalien.android.moviedb.scraper.FourKHDHubScraper
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class WatchActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWatchBinding
    private var player: ExoPlayer? = null
    private val client = OkHttpClient()
    private lateinit var scraper: FourKHDHubScraper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scraper = FourKHDHubScraper(client)

        val title = intent.getStringExtra("title") ?: ""
        val isMovie = intent.getBooleanExtra("isMovie", true)
        val year = intent.getIntExtra("year", 0)

        if (title.isNotEmpty()) {
            fetchAndPlay(title, year, isMovie)
        } else {
            Toast.makeText(this, "Invalid Title", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchAndPlay(title: String, year: Int, isMovie: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val results = scraper.searchContent(title)
                if (results.isNotEmpty()) {
                    val content = scraper.loadContent(results[0].url)
                    val links = if (isMovie) {
                        scraper.extractStreamingLinks(content.downloadLinks)
                    } else {
                        // Default to S1E1 for now or handle appropriately
                        if (content.episodes.isNotEmpty()) {
                            scraper.extractStreamingLinks(content.episodes[0].downloadLinks)
                        } else {
                            emptyList()
                        }
                    }

                    if (links.isNotEmpty()) {
                        startPlayer(links[0].url)
                    } else {
                        Toast.makeText(this@WatchActivity, "No links found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@WatchActivity, "Not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WatchActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun startPlayer(url: String) {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_MATROSKA)
            .build()
            
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}