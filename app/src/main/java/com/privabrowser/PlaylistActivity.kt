package com.privabrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.privabrowser.databinding.ActivityPlaylistBinding
import com.privabrowser.databinding.ItemVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistBinding
    private lateinit var adapter: VideoAdapter
    private val videos = mutableListOf<VideoEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "📋 Playlist"
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        loadVideos()
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(videos,
            onPlay = { video -> playVideo(video) },
            onDelete = { video -> confirmDelete(video) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadVideos() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@PlaylistActivity)
            val result = withContext(Dispatchers.IO) { db.videoDao().getAllVideos() }
            videos.clear()
            videos.addAll(result)
            adapter.notifyDataSetChanged()

            binding.tvEmpty.visibility = if (videos.isEmpty())
                android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun playVideo(video: VideoEntity) {
        // Try local file first, fallback to URL
        val file = File(video.localPath)
        val uri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            Uri.parse(video.url)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No video player installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(video: VideoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Video")
            .setMessage("Remove '${video.title}' from playlist?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(this@PlaylistActivity)
                    withContext(Dispatchers.IO) { db.videoDao().delete(video) }
                    // Also delete local file
                    File(video.localPath).takeIf { it.exists() }?.delete()
                    loadVideos()
                    Toast.makeText(this@PlaylistActivity, "Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// ─────────────────────────────────────────
// RecyclerView Adapter
// ─────────────────────────────────────────
class VideoAdapter(
    private val items: List<VideoEntity>,
    private val onPlay: (VideoEntity) -> Unit,
    private val onDelete: (VideoEntity) -> Unit
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = items[position]
        with(holder.binding) {
            tvTitle.text = video.title
            tvUrl.text = video.url.take(60) + if (video.url.length > 60) "..." else ""
            tvDate.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(video.timestamp))
            tvStatus.text = if (File(video.localPath).exists()) "✅ Downloaded" else "🌐 Online"

            btnPlay.setOnClickListener { onPlay(video) }
            btnDelete.setOnClickListener { onDelete(video) }
        }
    }

    override fun getItemCount() = items.size
}
