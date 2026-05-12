package com.example.qrscannerapp.features.homescreen.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Single-row entity (id = 0) holding the user's chosen homescreen background.
 * Path points to the copied file in app's internal storage (filesDir/homescreen_bg).
 * Stored locally so the user gets the same look on cold start without a network round-trip.
 */
@Entity(tableName = "homescreen_background")
data class HomescreenBackgroundEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,                       // always 0 — only one row

    @ColumnInfo(name = "image_path")
    val imagePath: String?,                // null → fall back to shader background

    @ColumnInfo(name = "blur_amount")
    val blurAmount: Float = 24f,           // px of blur applied behind widgets

    @ColumnInfo(name = "darken_amount")
    val darkenAmount: Float = 0.35f,       // 0..1 — black scrim opacity over photo

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface HomescreenBackgroundDao {

    @Query("SELECT * FROM homescreen_background WHERE id = 0 LIMIT 1")
    fun observe(): Flow<HomescreenBackgroundEntity?>

    @Query("SELECT * FROM homescreen_background WHERE id = 0 LIMIT 1")
    suspend fun get(): HomescreenBackgroundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HomescreenBackgroundEntity)

    @Query("DELETE FROM homescreen_background WHERE id = 0")
    suspend fun clear()
}