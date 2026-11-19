package com.example.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CuratedTreksCatalog
import com.example.data.local.OfflineRegionEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.data.model.ActiveTrekProgress
import com.example.data.model.FictionalTrek
import com.example.data.model.GeoPoint
import com.example.data.model.GeoProjection
import com.example.data.model.JourneyLogDoc
import com.example.data.model.LeaderboardEntryDoc
import com.example.data.model.ProjectedLoreDrop
import com.example.data.model.TrekkerProfile
import com.example.data.remote.FictionHikeRepository
import com.example.data.remote.OfflineTileManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveHikeState(
    val selectedTrek: FictionalTrek = CuratedTreksCatalog.treks.first(),
    val isActivelyTrekking: Boolean = false,
    val liveGpsLocation: GeoPoint? = null,
    val anchorPoint: GeoPoint = GeoPoint(37.7749, -122.4194, 45.0, "Trailhead Anchor"),
    val hasLiveGpsFix: Boolean = false,
    val scaleKm: Double = 3.0,
    val distanceHikedKm: Double = 0.0,
    val currentWaypointIndex: Int = 0,
    val unlockedLoreDropIds: Set<String> = emptySet(),
    val triggeredLoreDrop: ProjectedLoreDrop? = null,
    val is3DMode: Boolean = true,
    val downloadProgress: Float? = null
)

class FictionHikeViewModel(
    private val repository: FictionHikeRepository,
    private val offlineTileManager: OfflineTileManager,
    private val settingsDataStore: SettingsDataStore,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _hikeState = MutableStateFlow(ActiveHikeState())
    val hikeState: StateFlow<ActiveHikeState> = _hikeState.asStateFlow()

    private val _tileBytesCache = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val tileBytesCache: StateFlow<Map<String, ByteArray>> = _tileBytesCache.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var previousGpsFix: Location? = null

    val userSettings: StateFlow<UserSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserSettings()
        )

    val userProfileState: StateFlow<UiState<TrekkerProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("Failed to load trekker profile")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val activeProgressList: StateFlow<List<ActiveTrekProgress>> =
        repository.observeActiveProgress(currentUserId)
            .catch { error ->
                unwrapAndThrowIfCancellation(error, "Failed to observe progress")
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val allTreksState: StateFlow<UiState<List<FictionalTrek>>> =
        repository.observeCommunityTreks()
            .map<List<FictionalTrek>, UiState<List<FictionalTrek>>> { community ->
                UiState.Success(CuratedTreksCatalog.treks + community)
            }
            .catch { error ->
                val root = unwrapAndThrowIfCancellation(error, "Failed to load community treks")
                Log.w(TAG, "Error observing community treks", root)
                // Keep curated treks usable even if offline or stream errors
                emit(UiState.Success(CuratedTreksCatalog.treks))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Success(CuratedTreksCatalog.treks)
            )

    val communityLogsState: StateFlow<UiState<List<JourneyLogDoc>>> =
        repository.observeCommunityLogs()
            .toUiStateFlow("Failed to load community journey logs")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myLogsState: StateFlow<UiState<List<JourneyLogDoc>>> =
        repository.observeMyLogs(currentUserId)
            .toUiStateFlow("Failed to load your journey logs")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntryDoc>>> =
        repository.observeLeaderboard()
            .toUiStateFlow("Failed to load activity leaderboard")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val offlineRegions: StateFlow<List<OfflineRegionEntity>> =
        offlineTileManager.downloadedRegions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val projectedWaypoints: StateFlow<List<GeoPoint>> =
        _hikeState.map { state ->
            GeoProjection.projectWaypoints(
                waypoints = state.selectedTrek.waypoints,
                anchorLat = state.anchorPoint.latitude,
                anchorLon = state.anchorPoint.longitude,
                scaleKm = state.scaleKm
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val projectedLoreDrops: StateFlow<List<ProjectedLoreDrop>> =
        combine(_hikeState, projectedWaypoints) { state, waypoints ->
            GeoProjection.projectLoreDrops(
                trek = state.selectedTrek,
                projectedWaypoints = waypoints,
                userLat = state.liveGpsLocation?.latitude,
                userLon = state.liveGpsLocation?.longitude,
                unlockedIds = state.unlockedLoreDropIds,
                isActivelyTrekking = state.isActivelyTrekking
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfileAndLeaderboard(displayNameHint = initialDisplayName)
        }
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _hikeState.value = _hikeState.value.copy(scaleKm = settings.preferredScaleKm)
            }
        }
        refreshVisibleTiles(_hikeState.value.anchorPoint.latitude, _hikeState.value.anchorPoint.longitude)
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapAndThrowIfCancellation(error, fallbackMessage)
                Log.w(TAG, fallbackMessage, root)
                emit(UiState.Error(root.message ?: fallbackMessage))
            }

    private fun unwrapAndThrowIfCancellation(error: Throwable, contextMsg: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    fun selectTrek(trek: FictionalTrek, restoreProgress: ActiveTrekProgress? = null) {
        val currentAnchor = _hikeState.value.liveGpsLocation ?: _hikeState.value.anchorPoint
        val restoredAnchor = if (restoreProgress != null) {
            GeoPoint(restoreProgress.anchorLat, restoreProgress.anchorLon, 0.0, "Saved Anchor")
        } else {
            currentAnchor
        }
        _hikeState.value = _hikeState.value.copy(
            selectedTrek = trek,
            isActivelyTrekking = restoreProgress?.status == "active",
            anchorPoint = restoredAnchor,
            scaleKm = restoreProgress?.scaleKm ?: _hikeState.value.scaleKm,
            distanceHikedKm = restoreProgress?.distanceCoveredKm ?: 0.0,
            currentWaypointIndex = restoreProgress?.currentWaypointIndex?.toInt() ?: 0,
            unlockedLoreDropIds = restoreProgress?.unlockedLoreDropIds?.toSet() ?: emptySet(),
            triggeredLoreDrop = null
        )
        previousGpsFix = null
        refreshVisibleTiles(restoredAnchor.latitude, restoredAnchor.longitude)
    }

    fun startActiveTrek() {
        val state = _hikeState.value
        val anchor = state.liveGpsLocation ?: state.anchorPoint
        previousGpsFix = null
        _hikeState.value = state.copy(
            isActivelyTrekking = true,
            anchorPoint = anchor,
            triggeredLoreDrop = null
        )
        persistActiveProgress(status = "active")
        checkLoreDropProximity()
        _snackbarMessage.value = "Trek started as ${state.selectedTrek.characterName}. Reach Lore Drop GPS pins to unlock!"
    }

    fun pauseOrStopActiveTrek() {
        val state = _hikeState.value
        _hikeState.value = state.copy(isActivelyTrekking = false, triggeredLoreDrop = null)
        previousGpsFix = null
        persistActiveProgress(status = "active")
    }

    fun completeActiveTrek() {
        val state = _hikeState.value
        _hikeState.value = state.copy(isActivelyTrekking = false)
        persistActiveProgress(status = "completed")
        viewModelScope.launch {
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            repository.syncCumulativeStats(
                displayName = profileName,
                addedDistanceKm = 0.0,
                addedTreksCompleted = 1,
                addedLoreDrops = 0,
                activeTrekTitle = state.selectedTrek.title,
                activeCharacter = state.selectedTrek.characterName
            )
            _snackbarMessage.value = "${state.selectedTrek.title} marked complete!"
        }
    }

    fun anchorOverlayAtCurrentGps() {
        val live = _hikeState.value.liveGpsLocation
        if (live == null) {
            _snackbarMessage.value = "Waiting for GPS lock..."
            return
        }
        _hikeState.value = _hikeState.value.copy(anchorPoint = live)
        refreshVisibleTiles(live.latitude, live.longitude)
        if (_hikeState.value.isActivelyTrekking) {
            persistActiveProgress("active")
        }
        _snackbarMessage.value = "Fictional trail anchored to your current GPS position."
    }

    fun updateOverlayScale(scaleKm: Double) {
        viewModelScope.launch {
            settingsDataStore.setPreferredScaleKm(scaleKm)
        }
        _hikeState.value = _hikeState.value.copy(scaleKm = scaleKm)
        if (_hikeState.value.isActivelyTrekking) {
            persistActiveProgress("active")
        }
    }

    fun toggle3DMode(enabled: Boolean) {
        _hikeState.value = _hikeState.value.copy(is3DMode = enabled)
    }

    fun onNewGpsLocation(location: Location) {
        val geo = GeoPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            elevationM = if (location.hasAltitude()) location.altitude else 50.0,
            label = "Current GPS"
        )
        val prevState = _hikeState.value
        val firstFix = !prevState.hasLiveGpsFix
        val newAnchor = if (firstFix && !prevState.isActivelyTrekking) geo else prevState.anchorPoint

        var addedKm = 0.0
        if (prevState.isActivelyTrekking) {
            val prev = previousGpsFix
            if (prev != null) {
                val deltaMeters = prev.distanceTo(location).toDouble()
                if (deltaMeters in 2.0..1500.0) {
                    addedKm = deltaMeters / 1000.0
                }
            }
            previousGpsFix = location
        }

        val updatedDist = prevState.distanceHikedKm + addedKm
        val waypoints = GeoProjection.projectWaypoints(
            prevState.selectedTrek.waypoints,
            newAnchor.latitude,
            newAnchor.longitude,
            prevState.scaleKm
        )
        val nearestWpIdx = waypoints.indices.minByOrNull { idx ->
            GeoProjection.distanceMeters(
                geo.latitude,
                geo.longitude,
                waypoints[idx].latitude,
                waypoints[idx].longitude
            )
        } ?: prevState.currentWaypointIndex

        _hikeState.value = prevState.copy(
            liveGpsLocation = geo,
            hasLiveGpsFix = true,
            anchorPoint = newAnchor,
            distanceHikedKm = updatedDist,
            currentWaypointIndex = maxOf(prevState.currentWaypointIndex, nearestWpIdx)
        )

        if (firstFix) {
            refreshVisibleTiles(newAnchor.latitude, newAnchor.longitude)
        }

        if (addedKm > 0.0) {
            persistActiveProgress("active")
            viewModelScope.launch {
                val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
                repository.syncCumulativeStats(
                    displayName = profileName,
                    addedDistanceKm = addedKm,
                    addedTreksCompleted = 0,
                    addedLoreDrops = 0,
                    activeTrekTitle = prevState.selectedTrek.title,
                    activeCharacter = prevState.selectedTrek.characterName
                )
            }
        }

        checkLoreDropProximity()
    }

    private fun checkLoreDropProximity() {
        val state = _hikeState.value
        if (!state.isActivelyTrekking) return
        val live = state.liveGpsLocation ?: return

        val projectedWps = GeoProjection.projectWaypoints(
            state.selectedTrek.waypoints,
            state.anchorPoint.latitude,
            state.anchorPoint.longitude,
            state.scaleKm
        )
        val drops = GeoProjection.projectLoreDrops(
            trek = state.selectedTrek,
            projectedWaypoints = projectedWps,
            userLat = live.latitude,
            userLon = live.longitude,
            unlockedIds = state.unlockedLoreDropIds,
            isActivelyTrekking = true
        )

        val newlyTriggered = drops.firstOrNull { it.isInTriggerRange && !it.isUnlocked }
        if (newlyTriggered != null) {
            val newUnlocked = state.unlockedLoreDropIds + newlyTriggered.loreDrop.id
            _hikeState.value = state.copy(
                unlockedLoreDropIds = newUnlocked,
                triggeredLoreDrop = newlyTriggered.copy(isUnlocked = true)
            )
            persistActiveProgress("active")
            viewModelScope.launch {
                val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
                repository.syncCumulativeStats(
                    displayName = profileName,
                    addedDistanceKm = 0.0,
                    addedTreksCompleted = 0,
                    addedLoreDrops = 1,
                    activeTrekTitle = state.selectedTrek.title,
                    activeCharacter = state.selectedTrek.characterName
                )
            }
        }
    }

    fun inspectLoreDrop(drop: ProjectedLoreDrop) {
        val state = _hikeState.value
        if (!state.isActivelyTrekking) {
            _snackbarMessage.value = "Start this trek first! Lore Drops unlock only when actively hiking to their GPS coordinates."
            return
        }
        if (!drop.isUnlocked && !drop.isInTriggerRange) {
            val distText = if (drop.distanceMetersFromUser < 10_000) {
                "${drop.distanceMetersFromUser.toInt()}m away"
            } else {
                "out of range"
            }
            _snackbarMessage.value = "Locked Lore Drop (${distText}). Walk within ${GeoProjection.LORE_TRIGGER_RADIUS_METERS.toInt()}m to reveal!"
            return
        }
        _hikeState.value = state.copy(triggeredLoreDrop = drop)
    }

    fun dismissLoreDropSheet() {
        _hikeState.value = _hikeState.value.copy(triggeredLoreDrop = null)
    }

    fun logMilestoneProgress(addedKm: Double, note: String, moodTag: String, shareWithCommunity: Boolean) {
        val state = _hikeState.value
        val trek = state.selectedTrek
        val safeAdded = addedKm.coerceIn(0.0, 500.0)
        val newDistance = state.distanceHikedKm + safeAdded
        val completed = newDistance >= state.scaleKm

        _hikeState.value = state.copy(distanceHikedKm = newDistance)
        persistActiveProgress(if (completed) "completed" else "active")

        viewModelScope.launch {
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val logResult = repository.publishJourneyLog(
                authorName = profileName,
                trek = trek,
                distanceKm = if (safeAdded > 0.0) safeAdded else newDistance,
                elevationGainM = trek.elevationGainM,
                loreDropsUnlocked = state.unlockedLoreDropIds.size,
                note = note,
                moodTag = moodTag,
                isPublished = shareWithCommunity
            )
            repository.syncCumulativeStats(
                displayName = profileName,
                addedDistanceKm = safeAdded,
                addedTreksCompleted = if (completed) 1 else 0,
                addedLoreDrops = 0,
                activeTrekTitle = trek.title,
                activeCharacter = trek.characterName
            )
            logResult.onSuccess {
                _snackbarMessage.value = if (shareWithCommunity) {
                    "Journey log published to the community feed!"
                } else {
                    "Journey log saved privately."
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save journey log."
            }
        }
    }

    fun publishCustomTrek(trek: FictionalTrek, onSuccess: (FictionalTrek) -> Unit) {
        viewModelScope.launch {
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val readyTrek = trek.copy(creatorName = profileName, creatorId = currentUserId)
            repository.publishCommunityTrek(readyTrek)
                .onSuccess { createdId ->
                    val saved = readyTrek.copy(id = createdId)
                    selectTrek(saved)
                    _snackbarMessage.value = "Custom trek '${saved.title}' shared with the community!"
                    onSuccess(saved)
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to publish custom trek."
                }
        }
    }

    fun downloadOfflineRegion(trek: FictionalTrek) {
        val anchor = _hikeState.value.anchorPoint
        val scale = _hikeState.value.scaleKm
        viewModelScope.launch {
            _hikeState.value = _hikeState.value.copy(downloadProgress = 0.05f)
            offlineTileManager.downloadTrekRegion(
                trek = trek,
                anchorLat = anchor.latitude,
                anchorLon = anchor.longitude,
                scaleKm = scale,
                onProgress = { pct ->
                    _hikeState.value = _hikeState.value.copy(downloadProgress = pct)
                }
            ).onSuccess { region ->
                _hikeState.value = _hikeState.value.copy(downloadProgress = null)
                refreshVisibleTiles(anchor.latitude, anchor.longitude)
                val kb = (region.sizeBytes / 1024L).coerceAtLeast(1L)
                _snackbarMessage.value = "Offline map cached for ${trek.title} (${region.tileCount} tiles, ${kb} KB)."
            }.onFailure { err ->
                _hikeState.value = _hikeState.value.copy(downloadProgress = null)
                _snackbarMessage.value = err.localizedMessage ?: "Offline download failed."
            }
        }
    }

    fun deleteOfflineRegion(trekId: String) {
        viewModelScope.launch {
            offlineTileManager.deleteRegion(trekId)
            _snackbarMessage.value = "Offline map pack removed."
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDarkMode(enabled) }
    }

    fun setOfflineModeOnly(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setOfflineMode(enabled) }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    private fun persistActiveProgress(status: String) {
        val state = _hikeState.value
        viewModelScope.launch {
            repository.saveActiveTrekProgress(
                trek = state.selectedTrek,
                status = status,
                distanceCoveredKm = state.distanceHikedKm,
                targetDistanceKm = state.scaleKm,
                currentWaypointIndex = state.currentWaypointIndex,
                unlockedLoreDropIds = state.unlockedLoreDropIds.toList(),
                anchorLat = state.anchorPoint.latitude,
                anchorLon = state.anchorPoint.longitude,
                scaleKm = state.scaleKm
            )
        }
    }

    private fun refreshVisibleTiles(lat: Double, lon: Double, zoom: Int = 14) {
        viewModelScope.launch {
            val allowNet = !userSettings.value.offlineModeOnly
            val (cx, cy) = GeoProjection.latLonToTileXY(lat, lon, zoom)
            val newMap = _tileBytesCache.value.toMutableMap()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val tx = (cx + dx).coerceAtLeast(0)
                    val ty = (cy + dy).coerceAtLeast(0)
                    val key = "${zoom}_${tx}_$ty"
                    if (!newMap.containsKey(key)) {
                        val bytes = offlineTileManager.getTileBytes(zoom, tx, ty, allowNet)
                        if (bytes != null) {
                            newMap[key] = bytes
                        }
                    }
                }
            }
            _tileBytesCache.value = newMap
        }
    }

    companion object {
        private const val TAG = "FictionHikeVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
