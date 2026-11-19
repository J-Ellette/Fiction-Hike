package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TrekDifficulty(val label: String) {
    EASY("Easy"),
    MODERATE("Moderate"),
    EPIC("Epic"),
    LEGENDARY("Legendary");

    companion object {
        fun fromRaw(raw: String?): TrekDifficulty =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MODERATE
    }
}

data class Waypoint(
    val dx: Double = 0.0,
    val dy: Double = 0.0,
    val elevM: Double = 100.0,
    val label: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "dx" to dx,
        "dy" to dy,
        "elevM" to elevM,
        "label" to label
    )

    companion object {
        fun fromMap(map: Map<*, *>): Waypoint = Waypoint(
            dx = (map["dx"] as? Number)?.toDouble() ?: 0.0,
            dy = (map["dy"] as? Number)?.toDouble() ?: 0.0,
            elevM = (map["elevM"] as? Number)?.toDouble() ?: 100.0,
            label = (map["label"] as? String).orEmpty()
        )
    }
}

data class LoreDrop(
    val id: String = "",
    val title: String = "",
    val category: String = "Trivia",
    val content: String = "",
    val quote: String = "",
    val waypointIndex: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "category" to category,
        "content" to content,
        "quote" to quote,
        "waypointIndex" to waypointIndex
    )

    companion object {
        fun fromMap(map: Map<*, *>, fallbackIndex: Int = 0): LoreDrop = LoreDrop(
            id = (map["id"] as? String).orEmpty().ifBlank { "lore_$fallbackIndex" },
            title = (map["title"] as? String).orEmpty(),
            category = (map["category"] as? String).orEmpty().ifBlank { "Trivia" },
            content = (map["content"] as? String).orEmpty(),
            quote = (map["quote"] as? String).orEmpty(),
            waypointIndex = (map["waypointIndex"] as? Number)?.toInt() ?: fallbackIndex
        )
    }
}

data class FictionalTrek(
    val id: String,
    val title: String,
    val universe: String,
    val characterName: String,
    val characterRole: String,
    val description: String,
    val difficulty: TrekDifficulty,
    val estimatedKm: Double,
    val elevationGainM: Int,
    val accentColorHex: String,
    val waypoints: List<Waypoint>,
    val loreDrops: List<LoreDrop>,
    val isCustom: Boolean = false,
    val creatorName: String = "Lore Cartographers",
    val creatorId: String = ""
)

@Keep
@IgnoreExtraProperties
data class TrekkerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val favoriteUniverse: String = "Lord of the Rings",
    val totalDistanceKm: Double = 0.0,
    val treksCompleted: Long = 0L,
    val loreDropsFound: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActiveTrekProgress(
    @DocumentId val id: String = "",
    val trekId: String = "",
    val trekTitle: String = "",
    val universe: String = "",
    val characterName: String = "",
    val status: String = "active",
    val distanceCoveredKm: Double = 0.0,
    val targetDistanceKm: Double = 1.0,
    val currentWaypointIndex: Long = 0L,
    val unlockedLoreDropIds: List<String> = emptyList(),
    val anchorLat: Double = 37.7749,
    val anchorLon: Double = -122.4194,
    val scaleKm: Double = 2.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityTrekDoc(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val universe: String = "",
    val characterName: String = "",
    val description: String = "",
    val difficulty: String = "MODERATE",
    val estimatedKm: Double = 1.0,
    val elevationGainM: Long = 0L,
    val accentColorHex: String = "#E8B84B",
    val published: Boolean = true,
    val waypoints: List<Map<String, Any>> = emptyList(),
    val loreDrops: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFictionalTrek(): FictionalTrek {
        val parsedWaypoints = waypoints.map { Waypoint.fromMap(it) }
        val parsedLore = loreDrops.mapIndexed { idx, m -> LoreDrop.fromMap(m, idx) }
        return FictionalTrek(
            id = id,
            title = title,
            universe = universe,
            characterName = characterName,
            characterRole = "Community Trailblazer",
            description = description,
            difficulty = TrekDifficulty.fromRaw(difficulty),
            estimatedKm = estimatedKm,
            elevationGainM = elevationGainM.toInt(),
            accentColorHex = accentColorHex,
            waypoints = parsedWaypoints,
            loreDrops = parsedLore,
            isCustom = true,
            creatorName = creatorName,
            creatorId = creatorId
        )
    }
}

@Keep
@IgnoreExtraProperties
data class JourneyLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val trekId: String = "",
    val trekTitle: String = "",
    val universe: String = "",
    val characterName: String = "",
    val distanceKm: Double = 0.0,
    val elevationGainM: Long = 0L,
    val loreDropsUnlocked: Long = 0L,
    val note: String = "",
    val moodTag: String = "Inspired",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val rankTitle: String = "Pathfinder",
    val activeTrekTitle: String = "",
    val activeCharacter: String = "",
    val totalDistanceKm: Double = 0.0,
    val treksCompleted: Long = 0L,
    val loreDropsFound: Long = 0L,
    val activeOnBoard: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
