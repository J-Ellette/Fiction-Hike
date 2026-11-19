package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CuratedTreksCatalog
import com.example.data.model.FictionalTrek
import com.example.data.model.GeoProjection
import com.example.data.model.LoreDrop
import com.example.data.model.TrekDifficulty
import com.example.data.model.Waypoint
import com.example.viewmodel.FictionHikeViewModel
import java.util.UUID
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrekCreatorScreen(
    viewModel: FictionHikeViewModel,
    onNavigateBack: () -> Unit,
    onTrekPublished: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val drawnPoints = remember { mutableStateListOf<Offset>() }
    val customLoreDrops = remember { mutableStateListOf<LoreDrop>() }

    var freehandDrawMode by remember { mutableStateOf(false) }
    var trekTitle by remember { mutableStateOf("") }
    var characterName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val presetUniverses = remember { CuratedTreksCatalog.allUniverses + "Create New Universe" }
    var selectedUniverseChoice by remember { mutableStateOf(presetUniverses.first()) }
    var customUniverseText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(TrekDifficulty.MODERATE) }
    var selectedAccentHex by remember { mutableStateOf("#E8B84B") }

    var showLoreDialog by remember { mutableStateOf(false) }
    var loreWaypointIndex by remember { mutableIntStateOf(0) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val accentSwatches = listOf("#E8B84B", "#3DD598", "#F48C42", "#EF6461", "#7CC6FE", "#C792EA")

    val computedWaypoints = remember(drawnPoints.size, drawnPoints.toList()) {
        if (drawnPoints.isEmpty()) {
            emptyList()
        } else {
            val first = drawnPoints.first()
            drawnPoints.mapIndexed { idx, pt ->
                val dx = ((pt.x - first.x) / 300.0).coerceIn(-2.0, 2.0)
                val dy = (-(pt.y - first.y) / 300.0).coerceIn(-2.0, 2.0)
                val elev = 140.0 + 180.0 * sin(dx * 4.0 + idx * 0.5) + 95.0 * cos(dy * 3.5)
                Waypoint(
                    dx = dx,
                    dy = dy,
                    elevM = elev.coerceAtLeast(30.0),
                    label = when (idx) {
                        0 -> "Trailhead"
                        drawnPoints.lastIndex -> "Summit End"
                        else -> "Waypoint ${idx + 1}"
                    }
                )
            }
        }
    }

    val estimatedKm = remember(computedWaypoints) {
        (GeoProjection.normalizedPathLength(computedWaypoints) * 6.5).coerceIn(0.5, 50.0)
    }
    val elevationGain = remember(computedWaypoints) {
        GeoProjection.calculateElevationGainMeters(computedWaypoints)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Custom Trek") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_studio),
                        contentDescription = "Cartographer drafting table",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.25f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "CARTOGRAPHER STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE8B84B)
                        )
                        Text(
                            text = "Plot a Fictional Route & Lore Drops",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Interactive Map Drawing Canvas
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Draw Route on Map (${drawnPoints.size} pts)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !freehandDrawMode,
                                    onClick = { freehandDrawMode = false },
                                    label = { Text("Tap") },
                                    leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                                FilterChip(
                                    selected = freehandDrawMode,
                                    onClick = { freehandDrawMode = true },
                                    label = { Text("Draw") },
                                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }

                        val routeAccent = remember(selectedAccentHex) {
                            try {
                                Color(android.graphics.Color.parseColor(selectedAccentHex))
                            } catch (_: Exception) {
                                Color(0xFFE8B84B)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF101C17))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .testTag("creator_draw_canvas")
                                .pointerInput(freehandDrawMode) {
                                    if (!freehandDrawMode) {
                                        detectTapGestures { pos ->
                                            if (drawnPoints.size < 45) {
                                                drawnPoints.add(pos)
                                            }
                                        }
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { pos ->
                                                if (drawnPoints.size < 45) drawnPoints.add(pos)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val p = change.position
                                                val last = drawnPoints.lastOrNull()
                                                if ((last == null || hypot(p.x - last.x, p.y - last.y) > 28f) && drawnPoints.size < 45) {
                                                    drawnPoints.add(p)
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridColor = Color(0x283DD598)
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                                    x += 48f
                                }
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                                    y += 48f
                                }

                                if (drawnPoints.size >= 2) {
                                    val path = Path().apply {
                                        drawnPoints.forEachIndexed { idx, pt ->
                                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = routeAccent.copy(alpha = 0.35f),
                                        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    drawPath(
                                        path = path,
                                        color = routeAccent,
                                        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }

                                drawnPoints.forEachIndexed { idx, pt ->
                                    val hasLore = customLoreDrops.any { it.waypointIndex == idx }
                                    drawCircle(
                                        color = if (hasLore) Color(0xFF3DD598) else routeAccent,
                                        radius = if (hasLore) 11f else 7f,
                                        center = pt
                                    )
                                    drawCircle(color = Color(0xFF0C1411), radius = 3.5f, center = pt)
                                }
                            }

                            if (drawnPoints.isEmpty()) {
                                Text(
                                    text = if (freehandDrawMode) "Drag finger here to draw your trail" else "Tap points here to plot route waypoints",
                                    color = Color(0xFFA7B8AF),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Est. ${"%.1f".format(estimatedKm)} km • +${elevationGain}m gain",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (drawnPoints.isNotEmpty()) {
                                            val removedIdx = drawnPoints.lastIndex
                                            drawnPoints.removeAt(removedIdx)
                                            customLoreDrops.removeAll { it.waypointIndex >= drawnPoints.size }
                                        }
                                    },
                                    enabled = drawnPoints.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Undo")
                                }
                                OutlinedButton(
                                    onClick = {
                                        drawnPoints.clear()
                                        customLoreDrops.clear()
                                    },
                                    enabled = drawnPoints.isNotEmpty()
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }

            // Trek Details Form
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = trekTitle,
                        onValueChange = { if (it.length <= 80) trekTitle = it },
                        label = { Text("Trek Title *") },
                        placeholder = { Text("e.g., The Whispering Fen Crossing") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_title_input")
                    )

                    OutlinedTextField(
                        value = characterName,
                        onValueChange = { if (it.length <= 50) characterName = it },
                        label = { Text("Character Name *") },
                        placeholder = { Text("e.g., Samwise Gamgee, Arya Stark, Original Ranger") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_character_input")
                    )

                    Text(
                        text = "Fictional Universe",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetUniverses) { u ->
                            FilterChip(
                                selected = selectedUniverseChoice == u,
                                onClick = { selectedUniverseChoice = u },
                                label = { Text(u) }
                            )
                        }
                    }

                    if (selectedUniverseChoice == "Create New Universe") {
                        OutlinedTextField(
                            value = customUniverseText,
                            onValueChange = { if (it.length <= 50) customUniverseText = it },
                            label = { Text("New Universe Name *") },
                            placeholder = { Text("e.g., Earthsea, Stormlight Archive, Original World") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("creator_new_universe_input")
                        )
                    }

                    Text("Difficulty & Trail Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TrekDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = selectedDifficulty == diff,
                                onClick = { selectedDifficulty = diff },
                                label = { Text(diff.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        accentSwatches.forEach { hex ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (selectedAccentHex == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAccentHex = hex }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        label = { Text("Route Overview & Lore Setting") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("creator_description_input")
                    )
                }
            }

            // Custom Lore Drops Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Custom Lore Drops (${customLoreDrops.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    loreWaypointIndex = (drawnPoints.lastIndex).coerceAtLeast(0)
                                    showLoreDialog = true
                                },
                                enabled = drawnPoints.size >= 2 && customLoreDrops.size < 20,
                                modifier = Modifier.testTag("add_custom_lore_drop_button")
                            ) {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Lore Drop")
                            }
                        }

                        if (drawnPoints.size < 2) {
                            Text(
                                text = "Plot at least 2 route points on the canvas above to attach Lore Drops.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (customLoreDrops.isEmpty()) {
                            Text(
                                text = "No Lore Drops added yet. Tap 'Add Lore Drop' to place discoverable trivia or quotes along your trail.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            customLoreDrops.forEachIndexed { idx, lore ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${lore.title} (${lore.category}) • Waypoint ${lore.waypointIndex + 1}",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = lore.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { customLoreDrops.removeAt(idx) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Lore Drop")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (validationError != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val finalUniverse = if (selectedUniverseChoice == "Create New Universe") {
                            customUniverseText.trim()
                        } else {
                            selectedUniverseChoice
                        }
                        when {
                            drawnPoints.size < 2 -> validationError = "Please plot at least 2 waypoints on the map."
                            trekTitle.trim().length < 2 -> validationError = "Enter a trek title (at least 2 characters)."
                            characterName.trim().length < 2 -> validationError = "Enter a character name (at least 2 characters)."
                            finalUniverse.length < 2 -> validationError = "Choose or enter a fictional universe name."
                            else -> {
                                validationError = null
                                val newTrek = FictionalTrek(
                                    id = "draft_${UUID.randomUUID()}",
                                    title = trekTitle.trim(),
                                    universe = finalUniverse,
                                    characterName = characterName.trim(),
                                    characterRole = "Community Quest",
                                    description = description.trim().ifBlank { "Custom route through $finalUniverse." },
                                    difficulty = selectedDifficulty,
                                    estimatedKm = estimatedKm,
                                    elevationGainM = elevationGain,
                                    accentColorHex = selectedAccentHex,
                                    waypoints = computedWaypoints,
                                    loreDrops = customLoreDrops.toList(),
                                    isCustom = true
                                )
                                viewModel.publishCustomTrek(newTrek) {
                                    onTrekPublished()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("publish_custom_trek_button")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publish Trek to Community", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLoreDialog) {
        AddCustomLoreDropDialog(
            maxWaypointIndex = (drawnPoints.lastIndex).coerceAtLeast(0),
            initialWaypointIndex = loreWaypointIndex,
            onDismiss = { showLoreDialog = false },
            onAdd = { drop ->
                customLoreDrops.add(drop)
                showLoreDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomLoreDropDialog(
    maxWaypointIndex: Int,
    initialWaypointIndex: Int,
    onDismiss: () -> Unit,
    onAdd: (LoreDrop) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Trivia") }
    var quote by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedWp by remember { mutableIntStateOf(initialWaypointIndex.coerceIn(0, maxWaypointIndex)) }
    val categories = listOf("Trivia", "Quote", "Behind the Scenes", "Lore")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Lore Drop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Lore Point Title *") },
                    singleLine = true,
                    modifier = Modifier.testTag("custom_lore_title_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Text("Attach to Waypoint #${selectedWp + 1} (of ${maxWaypointIndex + 1})", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items((0..maxWaypointIndex).toList()) { idx ->
                        AssistChip(
                            onClick = { selectedWp = idx },
                            label = { Text("WP ${idx + 1}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = quote,
                    onValueChange = { if (it.length <= 160) quote = it },
                    label = { Text("Thematic Quote (optional)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 350) content = it },
                    label = { Text("Trivia / Behind-the-Scenes Lore *") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.testTag("custom_lore_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeTitle = title.trim().ifBlank { "Trail Lore #${selectedWp + 1}" }
                    val safeContent = content.trim().ifBlank { "Custom expedition note at waypoint ${selectedWp + 1}." }
                    onAdd(
                        LoreDrop(
                            id = "custom_lore_${System.currentTimeMillis()}_${selectedWp}",
                            title = safeTitle,
                            category = category,
                            quote = quote.trim(),
                            content = safeContent,
                            waypointIndex = selectedWp
                        )
                    )
                },
                modifier = Modifier.testTag("confirm_add_lore_button")
            ) {
                Text("Attach Point")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
