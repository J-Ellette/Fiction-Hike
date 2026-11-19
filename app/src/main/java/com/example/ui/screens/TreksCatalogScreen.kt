package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ActiveTrekProgress
import com.example.data.model.FictionalTrek
import com.example.viewmodel.FictionHikeViewModel
import com.example.viewmodel.UiState

@Composable
fun TreksCatalogScreen(
    viewModel: FictionHikeViewModel,
    onSelectTrekToMap: () -> Unit,
    onOpenCreateTrek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTreksState by viewModel.allTreksState.collectAsStateWithLifecycle()
    val activeProgressList by viewModel.activeProgressList.collectAsStateWithLifecycle()
    val offlineRegions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val hikeState by viewModel.hikeState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedUniverse by remember { mutableStateOf("All") }
    var selectedCharacter by remember { mutableStateOf("All Characters") }
    var showCommunityOnly by remember { mutableStateOf(false) }

    val treks = (allTreksState as? UiState.Success)?.data.orEmpty()
    val universes = remember(treks) { listOf("All") + treks.map { it.universe }.distinct() }
    val characters = remember(treks, selectedUniverse) {
        val scoped = if (selectedUniverse == "All") treks else treks.filter { it.universe == selectedUniverse }
        listOf("All Characters") + scoped.map { it.characterName }.distinct()
    }

    val filteredTreks = remember(treks, searchQuery, selectedUniverse, selectedCharacter, showCommunityOnly) {
        treks.filter { trek ->
            val matchesUniverse = selectedUniverse == "All" || trek.universe == selectedUniverse
            val matchesChar = selectedCharacter == "All Characters" || trek.characterName == selectedCharacter
            val matchesCommunity = !showCommunityOnly || trek.isCustom
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                trek.title.contains(q, ignoreCase = true) ||
                trek.characterName.contains(q, ignoreCase = true) ||
                trek.universe.contains(q, ignoreCase = true)
            matchesUniverse && matchesChar && matchesCommunity && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateTrek,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Trek") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_custom_trek_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_expedition),
                        contentDescription = "Fictional expeditions header",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "LEGENDARY ROUTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Choose a Character & Realm",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Frodo, Arya Stark, The Road, Boulder...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("treks_search_input")
                    )

                    // Universe Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = showCommunityOnly,
                                onClick = { showCommunityOnly = !showCommunityOnly },
                                label = { Text("Community Treks") },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                        items(universes) { universe ->
                            FilterChip(
                                selected = selectedUniverse == universe,
                                onClick = {
                                    selectedUniverse = universe
                                    selectedCharacter = "All Characters"
                                },
                                label = { Text(universe) }
                            )
                        }
                    }

                    // Character Filter Row
                    Text(
                        text = "Filter by Character Route",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(characters) { character ->
                            FilterChip(
                                selected = selectedCharacter == character,
                                onClick = { selectedCharacter = character },
                                label = { Text(character) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                                },
                                modifier = Modifier.testTag("character_filter_$character")
                            )
                        }
                    }
                }
            }

            if (filteredTreks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No fictional treks match your filter.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'Create Trek' below to plot a custom quest for the community.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTreks, key = { it.id }) { trek ->
                    val progress = activeProgressList.firstOrNull { it.trekId == trek.id }
                    val isOffline = offlineRegions.any { it.trekId == trek.id }
                    val isSelected = hikeState.selectedTrek.id == trek.id

                    TrekCatalogCard(
                        trek = trek,
                        progress = progress,
                        isOfflineCached = isOffline,
                        isCurrentlySelected = isSelected,
                        onSelectHike = {
                            viewModel.selectTrek(trek, progress)
                            onSelectTrekToMap()
                        },
                        onDownloadOffline = {
                            viewModel.downloadOfflineRegion(trek)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrekCatalogCard(
    trek: FictionalTrek,
    progress: ActiveTrekProgress?,
    isOfflineCached: Boolean,
    isCurrentlySelected: Boolean,
    onSelectHike: () -> Unit,
    onDownloadOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = remember(trek.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(trek.accentColorHex))
        } catch (_: Exception) {
            Color(0xFFE8B84B)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlySelected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent)
                    )
                    Text(
                        text = trek.universe.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    if (trek.isCustom) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "COMMUNITY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                AssistChip(
                    onClick = {},
                    label = { Text(trek.difficulty.label, style = MaterialTheme.typography.labelSmall) }
                )
            }

            Text(
                text = trek.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${trek.characterName} • ${trek.characterRole}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (trek.description.isNotBlank()) {
                Text(
                    text = trek.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${"%.1f".format(trek.estimatedKm)} km",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+${trek.elevationGainM} m elev",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${trek.loreDrops.size} Lore Drops",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (progress != null) {
                val pct = (progress.distanceCoveredKm / progress.targetDistanceKm.coerceAtLeast(0.1))
                    .toFloat()
                    .coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Saved Progress: ${"%.2f".format(progress.distanceCoveredKm)} km (${progress.unlockedLoreDropIds.size} Lore Drops)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSelectHike,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_trek_${trek.id}")
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isCurrentlySelected) "Open Overlay" else "Overlay on GPS")
                }

                IconButton(
                    onClick = onDownloadOffline,
                    modifier = Modifier.testTag("offline_download_${trek.id}")
                ) {
                    Icon(
                        imageVector = if (isOfflineCached) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                        contentDescription = "Download offline map",
                        tint = if (isOfflineCached) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
