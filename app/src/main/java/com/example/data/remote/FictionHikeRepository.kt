package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.ActiveTrekProgress
import com.example.data.model.CommunityTrekDoc
import com.example.data.model.FictionalTrek
import com.example.data.model.GeoProjection
import com.example.data.model.JourneyLogDoc
import com.example.data.model.LeaderboardEntryDoc
import com.example.data.model.TrekkerProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FictionHikeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "id_${UUID.randomUUID().toString().replace("-", "")}" }
    }

    private fun <T> Flow<T>.wrapFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<TrekkerProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Profile stream mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        TrekkerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .wrapFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeActiveProgress(userId: String): Flow<List<ActiveTrekProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Active progress stream mismatch" }
        val path = "users/$uid/active_progress"
        emitAll(
            db.collection("users").document(uid).collection("active_progress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActiveTrekProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeCommunityTreks(): Flow<List<FictionalTrek>> = flow {
        val uid = requireUserId()
        val path = "community_treks"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityTrekDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toFictionalTrek() }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeCommunityLogs(): Flow<List<JourneyLogDoc>> = flow {
        val uid = requireUserId()
        val path = "journey_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JourneyLogDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyLogs(userId: String): Flow<List<JourneyLogDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "My logs stream mismatch" }
        val path = "journey_logs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JourneyLogDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntryDoc>> = flow {
        val uid = requireUserId()
        val path = "leaderboard_entries"
        emitAll(
            db.collection(path)
                .whereEqualTo("activeOnBoard", true)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntryDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.totalDistanceKm }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun ensureUserProfileAndLeaderboard(
        displayNameHint: String,
        favoriteUniverse: String = "Lord of the Rings"
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val safeName = displayNameHint.trim().ifBlank { "Trail Explorer" }.take(50)
        val safeUniverse = favoriteUniverse.trim().ifBlank { "Lord of the Rings" }.take(50)
        val userRef = db.collection("users").document(uid)
        val boardRef = db.collection("leaderboard_entries").document(uid)

        return try {
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "favoriteUniverse" to safeUniverse,
                        "totalDistanceKm" to 0.0,
                        "treksCompleted" to 0L,
                        "loreDropsFound" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val boardSnap = boardRef.get().await()
            if (!boardSnap.exists()) {
                boardRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "rankTitle" to GeoProjection.rankTitleForDistance(0.0),
                        "activeTrekTitle" to "",
                        "activeCharacter" to "",
                        "totalDistanceKm" to 0.0,
                        "treksCompleted" to 0L,
                        "loreDropsFound" to 0L,
                        "activeOnBoard" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveActiveTrekProgress(
        trek: FictionalTrek,
        status: String,
        distanceCoveredKm: Double,
        targetDistanceKm: Double,
        currentWaypointIndex: Int,
        unlockedLoreDropIds: List<String>,
        anchorLat: Double,
        anchorLon: Double,
        scaleKm: Double
    ): Result<Unit> {
        val safeTrekId = sanitizeId(trek.id)
        var path = "users/unauthenticated/active_progress/$safeTrekId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/active_progress/$safeTrekId"
            val docRef = db.collection("users").document(uid)
                .collection("active_progress").document(safeTrekId)

            val existing = docRef.get().await()
            val validStatus = if (status == "completed") "completed" else "active"
            val clampedCovered = distanceCoveredKm.coerceIn(0.0, 5000.0)
            val clampedTarget = targetDistanceKm.coerceIn(0.1, 5000.0)
            val clampedScale = scaleKm.coerceIn(0.1, 5000.0)
            val clampedWp = currentWaypointIndex.coerceIn(0, 100).toLong()
            val boundedLore = unlockedLoreDropIds.distinct().take(50)

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "trekTitle" to trek.title.take(80),
                        "universe" to trek.universe.take(50),
                        "characterName" to trek.characterName.take(50),
                        "status" to validStatus,
                        "distanceCoveredKm" to clampedCovered,
                        "targetDistanceKm" to clampedTarget,
                        "currentWaypointIndex" to clampedWp,
                        "unlockedLoreDropIds" to boundedLore,
                        "anchorLat" to anchorLat.coerceIn(-90.0, 90.0),
                        "anchorLon" to anchorLon.coerceIn(-180.0, 180.0),
                        "scaleKm" to clampedScale,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "trekId" to safeTrekId,
                        "trekTitle" to trek.title.take(80),
                        "universe" to trek.universe.take(50),
                        "characterName" to trek.characterName.take(50),
                        "status" to validStatus,
                        "distanceCoveredKm" to clampedCovered,
                        "targetDistanceKm" to clampedTarget,
                        "currentWaypointIndex" to clampedWp,
                        "unlockedLoreDropIds" to boundedLore,
                        "anchorLat" to anchorLat.coerceIn(-90.0, 90.0),
                        "anchorLon" to anchorLon.coerceIn(-180.0, 180.0),
                        "scaleKm" to clampedScale,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Keep active trek indicator synced on leaderboard
            val boardRef = db.collection("leaderboard_entries").document(uid)
            val boardSnap = boardRef.get().await()
            if (boardSnap.exists()) {
                boardRef.update(
                    mapOf(
                        "activeTrekTitle" to trek.title.take(80),
                        "activeCharacter" to trek.characterName.take(50),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncCumulativeStats(
        displayName: String,
        addedDistanceKm: Double,
        addedTreksCompleted: Int,
        addedLoreDrops: Int,
        activeTrekTitle: String,
        activeCharacter: String
    ): Result<Unit> {
        var path = "leaderboard_entries/unauthenticated"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "Trail Explorer" }.take(50)
            val userRef = db.collection("users").document(uid)
            val boardRef = db.collection("leaderboard_entries").document(uid)
            path = boardRef.path

            val userSnap = userRef.get().await()
            val currentDist = (userSnap.getDouble("totalDistanceKm") ?: 0.0)
            val currentCompleted = (userSnap.getLong("treksCompleted") ?: 0L)
            val currentLore = (userSnap.getLong("loreDropsFound") ?: 0L)

            val newDist = (currentDist + addedDistanceKm.coerceAtLeast(0.0)).coerceIn(0.0, 100000.0)
            val newCompleted = (currentCompleted + addedTreksCompleted.coerceAtLeast(0)).coerceIn(0L, 5000L)
            val newLore = (currentLore + addedLoreDrops.coerceAtLeast(0)).coerceIn(0L, 10000L)
            val rankTitle = GeoProjection.rankTitleForDistance(newDist)

            if (userSnap.exists()) {
                userRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "totalDistanceKm" to newDist,
                        "treksCompleted" to newCompleted,
                        "loreDropsFound" to newLore,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val boardSnap = boardRef.get().await()
            if (boardSnap.exists()) {
                boardRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "rankTitle" to rankTitle,
                        "activeTrekTitle" to activeTrekTitle.take(80),
                        "activeCharacter" to activeCharacter.take(50),
                        "totalDistanceKm" to newDist,
                        "treksCompleted" to newCompleted,
                        "loreDropsFound" to newLore,
                        "activeOnBoard" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                boardRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "rankTitle" to rankTitle,
                        "activeTrekTitle" to activeTrekTitle.take(80),
                        "activeCharacter" to activeCharacter.take(50),
                        "totalDistanceKm" to newDist,
                        "treksCompleted" to newCompleted,
                        "loreDropsFound" to newLore,
                        "activeOnBoard" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishCommunityTrek(trek: FictionalTrek): Result<String> {
        var path = "community_treks"
        return try {
            val uid = requireUserId()
            require(trek.title.trim().length in 2..80) { "Trek title must be between 2 and 80 characters." }
            require(trek.universe.trim().length in 2..50) { "Universe name must be between 2 and 50 characters." }
            require(trek.characterName.trim().length in 2..50) { "Character name must be between 2 and 50 characters." }
            require(trek.waypoints.size in 2..60) { "Plot between 2 and 60 route waypoints." }

            val trekId = sanitizeId("custom_${uid.take(8)}_${System.currentTimeMillis()}")
            val docRef = db.collection("community_treks").document(trekId)
            path = docRef.path

            val payload = mapOf(
                "creatorId" to uid,
                "creatorName" to trek.creatorName.trim().ifBlank { "Trailblazer" }.take(50),
                "title" to trek.title.trim().take(80),
                "universe" to trek.universe.trim().take(50),
                "characterName" to trek.characterName.trim().take(50),
                "description" to trek.description.trim().take(500),
                "difficulty" to trek.difficulty.name,
                "estimatedKm" to trek.estimatedKm.coerceIn(0.1, 5000.0),
                "elevationGainM" to trek.elevationGainM.coerceIn(0, 50000).toLong(),
                "accentColorHex" to trek.accentColorHex.take(9).ifBlank { "#E8B84B" },
                "published" to true,
                "waypoints" to trek.waypoints.take(60).map { it.toMap() },
                "loreDrops" to trek.loreDrops.take(20).map { it.toMap() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(trekId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishJourneyLog(
        authorName: String,
        trek: FictionalTrek,
        distanceKm: Double,
        elevationGainM: Int,
        loreDropsUnlocked: Int,
        note: String,
        moodTag: String,
        isPublished: Boolean = true
    ): Result<String> {
        var path = "journey_logs"
        return try {
            val uid = requireUserId()
            val trimmedNote = note.trim()
            require(trimmedNote.isNotEmpty() && trimmedNote.length <= 600) {
                "Journey note must be between 1 and 600 characters."
            }
            val logId = sanitizeId("log_${uid.take(8)}_${System.currentTimeMillis()}")
            val docRef = db.collection("journey_logs").document(logId)
            path = docRef.path

            val safeDist = distanceKm.coerceIn(0.0, 5000.0)
            val safeElev = elevationGainM.coerceIn(0, 50000).toLong()
            val safeLore = loreDropsUnlocked.coerceIn(0, 50).toLong()

            val payload = mapOf(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "Trailblazer" }.take(50),
                "trekId" to sanitizeId(trek.id),
                "trekTitle" to trek.title.trim().ifBlank { "Unnamed Trek" }.take(80),
                "universe" to trek.universe.trim().ifBlank { "Custom Realm" }.take(50),
                "characterName" to trek.characterName.trim().ifBlank { "Wanderer" }.take(50),
                "distanceKm" to safeDist,
                "elevationGainM" to safeElev,
                "loreDropsUnlocked" to safeLore,
                "note" to trimmedNote,
                "moodTag" to moodTag.trim().ifBlank { "Inspired" }.take(30),
                "published" to isPublished,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(logId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityTrek(trekId: String): Result<Unit> {
        val safeId = sanitizeId(trekId)
        val path = "community_treks/$safeId"
        return try {
            requireUserId()
            db.collection("community_treks").document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
