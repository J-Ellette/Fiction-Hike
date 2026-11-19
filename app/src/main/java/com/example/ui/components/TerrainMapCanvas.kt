package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.example.data.model.FictionalTrek
import com.example.data.model.GeoPoint
import com.example.data.model.GeoProjection
import com.example.data.model.ProjectedLoreDrop
import com.example.data.model.Waypoint
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun TerrainMapCanvas(
    trek: FictionalTrek,
    projectedWaypoints: List<GeoPoint>,
    projectedLoreDrops: List<ProjectedLoreDrop>,
    liveGpsLocation: GeoPoint?,
    anchorPoint: GeoPoint,
    scaleKm: Double,
    is3DMode: Boolean,
    isActivelyTrekking: Boolean,
    tileBytesCache: Map<String, ByteArray>,
    zoomLevel: Float,
    darkTheme: Boolean,
    onLoreDropTapped: (ProjectedLoreDrop) -> Unit,
    modifier: Modifier = Modifier
) {
    var azimuthDeg by remember { mutableFloatStateOf(32f) }
    var pitchDeg by remember { mutableFloatStateOf(38f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var loreScreenPositions by remember { mutableStateOf<List<Pair<Offset, ProjectedLoreDrop>>>(emptyList()) }

    val textMeasurer = rememberTextMeasurer()
    val pulseTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 12f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    val accentColor = remember(trek.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(trek.accentColorHex))
        } catch (_: Exception) {
            Color(0xFFE8B84B)
        }
    }

    val terrainGrid = remember(trek.id, trek.waypoints) {
        GeoProjection.generateTerrainGrid(trek.waypoints, gridSize = 18)
    }

    val decodedTiles = remember(tileBytesCache) {
        tileBytesCache.mapValuesNotNull { (_, bytes) ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    val bgBrush = if (darkTheme) {
        Brush.verticalGradient(
            listOf(Color(0xFF070E0C), Color(0xFF101D17), Color(0xFF0A1310))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFE9E2D0), Color(0xFFF5EFE0), Color(0xFFE3D9C3))
        )
    }

    val labelColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val emeraldColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .testTag("terrain_map_canvas")
            .pointerInput(is3DMode) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (is3DMode) {
                        azimuthDeg = (azimuthDeg + dragAmount.x * 0.35f) % 360f
                        pitchDeg = (pitchDeg - dragAmount.y * 0.25f).coerceIn(18f, 75f)
                    } else {
                        panOffset += dragAmount
                    }
                }
            }
            .pointerInput(projectedLoreDrops, is3DMode) {
                detectTapGestures { tapOffset ->
                    val hit = loreScreenPositions.minByOrNull { (pos, _) ->
                        hypot(pos.x - tapOffset.x, pos.y - tapOffset.y)
                    }
                    if (hit != null && hypot(hit.first.x - tapOffset.x, hit.first.y - tapOffset.y) <= 52f) {
                        onLoreDropTapped(hit.second)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hitTargets = mutableListOf<Pair<Offset, ProjectedLoreDrop>>()
            if (is3DMode) {
                draw3DTerrainScene(
                    trek = trek,
                    terrainGrid = terrainGrid,
                    projectedLoreDrops = projectedLoreDrops,
                    liveGpsLocation = liveGpsLocation,
                    anchorPoint = anchorPoint,
                    scaleKm = scaleKm,
                    azimuthDeg = azimuthDeg,
                    pitchDeg = pitchDeg,
                    zoomLevel = zoomLevel,
                    accentColor = accentColor,
                    emeraldColor = emeraldColor,
                    labelColor = labelColor,
                    pulseRadius = pulseRadius,
                    isActivelyTrekking = isActivelyTrekking,
                    darkTheme = darkTheme,
                    textMeasurer = textMeasurer,
                    hitTargets = hitTargets
                )
            } else {
                draw2DGpsOverlayScene(
                    trek = trek,
                    projectedWaypoints = projectedWaypoints,
                    projectedLoreDrops = projectedLoreDrops,
                    liveGpsLocation = liveGpsLocation,
                    anchorPoint = anchorPoint,
                    decodedTiles = decodedTiles,
                    panOffset = panOffset,
                    zoomLevel = zoomLevel,
                    accentColor = accentColor,
                    primaryColor = primaryColor,
                    emeraldColor = emeraldColor,
                    labelColor = labelColor,
                    pulseRadius = pulseRadius,
                    isActivelyTrekking = isActivelyTrekking,
                    darkTheme = darkTheme,
                    textMeasurer = textMeasurer,
                    hitTargets = hitTargets
                )
            }
            loreScreenPositions = hitTargets
        }
    }
}

private inline fun <K, V, R : Any> Map<K, V>.mapValuesNotNull(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val result = LinkedHashMap<K, R>()
    for (entry in this) {
        val v = transform(entry)
        if (v != null) result[entry.key] = v
    }
    return result
}

private fun DrawScope.draw3DTerrainScene(
    trek: FictionalTrek,
    terrainGrid: List<List<Double>>,
    projectedLoreDrops: List<ProjectedLoreDrop>,
    liveGpsLocation: GeoPoint?,
    anchorPoint: GeoPoint,
    scaleKm: Double,
    azimuthDeg: Float,
    pitchDeg: Float,
    zoomLevel: Float,
    accentColor: Color,
    emeraldColor: Color,
    labelColor: Color,
    pulseRadius: Float,
    isActivelyTrekking: Boolean,
    darkTheme: Boolean,
    textMeasurer: TextMeasurer,
    hitTargets: MutableList<Pair<Offset, ProjectedLoreDrop>>
) {
    val rows = terrainGrid.size
    val cols = terrainGrid.firstOrNull()?.size ?: return
    val minElev = terrainGrid.flatten().minOrNull() ?: 0.0
    val maxElev = (terrainGrid.flatten().maxOrNull() ?: 1000.0).coerceAtLeast(minElev + 50.0)

    val radAz = Math.toRadians(azimuthDeg.toDouble())
    val radPitch = Math.toRadians(pitchDeg.toDouble())
    val cosA = cos(radAz)
    val sinA = sin(radAz)
    val sinP = sin(radPitch)
    val cosP = cos(radPitch)

    val meshScale = (size.minDimension * 0.34f) * zoomLevel
    val center = Offset(size.width / 2f, size.height * 0.56f)

    fun project3D(nx: Double, ny: Double, elev: Double): Pair<Offset, Double> {
        val normElev = ((elev - minElev) / (maxElev - minElev)).coerceIn(0.0, 1.35)
        val rx = nx * cosA - ny * sinA
        val ry = nx * sinA + ny * cosA
        val depth = ry * cosP - normElev * 0.35 * sinP
        val screenX = center.x + (rx * meshScale).toFloat()
        val screenY = center.y + ((ry * sinP - normElev * 0.42 * cosP) * meshScale).toFloat()
        return Offset(screenX, screenY) to depth
    }

    // Build and depth-sort quads back-to-front
    data class TerrainQuad(
        val p00: Offset,
        val p10: Offset,
        val p11: Offset,
        val p01: Offset,
        val avgDepth: Double,
        val normElev: Double
    )

    val quads = ArrayList<TerrainQuad>((rows - 1) * (cols - 1))
    for (r in 0 until rows - 1) {
        for (c in 0 until cols - 1) {
            val nx0 = (c.toDouble() / (cols - 1)) * 2.0 - 1.0
            val nx1 = ((c + 1).toDouble() / (cols - 1)) * 2.0 - 1.0
            val ny0 = (r.toDouble() / (rows - 1)) * 2.0 - 1.0
            val ny1 = ((r + 1).toDouble() / (rows - 1)) * 2.0 - 1.0

            val e00 = terrainGrid[r][c]
            val e10 = terrainGrid[r][c + 1]
            val e11 = terrainGrid[r + 1][c + 1]
            val e01 = terrainGrid[r + 1][c]

            val (p00, d00) = project3D(nx0, ny0, e00)
            val (p10, d10) = project3D(nx1, ny0, e10)
            val (p11, d11) = project3D(nx1, ny1, e11)
            val (p01, d01) = project3D(nx0, ny1, e01)

            val avgE = (e00 + e10 + e11 + e01) / 4.0
            val normE = ((avgE - minElev) / (maxElev - minElev)).coerceIn(0.0, 1.0)
            quads.add(TerrainQuad(p00, p10, p11, p01, (d00 + d10 + d11 + d01) / 4.0, normE))
        }
    }

    quads.sortBy { it.avgDepth }

    val strokeGridColor = if (darkTheme) Color(0x337CE8BF) else Color(0x331E5E45)
    for (q in quads) {
        val fillColor = hypsometricColor(q.normElev, darkTheme)
        val path = Path().apply {
            moveTo(q.p00.x, q.p00.y)
            lineTo(q.p10.x, q.p10.y)
            lineTo(q.p11.x, q.p11.y)
            lineTo(q.p01.x, q.p01.y)
            close()
        }
        drawPath(path, color = fillColor)
        drawPath(path, color = strokeGridColor, style = Stroke(width = 1f))
    }

    // Map waypoint normalized coordinates into [-0.75, 0.75] mesh domain
    val wps = trek.waypoints
    if (wps.isNotEmpty()) {
        val minX = (wps.minOf { it.dx }) - 0.35
        val maxX = (wps.maxOf { it.dx }) + 0.35
        val minY = (wps.minOf { it.dy }) - 0.35
        val maxY = (wps.maxOf { it.dy }) + 0.35
        val spanX = (maxX - minX).coerceAtLeast(0.5)
        val spanY = (maxY - minY).coerceAtLeast(0.5)

        fun waypointTo3D(wp: Waypoint): Offset {
            val nx = ((wp.dx - minX) / spanX) * 2.0 - 1.0
            val ny = ((wp.dy - minY) / spanY) * 2.0 - 1.0
            return project3D(nx, ny, wp.elevM + 25.0).first
        }

        val routePoints = wps.map { waypointTo3D(it) }
        val trailPath = Path().apply {
            routePoints.forEachIndexed { i, pt ->
                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }
        // Soft trail glow
        drawPath(
            path = trailPath,
            color = accentColor.copy(alpha = 0.32f),
            style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = trailPath,
            color = accentColor,
            style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw waypoint pillars & labels
        wps.forEachIndexed { idx, wp ->
            val pt = routePoints[idx]
            drawCircle(color = accentColor, radius = 6f, center = pt)
            if (idx == 0 || idx == wps.lastIndex || wps.size <= 6) {
                val layout = textMeasurer.measure(
                    text = wp.label.take(18),
                    style = TextStyle(
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(pt.x - layout.size.width / 2f, pt.y - 24f)
                )
            }
        }

        // Lore Drop beacons on 3D surface
        projectedLoreDrops.forEach { drop ->
            val idx = drop.loreDrop.waypointIndex.coerceIn(0, wps.lastIndex)
            val basePt = routePoints[idx]
            val pinTop = Offset(basePt.x, basePt.y - 28f)
            val pinColor = when {
                drop.isUnlocked -> emeraldColor
                drop.isInTriggerRange -> Color(0xFFFFD54F)
                isActivelyTrekking -> Color(0xFFE8B84B)
                else -> Color(0xFF7B8B83)
            }
            drawLine(color = pinColor, start = basePt, end = pinTop, strokeWidth = 3f)
            if (drop.isInTriggerRange && !drop.isUnlocked) {
                drawCircle(color = pinColor.copy(alpha = 0.25f), radius = pulseRadius * 0.7f, center = pinTop)
            }
            drawCircle(color = pinColor, radius = 10f, center = pinTop)
            drawCircle(color = Color(0xFF0C1411), radius = 4f, center = pinTop)
            hitTargets.add(pinTop to drop)
        }

        // Project user's live GPS location onto the 3D terrain mesh
        if (liveGpsLocation != null) {
            val firstWp = wps.first()
            val normLen = GeoProjection.normalizedPathLength(wps)
            val metersPerUnit = (scaleKm.coerceAtLeast(0.2) / normLen) * 1000.0
            val latRad = Math.toRadians(anchorPoint.latitude).coerceIn(-1.45, 1.45)
            val northMeters = (liveGpsLocation.latitude - anchorPoint.latitude) * 111_320.0
            val eastMeters = (liveGpsLocation.longitude - anchorPoint.longitude) * (111_320.0 * cos(latRad))
            val userDx = firstWp.dx + eastMeters / metersPerUnit
            val userDy = firstWp.dy + northMeters / metersPerUnit
            val nx = (((userDx - minX) / spanX) * 2.0 - 1.0).coerceIn(-1.15, 1.15)
            val ny = (((userDy - minY) / spanY) * 2.0 - 1.0).coerceIn(-1.15, 1.15)
            val userPt = project3D(nx, ny, wps.first().elevM + 60.0).first

            drawCircle(color = emeraldColor.copy(alpha = 0.26f), radius = pulseRadius, center = userPt)
            drawCircle(color = Color.White, radius = 9f, center = userPt)
            drawCircle(color = emeraldColor, radius = 6f, center = userPt)
        }
    }
}

private fun hypsometricColor(normElev: Double, darkTheme: Boolean): Color {
    val t = normElev.toFloat().coerceIn(0f, 1f)
    return if (darkTheme) {
        when {
            t < 0.3f -> lerpColor(Color(0xFF102A22), Color(0xFF1E4838), t / 0.3f)
            t < 0.65f -> lerpColor(Color(0xFF1E4838), Color(0xFF3E5E4B), (t - 0.3f) / 0.35f)
            else -> lerpColor(Color(0xFF3E5E4B), Color(0xFFA28A5B), (t - 0.65f) / 0.35f)
        }
    } else {
        when {
            t < 0.3f -> lerpColor(Color(0xFFC3DEC9), Color(0xFF9AC5A6), t / 0.3f)
            t < 0.65f -> lerpColor(Color(0xFF9AC5A6), Color(0xFFD2C095), (t - 0.3f) / 0.35f)
            else -> lerpColor(Color(0xFFD2C095), Color(0xFFF3ECE0), (t - 0.65f) / 0.35f)
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = 1f
    )
}

private fun DrawScope.draw2DGpsOverlayScene(
    trek: FictionalTrek,
    projectedWaypoints: List<GeoPoint>,
    projectedLoreDrops: List<ProjectedLoreDrop>,
    liveGpsLocation: GeoPoint?,
    anchorPoint: GeoPoint,
    decodedTiles: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    panOffset: Offset,
    zoomLevel: Float,
    accentColor: Color,
    primaryColor: Color,
    emeraldColor: Color,
    labelColor: Color,
    pulseRadius: Float,
    isActivelyTrekking: Boolean,
    darkTheme: Boolean,
    textMeasurer: TextMeasurer,
    hitTargets: MutableList<Pair<Offset, ProjectedLoreDrop>>
) {
    val zoom = 14
    val (centerTx, centerTy) = GeoProjection.latLonToTileXY(anchorPoint.latitude, anchorPoint.longitude, zoom)
    val tileSizePx = (256f * zoomLevel).roundToInt().coerceAtLeast(120)
    val centerX = size.width / 2f + panOffset.x
    val centerY = size.height / 2f + panOffset.y

    // Draw cached/downloaded OSM tiles centered around anchor tile
    for (dx in -1..1) {
        for (dy in -1..1) {
            val tx = (centerTx + dx).coerceAtLeast(0)
            val ty = (centerTy + dy).coerceAtLeast(0)
            val bmp = decodedTiles["${zoom}_${tx}_$ty"]
            val left = (centerX + (dx - 0.5f) * tileSizePx).roundToInt()
            val top = (centerY + (dy - 0.5f) * tileSizePx).roundToInt()
            if (bmp != null) {
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(tileSizePx, tileSizePx),
                    alpha = if (darkTheme) 0.55f else 0.78f
                )
            }
        }
    }

    // Topographic graticule & contour rings for clear cartographic framing
    val gridColor = if (darkTheme) Color(0x283DD598) else Color(0x281E5E45)
    val step = 72f * zoomLevel.coerceIn(0.6f, 2f)
    var gx = centerX % step
    while (gx < size.width) {
        drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
        gx += step
    }
    var gy = centerY % step
    while (gy < size.height) {
        drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        gy += step
    }

    // Project Lat/Lon onto screen around anchorPoint
    val spanDegLat = 0.032 / zoomLevel.coerceIn(0.4f, 4.0f)
    val cosLat = cos(Math.toRadians(anchorPoint.latitude)).coerceAtLeast(0.2)
    val spanDegLon = spanDegLat / cosLat

    fun geoToScreen(lat: Double, lon: Double): Offset {
        val px = centerX + (((lon - anchorPoint.longitude) / spanDegLon) * size.minDimension * 0.85).toFloat()
        val py = centerY - (((lat - anchorPoint.latitude) / spanDegLat) * size.minDimension * 0.85).toFloat()
        return Offset(px, py)
    }

    val screenPoints = projectedWaypoints.map { geoToScreen(it.latitude, it.longitude) }
    if (screenPoints.size >= 2) {
        val routePath = Path().apply {
            screenPoints.forEachIndexed { idx, pt ->
                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }
        drawPath(
            path = routePath,
            color = accentColor.copy(alpha = 0.32f),
            style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = routePath,
            color = accentColor,
            style = Stroke(
                width = 5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
            )
        )
    }

    // Draw waypoints
    screenPoints.forEachIndexed { idx, pt ->
        drawCircle(color = primaryColor, radius = 7f, center = pt)
        drawCircle(color = Color(0xFF0C1411), radius = 3f, center = pt)
        val label = projectedWaypoints[idx].label
        if (label.isNotBlank() && (idx == 0 || idx == screenPoints.lastIndex || screenPoints.size <= 7)) {
            val layout = textMeasurer.measure(
                text = label.take(18),
                style = TextStyle(
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(pt.x + 10f, pt.y - 8f)
            )
        }
    }

    // Draw Lore Drop beacons
    projectedLoreDrops.forEach { drop ->
        val pt = geoToScreen(drop.coordinate.latitude, drop.coordinate.longitude)
        val badgeColor = when {
            drop.isUnlocked -> emeraldColor
            drop.isInTriggerRange -> Color(0xFFFFD54F)
            isActivelyTrekking -> accentColor
            else -> Color(0xFF6B7B74)
        }
        if (drop.isInTriggerRange && !drop.isUnlocked) {
            drawCircle(color = badgeColor.copy(alpha = 0.25f), radius = pulseRadius, center = pt)
        }
        drawCircle(color = badgeColor, radius = 12f, center = pt)
        drawCircle(color = Color(0xFF0C1411), radius = 5f, center = pt)
        hitTargets.add(pt to drop)
    }

    // Draw live GPS hiker beacon
    if (liveGpsLocation != null) {
        val userPt = geoToScreen(liveGpsLocation.latitude, liveGpsLocation.longitude)
        drawCircle(color = emeraldColor.copy(alpha = 0.28f), radius = pulseRadius, center = userPt)
        drawCircle(color = Color.White, radius = 10f, center = userPt)
        drawCircle(color = emeraldColor, radius = 6.5f, center = userPt)
    }
}
