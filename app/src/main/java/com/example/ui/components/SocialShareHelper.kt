package com.example.ui.components

import android.content.Context
import android.content.Intent
import com.example.data.model.FictionalTrek
import com.example.data.model.JourneyLogDoc
import com.example.data.model.LoreDrop

object SocialShareHelper {
    fun shareTrekProgress(
        context: Context,
        trek: FictionalTrek,
        distanceKm: Double,
        loreCount: Int
    ) {
        val text = buildString {
            appendLine("Expedition Dispatch from Fiction Hike")
            appendLine("Walking ${trek.characterName}'s path: ${trek.title} (${trek.universe})")
            appendLine("Progress: ${"%.2f".format(distanceKm)} km hiked | $loreCount/${trek.loreDrops.size} Lore Drops discovered")
            append("#FictionHike #${trek.universe.replace(" ", "")}")
        }
        launchShareChooser(context, text, "Share Trek Progress")
    }

    fun shareLoreDrop(
        context: Context,
        trek: FictionalTrek,
        loreDrop: LoreDrop
    ) {
        val text = buildString {
            appendLine("Lore Drop Discovered on Fiction Hike!")
            appendLine("${loreDrop.title} (${trek.universe} - ${trek.characterName}'s Route)")
            if (loreDrop.quote.isNotBlank()) {
                appendLine("\"${loreDrop.quote}\"")
            }
            appendLine(loreDrop.content)
            append("#FictionHike #LoreDrop")
        }
        launchShareChooser(context, text, "Share Lore Discovery")
    }

    fun shareJourneyLog(
        context: Context,
        log: JourneyLogDoc
    ) {
        val text = buildString {
            appendLine("${log.authorName} logged a Fiction Hike expedition!")
            appendLine("${log.trekTitle} as ${log.characterName} (${log.universe})")
            appendLine("${"%.2f".format(log.distanceKm)} km | +${log.elevationGainM}m | ${log.loreDropsUnlocked} Lore Drops")
            appendLine("\"${log.note}\"")
            append("#FictionHike #TrailLog")
        }
        launchShareChooser(context, text, "Share Journey Log")
    }

    private fun launchShareChooser(context: Context, body: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
