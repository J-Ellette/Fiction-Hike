package com.example

import com.example.data.local.CuratedTreksCatalog
import com.example.data.model.GeoProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoProjectionUnitTest {

    @Test
    fun loreDropsAreOnlyTriggerableWhenActivelyTrekking() {
        val frodoTrek = CuratedTreksCatalog.treks.first { it.id == "lotr_frodo_mount_doom" }
        val anchorLat = 37.7749
        val anchorLon = -122.4194
        val waypoints = GeoProjection.projectWaypoints(frodoTrek.waypoints, anchorLat, anchorLon, 3.0)

        // User is standing right on the first Lore Drop (Bag End)
        val previewDrops = GeoProjection.projectLoreDrops(
            trek = frodoTrek,
            projectedWaypoints = waypoints,
            userLat = anchorLat,
            userLon = anchorLon,
            unlockedIds = emptySet(),
            isActivelyTrekking = false
        )
        assertFalse(
            "Lore Drops must NOT trigger when user is only previewing and not actively trekking",
            previewDrops.any { it.isInTriggerRange }
        )

        val activeDrops = GeoProjection.projectLoreDrops(
            trek = frodoTrek,
            projectedWaypoints = waypoints,
            userLat = anchorLat,
            userLon = anchorLon,
            unlockedIds = emptySet(),
            isActivelyTrekking = true
        )
        assertTrue(
            "First Lore Drop should trigger when user is actively on trek within trigger radius",
            activeDrops.first().isInTriggerRange
        )
    }

    @Test
    fun projectedWaypointsScaleMatchesTargetKilometers() {
        val aryaTrek = CuratedTreksCatalog.treks.first { it.id == "got_arya_riverlands" }
        val targetKm = 4.0
        val projected = GeoProjection.projectWaypoints(aryaTrek.waypoints, 40.0, -105.0, targetKm)

        var totalMeters = 0.0
        for (i in 1 until projected.size) {
            totalMeters += GeoProjection.distanceMeters(
                projected[i - 1].latitude,
                projected[i - 1].longitude,
                projected[i].latitude,
                projected[i].longitude
            )
        }
        assertEquals(targetKm * 1000.0, totalMeters, 150.0)
    }

    @Test
    fun terrainGridGeneratesExpectedDimensions() {
        val trek = CuratedTreksCatalog.treks.first()
        val grid = GeoProjection.generateTerrainGrid(trek.waypoints, gridSize = 18)
        assertEquals(18, grid.size)
        assertEquals(18, grid.first().size)
        assertTrue(grid.flatten().all { it >= 10.0 })
    }
}
