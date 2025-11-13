package de.sudoq.controller.sudoku

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.sudoq.R
import de.sudoq.model.game.Game
import de.sudoq.view.SudokuLayout
import android.view.Gravity

/**
 * State for the Sudoku game screen
 */
data class SudokuGameState(
    val game: Game,
    val isActionTreeShown: Boolean = false,
    val isFinished: Boolean = false,
    val elapsedTime: Long = 0,
    val isPaused: Boolean = false,
    val showMenu: Boolean = false,
    val hintText: String? = null,
    val hintHasExecute: Boolean = false,
    val onHintContinue: (() -> Unit)? = null,
    val onHintExecute: (() -> Unit)? = null,
    val keyboardButtons: List<KeyboardButtonState> = emptyList()
)

/**
 * State for a keyboard button
 */
data class KeyboardButtonState(
    val symbol: Int,
    val displayText: String,
    val isEnabled: Boolean = true,
    val showCheckmark: Boolean = false
)

/**
 * Main Sudoku game screen with Material3 design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    state: SudokuGameState,
    sudokuLayout: SudokuLayout,
    onBackClick: () -> Unit,
    onMenuClick: (SudokuMenuItem) -> Unit,
    onActionTreeToggle: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onHintClick: () -> Unit,
    onSolveClick: () -> Unit,
    onNoteToggle: () -> Unit,
    onKeyboardInput: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formatTime(state.elapsedTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    // Action Tree toggle
                    IconButton(onClick = onActionTreeToggle) {
                        Icon(
                            imageVector = if (state.isActionTreeShown) Icons.Default.Close else Icons.Default.List,
                            contentDescription = "Action Tree"
                        )
                    }
                    
                    // More options menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.sf_mainmenu_preferences)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sf_mainmenu_preferences)) },
                            onClick = {
                                showMenu = false
                                onMenuClick(SudokuMenuItem.Settings)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sf_sudoku_new_game)) },
                            onClick = {
                                showMenu = false
                                onMenuClick(SudokuMenuItem.NewGame)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.gesture_builder_title)) },
                            onClick = {
                                showMenu = false
                                onMenuClick(SudokuMenuItem.Gestures)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.TouchApp, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sudoku Board - takes most of the space
            var boardWidth by remember { mutableStateOf(0) }
            var boardHeight by remember { mutableStateOf(0) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        val newWidth = coordinates.size.width
                        val newHeight = coordinates.size.height
                        if (newWidth > 0 && newHeight > 0 && (newWidth != boardWidth || newHeight != boardHeight)) {
                            boardWidth = newWidth
                            boardHeight = newHeight
                            android.util.Log.d("SudokuScreen", "Box sized: width=$boardWidth, height=$boardHeight")
                        }
                    }
            ) {
                if (boardWidth > 0 && boardHeight > 0) {
                    AndroidView(
                        factory = { context ->
                            android.util.Log.d("SudokuScreen", "AndroidView factory called with width=$boardWidth, height=$boardHeight")
                            sudokuLayout.apply {
                                post {
                                    android.util.Log.d("SudokuScreen", "Calling optiZoom with width=$boardWidth, height=$boardHeight")
                                    optiZoom(boardWidth, boardHeight)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Control Panel
            SudokuControlPanel(
                onUndoClick = onUndoClick,
                onRedoClick = onRedoClick,
                onHintClick = onHintClick,
                onSolveClick = onSolveClick,
                onNoteToggle = onNoteToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            // Virtual Keyboard or Hint Panel
            if (state.hintText != null) {
                // Show hint panel in keyboard area
                HintPanel(
                    text = state.hintText,
                    showExecute = state.hintHasExecute,
                    onContinue = state.onHintContinue ?: {},
                    onExecute = state.onHintExecute ?: {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                // Show virtual keyboard (Material Design Compose version)
                ComposeKeyboard(
                    buttons = state.keyboardButtons,
                    onButtonClick = onKeyboardInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Control panel with game action buttons
 */
@Composable
fun SudokuControlPanel(
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onHintClick: () -> Unit,
    onSolveClick: () -> Unit,
    onNoteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            IconButton(
                onClick = onUndoClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = stringResource(R.string.sf_sudoku_button_undo),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Redo button
            IconButton(
                onClick = onRedoClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = stringResource(R.string.sf_sudoku_button_redo),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Note toggle button
            FilledTonalIconButton(
                onClick = onNoteToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.sf_sudoku_button_note),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Hint button
            IconButton(
                onClick = onHintClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = stringResource(R.string.sf_sudoku_button_hint),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Fill candidates button
            IconButton(
                onClick = onSolveClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = stringResource(R.string.sf_sudoku_button_fill_candidates),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Hint panel that appears in the keyboard area
 */
@Composable
fun HintPanel(
    text: String,
    showExecute: Boolean,
    onContinue: () -> Unit,
    onExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable text area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = if (showExecute) Modifier.weight(1f) else Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.hint_panel_continue))
                }
                if (showExecute) {
                    Button(
                        onClick = onExecute,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.hint_panel_execute))
                    }
                }
            }
        }
    }
}

/**
 * Menu items for the Sudoku game
 */
enum class SudokuMenuItem {
    Settings,
    NewGame,
    Gestures
}

/**
 * Format elapsed time in HH:MM:SS format
 */
private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Material Design Compose keyboard for Sudoku input
 */
@Composable
fun ComposeKeyboard(
    buttons: List<KeyboardButtonState>,
    onButtonClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buttons.isEmpty()) return
    
    // Calculate grid dimensions (3x3 for 9 buttons, 4x4 for 16, etc.)
    val totalSymbols = buttons.size
    val gridSize = kotlin.math.ceil(kotlin.math.sqrt(totalSymbols.toDouble())).toInt()
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Create rows
            for (row in 0 until gridSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create columns
                    for (col in 0 until gridSize) {
                        val index = row * gridSize + col
                        if (index < buttons.size) {
                            val button = buttons[index]
                            FilledTonalButton(
                                onClick = { 
                                    if (button.isEnabled) {
                                        onButtonClick(button.symbol)
                                    }
                                },
                                enabled = button.isEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .alpha(if (button.isEnabled) 1f else 0.3f),  // Fade out disabled buttons
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (button.showCheckmark) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    contentColor = if (button.showCheckmark) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = if (button.showCheckmark) "✓" else button.displayText,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 24.sp
                                )
                            }
                        } else {
                            // Empty spacer for incomplete grid
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
