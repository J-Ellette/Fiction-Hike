package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.location.GpsLocationTracker
import com.example.ui.components.LogJourneyMilestoneSheet
import com.example.ui.components.LoreDropDiscoverySheet
import com.example.ui.components.SocialShareHelper
import com.example.ui.components.TerrainMapCanvas
import com.example.viewmodel.FictionHikeViewModel

@Composable
fun ExploreMapScreen(
    viewModel: FictionHikeViewModel,
    darkTheme: Boolean,
    onOpenTreksPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hikeState by viewModel.hikeState.collectAsStateWithLifecycle()
    val projectedWaypoints by viewModel.projectedWaypoints.collectAsStateWithLifecycle()
    val projectedLoreDrops by viewModel.projectedLoreDrops.collectAsStateWithLifecycle()
    val tileBytesCache by viewModel.tileBytesCache.collectAsStateWithLifecycle()
    val offlineRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val gpsTracker = remember { GpsLocationTracker(context) }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                gpsTracker.observeLocationUpdates().collect { loc ->
                    viewModel.onNewGpsLocation(loc)
                }
            } catch (_: SecurityException) {
                hasLocationPermission = false
            }
        }
    }

    var mapZoom by remember { mutableFloatStateOf(1.1f) }
    var showLogMilestoneSheet by remember { mutableStateOf(false) }

    val trek = hikeState.selectedTrek
    val isOfflineDownloaded = offlineRegions.any { it.trekId == trek.id }
    val scaleOptions = listOf(0.5, 2.0, 5.0, 15.0)

    val nearestLockedLore = projectedLoreDrops
        .filter { !it.isUnlocked }
        .minByOrNull { it.distanceMetersFromUser }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-bleed 3D/2D Interactive Topographic GPS Canvas
        TerrainMapCanvas(
            trek = trek,
            projectedWaypoints = projectedWaypoints,
            projectedLoreDrops = projectedLoreDrops,
            liveGpsLocation = hikeState.liveGpsLocation,
            anchorPoint = hikeState.anchorPoint,
            scaleKm = hikeState.scaleKm,
            is3DMode = hikeState.is3DMode,
            isActivelyTrekking = hikeState.isActivelyTrekking,
            tileBytesCache = tileBytesCache,
            zoomLevel = mapZoom,
            darkTheme = darkTheme,
            onLoreDropTapped = { drop -> viewModel.inspectLoreDrop(drop) }
        )

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${trek.universe.uppercase()} • ${trek.characterName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = trek.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = { viewModel.toggle3DMode(!hikeState.is3DMode) },
                            modifier = Modifier.testTag("toggle_3d_mode_button")
                        ) {
                            Icon(
                                imageVector = if (hikeState.is3DMode) Icons.Default.Terrain else Icons.Default.Map,
                                contentDescription = if (hikeState.is3DMode) "Switch to 2D GPS Map" else "Switch to 3D Topographic Terrain"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.downloadOfflineRegion(trek) },
                            modifier = Modifier.testTag("download_offline_map_button")
                        ) {
                            Icon(
                                imageVector = if (isOfflineDownloaded) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                contentDescription = "Download offline map pack",
                                tint = if (isOfflineDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onOpenTreksPicker,
                            modifier = Modifier.testTag("switch_trek_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Choose fictional trek or character"
                            )
                        }
                    }
                }
            }

            if (hikeState.downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { hikeState.downloadProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Scale & Location Permission Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                scaleOptions.forEach { km ->
                    val label = if (km < 1.0) "${(km * 1000).toInt()}m" else "${km.toInt()} km"
                    FilterChip(
                        selected = hikeState.scaleKm == km,
                        onClick = { viewModel.updateOverlayScale(km) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("scale_chip_${km}")
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!hasLocationPermission) {
                    AssistChip(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        label = { Text("Enable GPS") },
                        leadingIcon = { Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Right-side Zoom & Anchor GPS controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { mapZoom = (mapZoom * 1.25f).coerceAtMost(3.2f) },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            SmallFloatingActionButton(
                onClick = { mapZoom = (mapZoom / 1.25f).coerceAtLeast(0.55f) },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }
            SmallFloatingActionButton(
                onClick = { viewModel.anchorOverlayAtCurrentGps() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.testTag("anchor_gps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Anchor fictional trek at my current GPS location"
                )
            }
        }

        // Bottom Expedition Telemetry & Lore Drop Deck
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (hikeState.isActivelyTrekking) "ACTIVE GPS EXPEDITION" else "OVERLAY PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hikeState.isActivelyTrekking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.2f".format(hikeState.distanceHikedKm)} / ${"%.1f".format(hikeState.scaleKm)} km • ${hikeState.unlockedLoreDropIds.size}/${trek.loreDrops.size} Lore Drops",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val live = hikeState.liveGpsLocation
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (live != null) {
                                "${"%.4f".format(live.latitude)}, ${"%.4f".format(live.longitude)}"
                            } else {
                                "GPS Searching..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                val progressFraction = (hikeState.distanceHikedKm / hikeState.scaleKm.coerceAtLeast(0.1))
                    .toFloat()
                    .coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                )

                // Lore Drop Proximity Pill Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(projectedLoreDrops, key = { it.loreDrop.id }) { drop ->
                        val distLabel = when {
                            drop.isUnlocked -> "Unlocked"
                            !hikeState.isActivelyTrekking -> "Start trek to unlock"
                            drop.distanceMetersFromUser < 10_000 -> "${drop.distanceMetersFromUser.toInt()}m"
                            else -> "Locked"
                        }
                        Surface(
                            onClick = { viewModel.inspectLoreDrop(drop) },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                drop.isUnlocked -> MaterialTheme.colorScheme.secondaryContainer
                                drop.isInTriggerRange -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.testTag("lore_drop_chip_${drop.loreDrop.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (drop.isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (drop.isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${drop.loreDrop.title} ($distLabel)",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Nearest Lore Drop hint if active
                AnimatedVisibility(visible = hikeState.isActivelyTrekking && nearestLockedLore != null) {
                    val distM = nearestLockedLore?.distanceMetersFromUser ?: 0.0
                    val hintText = if (distM < 25_000) {
                        "Next Lore Drop '${nearestLockedLore?.loreDrop?.title}' is ${distM.toInt()}m away. Reach within 55m to reveal."
                    } else {
                        "Tap the GPS anchor icon to center '${trek.title}' on your live coordinates."
                    }
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (hikeState.isActivelyTrekking) viewModel.pauseOrStopActiveTrek()
                            else viewModel.startActiveTrek()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hikeState.isActivelyTrekking) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("start_pause_trek_button")
                    ) {
                        Icon(
                            imageVector = if (hikeState.isActivelyTrekking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (hikeState.isActivelyTrekking) "Pause" else "Start Trek")
                    }

                    OutlinedButton(
                        onClick = { showLogMilestoneSheet = true },
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("log_progress_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Log")
                    }

                    FilledTonalIconButton(
                        onClick = {
                            SocialShareHelper.shareTrekProgress(
                                context = context,
                                trek = trek,
                                distanceKm = hikeState.distanceHikedKm,
                                loreCount = hikeState.unlockedLoreDropIds.size
                            )
                        },
                        modifier = Modifier.testTag("share_progress_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share trek progress on social media")
                    }
                }
            }
        }
    }

    // Automatic or tapped Lore Drop interactive sheet
    hikeState.triggeredLoreDrop?.let { drop ->
        LoreDropDiscoverySheet(
            trek = trek,
            projectedDrop = drop,
            onDismiss = { viewModel.dismissLoreDropSheet() },
            onLogDiscovery = { note ->
                viewModel.logMilestoneProgress(
                    addedKm = 0.0,
                    note = note,
                    moodTag = "Lore Hunter",
                    shareWithCommunity = true
                )
            }
        )
    }

    if (showLogMilestoneSheet) {
        LogJourneyMilestoneSheet(
            trek = trek,
            currentDistanceKm = hikeState.distanceHikedKm,
            unlockedLoreCount = hikeState.unlockedLoreDropIds.size,
            onDismiss = { showLogMilestoneSheet = false },
            onSubmitLog = { addedKm, note, mood, publish ->
                viewModel.logMilestoneProgress(addedKm, note, mood, publish)
            }
        )
    }
}
