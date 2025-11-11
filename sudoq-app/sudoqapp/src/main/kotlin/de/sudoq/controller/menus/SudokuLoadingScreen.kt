/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.sudoq.R
import de.sudoq.model.game.GameData
import de.sudoq.persistence.game.GameRepo
import java.io.File
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuLoadingScreen(
    games: List<GameData>,
    gameRepo: GameRepo,
    onBackPressed: () -> Unit,
    onGameClick: (Int) -> Unit,
    onDeleteGame: (Int) -> Unit,
    onDeleteFinished: () -> Unit,
    onDeleteAll: () -> Unit,
    onExportGameAsText: ((Int) -> Unit)? = null,
    onExportGameAsFile: ((Int) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<GameData?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sf_sudokuloading_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (games.isNotEmpty()) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sudokuloading_delete_finished)) },
                                onClick = {
                                    onDeleteFinished()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sudokuloading_delete_all)) },
                                onClick = {
                                    onDeleteAll()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved games",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games) { game ->
                    GameCard(
                        game = game,
                        gameRepo = gameRepo,
                        onClick = { onGameClick(game.id) },
                        onLongClick = { showDeleteDialog = game },
                        onExportAsText = onExportGameAsText?.let { { it(game.id) } },
                        onExportAsFile = onExportGameAsFile?.let { { it(game.id) } }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { game ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            onGameClick(game.id)
                            showDeleteDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.sudokuloading_dialog_play),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            onDeleteGame(game.id)
                            showDeleteDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.sudokuloading_dialog_delete),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (onExportGameAsText != null && onExportGameAsFile != null) {
                        TextButton(
                            onClick = {
                                onExportGameAsText(game.id)
                                showDeleteDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export as text", modifier = Modifier.fillMaxWidth())
                        }
                        
                        TextButton(
                            onClick = {
                                onExportGameAsFile(game.id)
                                showDeleteDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export as file", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun GameCard(
    game: GameData,
    gameRepo: GameRepo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onExportAsText: (() -> Unit)?,
    onExportAsFile: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            val thumbnailFile = gameRepo.getGameThumbnailFile(game.id)
            if (thumbnailFile.exists()) {
                val bitmap = remember(game.id) {
                    try {
                        BitmapFactory.decodeFile(thumbnailFile.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Game thumbnail",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                } ?: Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.type.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Complexity: ${game.complexity}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = formatDate(game.playedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (game.isFinished) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Finished",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play"
                )
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return dateFormat.format(date)
}
