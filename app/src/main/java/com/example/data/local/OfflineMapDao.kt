package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMapDao {
    @Query("SELECT * FROM offline_regions ORDER BY downloadedAtMillis DESC")
    fun observeAllRegions(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions WHERE trekId = :trekId LIMIT 1")
    suspend fun getRegionByTrekId(trekId: String): OfflineRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegion(region: OfflineRegionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<OfflineTileEntity>)

    @Query("SELECT * FROM offline_tiles WHERE trekId = :trekId AND zoom = :zoom")
    suspend fun getTilesForTrekZoom(trekId: String, zoom: Int): List<OfflineTileEntity>

    @Query("SELECT * FROM offline_tiles WHERE zoom = :zoom AND tileX = :x AND tileY = :y LIMIT 1")
    suspend fun getTile(zoom: Int, x: Int, y: Int): OfflineTileEntity?

    @Query("DELETE FROM offline_tiles WHERE trekId = :trekId")
    suspend fun deleteTilesForTrek(trekId: String)

    @Query("DELETE FROM offline_regions WHERE trekId = :trekId")
    suspend fun deleteRegionByTrekId(trekId: String)
}
