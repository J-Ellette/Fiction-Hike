package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FictionalTrek
import com.example.data.model.ProjectedLoreDrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoreDropDiscoverySheet(
    trek: FictionalTrek,
    projectedDrop: ProjectedLoreDrop,
    onDismiss: () -> Unit,
    onLogDiscovery: (String) -> Unit
) {
    val context = LocalContext.current
    val drop = projectedDrop.loreDrop

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = "Lore Drop unlocked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LORE DROP UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = drop.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(drop.category) }
                )
            }

            Text(
                text = "${trek.universe} • ${trek.characterName}'s Route",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "GPS: ${"%.5f".format(projectedDrop.coordinate.latitude)}, ${"%.5f".format(projectedDrop.coordinate.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (drop.quote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${drop.quote}\"",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Text(
                text = drop.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { SocialShareHelper.shareLoreDrop(context, trek, drop) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_lore_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        onLogDiscovery("Discovered '${drop.title}' on ${trek.characterName}'s ${trek.title} trek!")
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_lore_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log Milestone")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogJourneyMilestoneSheet(
    trek: FictionalTrek,
    currentDistanceKm: Double,
    unlockedLoreCount: Int,
    onDismiss: () -> Unit,
    onSubmitLog: (addedKm: Double, note: String, moodTag: String, publishToCommunity: Boolean) -> Unit
) {
    var addedKmText by remember { mutableStateOf("1.5") }
    var noteText by remember { mutableStateOf("Walking ${trek.characterName}'s trail along ${trek.title}.") }
    var selectedMood by remember { mutableStateOf("Inspired") }
    var publishToCommunity by remember { mutableStateOf(true) }
    val moods = listOf("Inspired", "Vigilant", "Relentless", "Serene", "Epic")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log Expedition Milestone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${trek.characterName} • ${trek.title} ($unlockedLoreCount Lore Drops)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = addedKmText,
                onValueChange = { addedKmText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Distance to add (km) • Current: ${"%.2f".format(currentDistanceKm)} km") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_distance_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                moods.take(4).forEach { mood ->
                    AssistChip(
                        onClick = { selectedMood = mood },
                        label = { Text(mood) },
                        leadingIcon = if (selectedMood == mood) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 500) noteText = it },
                label = { Text("Trail Journal Reflection") },
                minLines = 3,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_note_input")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = publishToCommunity,
                    onCheckedChange = { publishToCommunity = it }
                )
                Text(
                    text = "Share journey log with online community",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val added = addedKmText.toDoubleOrNull()?.coerceIn(0.0, 200.0) ?: 0.0
                        val finalNote = noteText.trim().ifBlank { "Completed a leg on ${trek.title}." }
                        onSubmitLog(added, finalNote, selectedMood, publishToCommunity)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("submit_journey_log_button")
                ) {
                    Text("Save & Sync")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
