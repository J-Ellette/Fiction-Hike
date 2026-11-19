package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.local.OfflineMapDao
import com.example.data.local.OfflineRegionEntity
import com.example.data.local.OfflineTileEntity
import com.example.data.model.FictionalTrek
import com.example.data.model.GeoProjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class OfflineTileManager(
    private val dao: OfflineMapDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val downloadedRegions: Flow<List<OfflineRegionEntity>> = dao.observeAllRegions()

    suspend fun downloadTrekRegion(
        trek: FictionalTrek,
        anchorLat: Double,
        anchorLon: Double,
        scaleKm: Double,
        zoom: Int = 14,
        onProgress: (Float) -> Unit = {}
    ): Result<OfflineRegionEntity> = withContext(Dispatchers.IO) {
        try {
            val (centerX, centerY) = GeoProjection.latLonToTileXY(anchorLat, anchorLon, zoom)
            val tileCoords = buildList {
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        add((centerX + dx).coerceAtLeast(0) to (centerY + dy).coerceAtLeast(0))
                    }
                }
            }

            val downloadedTiles = mutableListOf<OfflineTileEntity>()
            var totalBytes = 0L

            tileCoords.forEachIndexed { index, (tx, ty) ->
                val bytes = fetchRemoteTileOrSynthesize(zoom, tx, ty)
                totalBytes += bytes.size.toLong()
                downloadedTiles += OfflineTileEntity(
                    tileKey = "${zoom}_${tx}_$ty",
                    trekId = trek.id,
                    zoom = zoom,
                    tileX = tx,
                    tileY = ty,
                    imageBytes = bytes
                )
                onProgress((index + 1).toFloat() / tileCoords.size.toFloat())
            }

            val grid = GeoProjection.generateTerrainGrid(trek.waypoints)
            val gridCsv = grid.joinToString(";") { row ->
                row.joinToString(",") { "%.1f".format(it) }
            }

            val region = OfflineRegionEntity(
                trekId = trek.id,
                trekTitle = trek.title,
                universe = trek.universe,
                characterName = trek.characterName,
                anchorLat = anchorLat,
                anchorLon = anchorLon,
                scaleKm = scaleKm,
                tileCount = downloadedTiles.size,
                sizeBytes = totalBytes + gridCsv.length.toLong(),
                elevationGridCsv = gridCsv
            )

            dao.deleteTilesForTrek(trek.id)
            dao.insertTiles(downloadedTiles)
            dao.upsertRegion(region)

            Result.success(region)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRegion(trekId: String) = withContext(Dispatchers.IO) {
        dao.deleteTilesForTrek(trekId)
        dao.deleteRegionByTrekId(trekId)
    }

    suspend fun getTileBytes(zoom: Int, x: Int, y: Int, allowNetwork: Boolean): ByteArray? =
        withContext(Dispatchers.IO) {
            val cached = dao.getTile(zoom, x, y)
            if (cached != null) return@withContext cached.imageBytes
            if (!allowNetwork) return@withContext null
            fetchRemoteTileOrNull(zoom, x, y)
        }

    private fun fetchRemoteTileOrNull(zoom: Int, x: Int, y: Int): ByteArray? {
        return try {
            val url = "https://tile.openstreetmap.org/$zoom/$x/$y.png"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FictionHikeAndroid/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()?.takeIf { it.isNotEmpty() }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchRemoteTileOrSynthesize(zoom: Int, x: Int, y: Int): ByteArray {
        val remote = fetchRemoteTileOrNull(zoom, x, y)
        if (remote != null) return remote
        return synthesizeTopographicTile(x, y)
    }

    /**
     * Generates a clean cartographic topographic PNG tile when remote tile servers are unavailable
     * so offline map packs always cache valid visual terrain tiles.
     */
    private fun synthesizeTopographicTile(tileX: Int, tileY: Int): ByteArray {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(18, 31, 26))

        val gridPaint = Paint().apply {
            color = Color.argb(40, 167, 184, 175)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 61, 213, 152)
            strokeWidth = 1.6f
            style = Paint.Style.STROKE
        }

        for (step in 0..size step 64) {
            canvas.drawLine(step.toFloat(), 0f, step.toFloat(), size.toFloat(), gridPaint)
            canvas.drawLine(0f, step.toFloat(), size.toFloat(), step.toFloat(), gridPaint)
        }

        val phase = (tileX * 13 + tileY * 7) * 0.4f
        for (ring in 1..4) {
            val r = ring * 30f
            val cx = 128f + 34f * sin(phase + ring)
            val cy = 128f + 34f * cos(phase - ring)
            canvas.drawCircle(cx, cy, r, contourPaint)
        }

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        bmp.recycle()
        return out.toByteArray()
    }
}
