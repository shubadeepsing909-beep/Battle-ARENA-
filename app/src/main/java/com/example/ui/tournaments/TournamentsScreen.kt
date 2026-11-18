package com.example.ui.tournaments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.GameMode
import com.example.data.model.Registration
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.data.repository.TournamentFormInput
import com.example.ui.components.ArenaCard
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StatusTone
import com.example.ui.components.formatInr
import com.example.ui.components.statusToneFor
import com.example.ui.theme.ArenaBluePrimary
import com.example.ui.theme.ArenaCanvas
import com.example.ui.theme.ArenaCrimson
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaInkPrimary
import com.example.ui.theme.ArenaInkSecondary
import com.example.ui.theme.ArenaSurfaceBlueTint
import com.example.ui.theme.ArenaWhite
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun TournamentsScreen(
    tournamentsState: UiState<List<Tournament>>,
    registrationsState: UiState<List<Registration>>,
    actionBusy: Boolean,
    openCreateInitially: Boolean = false,
    onCreateConsumed: () -> Unit = {},
    onSelectTournament: (String?) -> Unit,
    onCreateTournament: (TournamentFormInput) -> Unit,
    onUpdateTournament: (String, TournamentFormInput) -> Unit,
    onTogglePublished: (Tournament) -> Unit,
    onUpdateRoomCredentials: (String, String, String, String) -> Unit,
    onUpdateStatus: (String, String, TournamentStatus) -> Unit,
    onDeleteTournament: (Tournament) -> Unit,
    onRemoveParticipant: (Registration, String) -> Unit,
    onAllocateWinner: (Tournament, Registration, Long, Long, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var activeTournamentId by remember { mutableStateOf<String?>(null) }
    var showFormDialog by remember(openCreateInitially) { mutableStateOf(openCreateInitially) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }

    val activeTournament = remember(tournamentsState, activeTournamentId) {
        (tournamentsState as? UiState.Success)?.data?.firstOrNull { it.id == activeTournamentId }
    }

    if (activeTournament != null) {
        BackHandler {
            activeTournamentId = null
            onSelectTournament(null)
        }
        TournamentDetailView(
            tournament = activeTournament,
            registrationsState = registrationsState,
            actionBusy = actionBusy,
            onBack = {
                activeTournamentId = null
                onSelectTournament(null)
            },
            onEdit = {
                editingTournament = activeTournament
                showFormDialog = true
            },
            onTogglePublished = { onTogglePublished(activeTournament) },
            onSaveRoomCredentials = { roomId, roomPass ->
                onUpdateRoomCredentials(activeTournament.id, activeTournament.title, roomId, roomPass)
            },
            onUpdateStatus = { newStatus ->
                onUpdateStatus(activeTournament.id, activeTournament.title, newStatus)
            },
            onDelete = {
                onDeleteTournament(activeTournament)
                activeTournamentId = null
                onSelectTournament(null)
            },
            onRemoveParticipant = { reg ->
                onRemoveParticipant(reg, activeTournament.title)
            },
            onAllocateWinner = { reg, rank, kills, prize ->
                onAllocateWinner(activeTournament, reg, rank, kills, prize)
            }
        )
    } else {
        Scaffold(
            containerColor = ArenaCanvas,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingTournament = null
                        showFormDialog = true
                    },
                    containerColor = ArenaBluePrimary,
                    contentColor = ArenaWhite,
                    modifier = Modifier.testTag("create_tournament_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Tournament")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_tournaments_input"),
                    placeholder = { Text("Search tournaments by title or map...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                val filters = listOf("ALL", "UPCOMING", "LIVE", "COMPLETED", "CANCELLED", "DRAFT")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArenaBluePrimary,
                                selectedLabelColor = ArenaWhite
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                when (tournamentsState) {
                    UiState.Loading -> LoadingStateView("Loading tournaments...")
                    is UiState.Error -> ErrorBanner(tournamentsState.message)
                    is UiState.Success -> {
                        val filtered = tournamentsState.data.filter { t ->
                            val matchesSearch = searchQuery.isBlank() ||
                                t.title.contains(searchQuery, ignoreCase = true) ||
                                t.mapName.contains(searchQuery, ignoreCase = true)
                            val matchesFilter = when (selectedFilter) {
                                "ALL" -> true
                                "DRAFT" -> !t.published
                                else -> t.status == selectedFilter
                            }
                            matchesSearch && matchesFilter
                        }
                        if (filtered.isEmpty()) {
                            EmptyStateCard(
                                title = "No Tournaments Found",
                                message = "Tap the + button below to create your first Free Fire Solo, Duo, or Squad tournament.",
                                icon = Icons.Filled.SportsEsports
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 88.dp)
                            ) {
                                items(filtered, key = { it.id }) { tournament ->
                                    TournamentAdminCard(
                                        tournament = tournament,
                                        onClick = {
                                            activeTournamentId = tournament.id
                                            onSelectTournament(tournament.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        TournamentFormDialog(
            existing = editingTournament,
            actionBusy = actionBusy,
            onDismiss = {
                showFormDialog = false
                editingTournament = null
                onCreateConsumed()
            },
            onSubmit = { input ->
                val target = editingTournament
                if (target == null) onCreateTournament(input)
                else onUpdateTournament(target.id, input)
                showFormDialog = false
                editingTournament = null
                onCreateConsumed()
            }
        )
    }
}

@Composable
private fun TournamentAdminCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    ArenaCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(text = tournament.status, tone = statusToneFor(tournament.status))
                StatusPill(text = tournament.gameMode, tone = StatusTone.BLUE)
                if (!tournament.published) {
                    StatusPill(text = "UNPUBLISHED", tone = StatusTone.NEUTRAL)
                }
            }
            Text(
                text = "${tournament.registeredCount}/${tournament.maxPlayers} Slots",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = ArenaBluePrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tournament.title,
            style = MaterialTheme.typography.titleMedium,
            color = ArenaInkPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Map: ${tournament.mapName} • ${tournament.scheduledDate} at ${tournament.startTime}",
            style = MaterialTheme.typography.bodySmall,
            color = ArenaInkSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (tournament.entryFee <= 0.0) "Entry: FREE" else "Entry: ${formatInr(tournament.entryFee)}",
                style = MaterialTheme.typography.labelMedium,
                color = ArenaInkPrimary
            )
            Text(
                text = "Prize Pool: ${formatInr(tournament.prizePool)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = ArenaBluePrimary
            )
        }
    }
}

@Composable
private fun TournamentDetailView(
    tournament: Tournament,
    registrationsState: UiState<List<Registration>>,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePublished: () -> Unit,
    onSaveRoomCredentials: (String, String) -> Unit,
    onUpdateStatus: (TournamentStatus) -> Unit,
    onDelete: () -> Unit,
    onRemoveParticipant: (Registration) -> Unit,
    onAllocateWinner: (Registration, Long, Long, Double) -> Unit
) {
    var roomIdInput by remember(tournament.id, tournament.roomId) { mutableStateOf(tournament.roomId) }
    var roomPasswordInput by remember(tournament.id, tournament.roomPassword) { mutableStateOf(tournament.roomPassword) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var winnerTarget by remember { mutableStateOf<Registration?>(null) }
    var kickTarget by remember { mutableStateOf<Registration?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tournament_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Match Operations",
                        style = MaterialTheme.typography.titleLarge,
                        color = ArenaInkPrimary
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_tournament_button")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Tournament", tint = ArenaBluePrimary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.testTag("delete_tournament_button")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Tournament", tint = ArenaCrimson)
                    }
                }
            }
        }

        item {
            ArenaCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(text = tournament.status, tone = statusToneFor(tournament.status))
                    StatusPill(text = tournament.gameMode, tone = StatusTone.BLUE)
                    StatusPill(
                        text = if (tournament.published) "PUBLISHED" else "HIDDEN",
                        tone = if (tournament.published) StatusTone.GREEN else StatusTone.NEUTRAL
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(tournament.title, style = MaterialTheme.typography.headlineSmall, color = ArenaInkPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Map: ${tournament.mapName} • Date: ${tournament.scheduledDate} ${tournament.startTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArenaInkSecondary
                )
                Text(
                    "Entry: ${if (tournament.entryFee <= 0) "Free" else formatInr(tournament.entryFee)} • Pool: ${formatInr(tournament.prizePool)} • Slots: ${tournament.registeredCount}/${tournament.maxPlayers}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaInkMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTogglePublished,
                        enabled = !actionBusy,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            if (tournament.published) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (tournament.published) "Unpublish" else "Publish")
                    }
                    if (tournament.status == TournamentStatus.UPCOMING.name) {
                        Button(
                            onClick = { onUpdateStatus(TournamentStatus.LIVE) },
                            enabled = !actionBusy,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Go LIVE")
                        }
                    } else if (tournament.status == TournamentStatus.LIVE.name) {
                        Button(
                            onClick = { onUpdateStatus(TournamentStatus.COMPLETED) },
                            enabled = !actionBusy,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Mark Completed")
                        }
                    }
                }
                if (tournament.status == TournamentStatus.UPCOMING.name || tournament.status == TournamentStatus.LIVE.name) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onUpdateStatus(TournamentStatus.CANCELLED) },
                        enabled = !actionBusy
                    ) {
                        Text("Cancel Tournament", color = ArenaCrimson)
                    }
                }
            }
        }

        item {
            ArenaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = ArenaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Room ID & Password Release",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArenaInkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = roomIdInput,
                    onValueChange = { roomIdInput = it },
                    label = { Text("Free Fire Room ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomPasswordInput,
                    onValueChange = { roomPasswordInput = it },
                    label = { Text("Room Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSaveRoomCredentials(roomIdInput, roomPasswordInput) },
                    enabled = !actionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_room_credentials_button")
                ) {
                    Text("Save & Release Room Credentials")
                }
            }
        }

        item {
            SectionHeader(
                title = "Participants & Winner Allocation",
                subtitle = "Assign rank, kills & credit prize directly to wallet"
            )
        }

        when (registrationsState) {
            UiState.Loading -> item { LoadingStateView("Loading participants...") }
            is UiState.Error -> item { ErrorBanner(registrationsState.message) }
            is UiState.Success -> {
                if (registrationsState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No Participants Yet",
                            message = "Registered Free Fire players will appear here with slot numbers and UIDs."
                        )
                    }
                } else {
                    items(registrationsState.data, key = { it.id }) { reg ->
                        ArenaCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Slot #${reg.slotNumber} • ${reg.playerName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ArenaInkPrimary
                                    )
                                    Text(
                                        text = "FF UID: ${reg.freeFireUid}${if (reg.teamName.isNotBlank()) " • Team: ${reg.teamName}" else ""}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = ArenaInkSecondary
                                    )
                                }
                                StatusPill(text = reg.status, tone = statusToneFor(reg.status))
                            }
                            if (reg.status == "CONFIRMED") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { winnerTarget = reg },
                                        enabled = !actionBusy,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Award Prize")
                                    }
                                    OutlinedButton(
                                        onClick = { kickTarget = reg },
                                        enabled = !actionBusy,
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = "Remove Participant", tint = ArenaCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmActionDialog(
            title = "Delete Tournament?",
            message = "This permanently removes '${tournament.title}'. Audit logs remain preserved.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    kickTarget?.let { target ->
        ConfirmActionDialog(
            title = "Remove Participant?",
            message = "Remove ${target.playerName} (FF UID: ${target.freeFireUid}) from this match?",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                onRemoveParticipant(target)
                kickTarget = null
            },
            onDismiss = { kickTarget = null }
        )
    }

    winnerTarget?.let { target ->
        AllocateWinnerDialog(
            registration = target,
            tournament = tournament,
            onDismiss = { winnerTarget = null },
            onConfirm = { rank, kills, prize ->
                onAllocateWinner(target, rank, kills, prize)
                winnerTarget = null
            }
        )
    }
}

@Composable
private fun AllocateWinnerDialog(
    registration: Registration,
    tournament: Tournament,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Double) -> Unit
) {
    var rankStr by remember { mutableStateOf("1") }
    var killsStr by remember { mutableStateOf("0") }
    var prizeStr by remember { mutableStateOf(tournament.firstPrize.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaWhite,
        title = { Text("Allocate Prize • ${registration.playerName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "FF UID: ${registration.freeFireUid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ArenaInkMuted
                )
                OutlinedTextField(
                    value = rankStr,
                    onValueChange = { rankStr = it },
                    label = { Text("Final Rank (1-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = killsStr,
                    onValueChange = { killsStr = it },
                    label = { Text("Verified Kills") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = prizeStr,
                    onValueChange = { prizeStr = it },
                    label = { Text("Prize Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rank = rankStr.toLongOrNull()?.coerceIn(1L, 100L) ?: 1L
                    val kills = killsStr.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    val prize = prizeStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    onConfirm(rank, kills, prize)
                }
            ) {
                Text("Credit Prize")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TournamentFormDialog(
    existing: Tournament?,
    actionBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (TournamentFormInput) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var gameMode by remember { mutableStateOf(existing?.gameMode ?: GameMode.SQUAD.name) }
    var mapName by remember { mutableStateOf(existing?.mapName ?: "Bermuda") }
    var entryFee by remember { mutableStateOf((existing?.entryFee ?: 50.0).toInt().toString()) }
    var prizePool by remember { mutableStateOf((existing?.prizePool ?: 2000.0).toInt().toString()) }
    var firstPrize by remember { mutableStateOf((existing?.firstPrize ?: 1000.0).toInt().toString()) }
    var secondPrize by remember { mutableStateOf((existing?.secondPrize ?: 600.0).toInt().toString()) }
    var thirdPrize by remember { mutableStateOf((existing?.thirdPrize ?: 400.0).toInt().toString()) }
    var perKillPrize by remember { mutableStateOf((existing?.perKillPrize ?: 10.0).toInt().toString()) }
    var maxPlayers by remember { mutableStateOf((existing?.maxPlayers ?: 48L).toString()) }
    var scheduledDate by remember { mutableStateOf(existing?.scheduledDate ?: "2026-11-20") }
    var startTime by remember { mutableStateOf(existing?.startTime ?: "20:00") }
    var regClose by remember { mutableStateOf(existing?.registrationCloseTime ?: "19:45") }
    var rules by remember { mutableStateOf(existing?.rules ?: "No hacks or emulators. Fair play strictly enforced.") }
    var published by remember { mutableStateOf(existing?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaWhite,
        title = {
            Text(
                text = if (existing == null) "Create Tournament" else "Edit Tournament",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Game Mode", style = MaterialTheme.typography.labelMedium, color = ArenaInkSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameMode.entries.forEach { mode ->
                        FilterChip(
                            selected = gameMode == mode.name,
                            onClick = { gameMode = mode.name },
                            label = { Text(mode.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it },
                    label = { Text("Map (Bermuda / Purgatory / Kalahari)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it },
                        label = { Text("Entry ₹ (0=Free)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxPlayers,
                        onValueChange = { maxPlayers = it },
                        label = { Text("Max Slots") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it },
                        label = { Text("Prize Pool ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = perKillPrize,
                        onValueChange = { perKillPrize = it },
                        label = { Text("Per Kill ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstPrize,
                        onValueChange = { firstPrize = it },
                        label = { Text("1st ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secondPrize,
                        onValueChange = { secondPrize = it },
                        label = { Text("2nd ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thirdPrize,
                        onValueChange = { thirdPrize = it },
                        label = { Text("3rd ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = { scheduledDate = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = regClose,
                    onValueChange = { regClose = it },
                    label = { Text("Registration Close Time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Tournament Rules") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish immediately", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        TournamentFormInput(
                            title = title.ifBlank { "Battle Arena Cup" },
                            gameMode = gameMode,
                            mapName = mapName.ifBlank { "Bermuda" },
                            entryFee = entryFee.toDoubleOrNull() ?: 0.0,
                            prizePool = prizePool.toDoubleOrNull() ?: 0.0,
                            firstPrize = firstPrize.toDoubleOrNull() ?: 0.0,
                            secondPrize = secondPrize.toDoubleOrNull() ?: 0.0,
                            thirdPrize = thirdPrize.toDoubleOrNull() ?: 0.0,
                            perKillPrize = perKillPrize.toDoubleOrNull() ?: 0.0,
                            maxPlayers = (maxPlayers.toLongOrNull() ?: 48L).coerceIn(2L, 1000L),
                            scheduledDate = scheduledDate.ifBlank { "2026-11-20" },
                            startTime = startTime.ifBlank { "20:00" },
                            registrationCloseTime = regClose,
                            rules = rules,
                            published = published
                        )
                    )
                },
                enabled = !actionBusy && title.trim().length >= 2
            ) {
                Text(if (existing == null) "Create" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
