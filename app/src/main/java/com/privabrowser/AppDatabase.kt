package com.privabrowser

import android.content.Context
import androidx.room.*

// ─────────────────────────────────────────
// ENTITY
// ─────────────────────────────────────────
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val localPath: String,
    val downloadId: Long = -1L,
    val thumbnail: String? = null,
    val duration: Long = 0L,
    val fileSize: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false
)

// ─────────────────────────────────────────
// DAO
// ─────────────────────────────────────────
@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY timestamp DESC")
    suspend fun getAllVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    suspend fun getDownloadedVideos(): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Delete
    suspend fun delete(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE videos SET isDownloaded = 1 WHERE downloadId = :downloadId")
    suspend fun markAsDownloaded(downloadId: Long)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int
}

// ─────────────────────────────────────────
// DATABASE
// ─────────────────────────────────────────
@Database(
    entities = [VideoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privabrowser_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
