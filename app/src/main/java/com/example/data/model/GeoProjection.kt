package com.example.data.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationM: Double = 0.0,
    val label: String = ""
)

data class ProjectedLoreDrop(
    val loreDrop: LoreDrop,
    val coordinate: GeoPoint,
    val distanceMetersFromUser: Double,
    val isUnlocked: Boolean,
    val isInTriggerRange: Boolean
)

object GeoProjection {
    private const val EARTH_RADIUS_M = 6_371_000.0
    const val LORE_TRIGGER_RADIUS_METERS = 55.0

    /**
     * Haversine great-circle distance in meters.
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /**
     * Compute normalized route length in abstract units so we can scale any trek cleanly
     * to a target real-world kilometer distance around the hiker's GPS anchor.
     */
    fun normalizedPathLength(waypoints: List<Waypoint>): Double {
        if (waypoints.size < 2) return 1.0
        var sum = 0.0
        for (i in 1 until waypoints.size) {
            val dx = waypoints[i].dx - waypoints[i - 1].dx
            val dy = waypoints[i].dy - waypoints[i - 1].dy
            sum += sqrt(dx * dx + dy * dy)
        }
        return sum.coerceAtLeast(0.05)
    }

    /**
     * Projects a fictional trek's normalized waypoints onto real-world GPS coordinates
     * anchored at (anchorLat, anchorLon) with total path length scaled to scaleKm.
     */
    fun projectWaypoints(
        waypoints: List<Waypoint>,
        anchorLat: Double,
        anchorLon: Double,
        scaleKm: Double
    ): List<GeoPoint> {
        if (waypoints.isEmpty()) return emptyList()
        val normLen = normalizedPathLength(waypoints)
        val kmPerUnit = scaleKm.coerceAtLeast(0.2) / normLen
        val metersPerUnit = kmPerUnit * 1000.0
        val first = waypoints.first()

        val latRad = Math.toRadians(anchorLat).coerceIn(-1.45, 1.45)
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = (111_320.0 * cos(latRad)).coerceAtLeast(1000.0)

        return waypoints.map { wp ->
            val eastMeters = (wp.dx - first.dx) * metersPerUnit
            val northMeters = (wp.dy - first.dy) * metersPerUnit
            GeoPoint(
                latitude = (anchorLat + northMeters / metersPerDegLat).coerceIn(-85.0, 85.0),
                longitude = wrapLongitude(anchorLon + eastMeters / metersPerDegLon),
                elevationM = wp.elevM,
                label = wp.label
            )
        }
    }

    fun projectLoreDrops(
        trek: FictionalTrek,
        projectedWaypoints: List<GeoPoint>,
        userLat: Double?,
        userLon: Double?,
        unlockedIds: Set<String>,
        isActivelyTrekking: Boolean
    ): List<ProjectedLoreDrop> {
        if (projectedWaypoints.isEmpty()) return emptyList()
        return trek.loreDrops.map { lore ->
            val idx = lore.waypointIndex.coerceIn(0, projectedWaypoints.lastIndex)
            val point = projectedWaypoints[idx]
            val dist = if (userLat != null && userLon != null) {
                distanceMeters(userLat, userLon, point.latitude, point.longitude)
            } else {
                Double.MAX_VALUE
            }
            val alreadyUnlocked = unlockedIds.contains(lore.id)
            val inRange = isActivelyTrekking && dist <= LORE_TRIGGER_RADIUS_METERS
            ProjectedLoreDrop(
                loreDrop = lore,
                coordinate = point,
                distanceMetersFromUser = dist,
                isUnlocked = alreadyUnlocked,
                isInTriggerRange = inRange
            )
        }
    }

    /**
     * Synthesize a 16x16 terrain elevation grid around the route for 3D topographic rendering
     * and offline relief caching, blending real waypoint peaks with deterministic ridge harmonics.
     */
    fun generateTerrainGrid(
        waypoints: List<Waypoint>,
        gridSize: Int = 18
    ): List<List<Double>> {
        val minX = (waypoints.minOfOrNull { it.dx } ?: -0.5) - 0.35
        val maxX = (waypoints.maxOfOrNull { it.dx } ?: 0.5) + 0.35
        val minY = (waypoints.minOfOrNull { it.dy } ?: -0.5) - 0.35
        val maxY = (waypoints.maxOfOrNull { it.dy } ?: 0.5) + 0.35
        val spanX = (maxX - minX).coerceAtLeast(0.5)
        val spanY = (maxY - minY).coerceAtLeast(0.5)

        return List(gridSize) { row ->
            val ny = minY + (row.toDouble() / (gridSize - 1)) * spanY
            List(gridSize) { col ->
                val nx = minX + (col.toDouble() / (gridSize - 1)) * spanX
                var weightedElev = 0.0
                var weightSum = 0.0
                for (wp in waypoints) {
                    val d2 = (nx - wp.dx).pow(2) + (ny - wp.dy).pow(2) + 0.035
                    val w = 1.0 / d2
                    weightedElev += wp.elevM * w
                    weightSum += w
                }
                val baseElev = if (weightSum > 0) weightedElev / weightSum else 120.0
                val ridgeHarmonic = 85.0 * sin(nx * 5.2 + 0.7) * cos(ny * 4.7 - 0.3) +
                    45.0 * sin((nx + ny) * 8.5) +
                    22.0 * cos((nx - ny) * 12.0)
                (baseElev + ridgeHarmonic).coerceAtLeast(10.0)
            }
        }
    }

    fun calculateElevationGainMeters(waypoints: List<Waypoint>): Int {
        if (waypoints.size < 2) return 0
        var gain = 0.0
        for (i in 1 until waypoints.size) {
            val diff = waypoints[i].elevM - waypoints[i - 1].elevM
            if (diff > 0) gain += diff
        }
        return gain.roundToInt()
    }

    fun rankTitleForDistance(totalKm: Double): String = when {
        totalKm >= 250.0 -> "Grand Lorewarden"
        totalKm >= 100.0 -> "Mythic Ranger"
        totalKm >= 40.0 -> "Fellowship Pathfinder"
        totalKm >= 15.0 -> "Trail Chronicler"
        totalKm >= 3.0 -> "Wandering Scribe"
        else -> "Shire Novice"
    }

    fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val latRad = Math.toRadians(clampedLat)
        val x = (((lon + 180.0) / 360.0) * n).toInt().coerceIn(0, n - 1)
        val y = (((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0) * n)
            .toInt().coerceIn(0, n - 1)
        return x to y
    }

    fun tileXYToLatLon(x: Int, y: Int, zoom: Int): Pair<Double, Double> {
        val n = 1 shl zoom
        val lon = x.toDouble() / n * 360.0 - 180.0
        val latRad = kotlin.math.atan(kotlin.math.sinh(PI * (1.0 - 2.0 * y.toDouble() / n)))
        val lat = Math.toDegrees(latRad)
        return lat to lon
    }

    private fun wrapLongitude(lon: Double): Double {
        var wrapped = lon
        while (wrapped > 180.0) wrapped -= 360.0
        while (wrapped < -180.0) wrapped += 360.0
        return wrapped
    }
}
