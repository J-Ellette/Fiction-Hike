package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val trekId: String,
    val trekTitle: String,
    val universe: String,
    val characterName: String,
    val anchorLat: Double,
    val anchorLon: Double,
    val scaleKm: Double,
    val tileCount: Int,
    val sizeBytes: Long,
    val elevationGridCsv: String,
    val downloadedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_tiles")
data class OfflineTileEntity(
    @PrimaryKey val tileKey: String,
    val trekId: String,
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
    val imageBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OfflineTileEntity
        return tileKey == other.tileKey
    }

    override fun hashCode(): Int = tileKey.hashCode()
}
