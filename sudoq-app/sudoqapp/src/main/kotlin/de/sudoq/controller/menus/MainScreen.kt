package de.sudoq.controller.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import de.sudoq.R

data class MainScreenState(
    val profileName: String = "",
    val hasCurrentGame: Boolean = false,
    val currentGameProgress: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val currentScore: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainScreenState,
    onNewGameClick: () -> Unit,
    onContinueGameClick: () -> Unit,
    onLoadGameClick: () -> Unit,
    onProfileClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = state.profileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.sf_mainmenu_profile)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            SplitExtendedFAB(
                hasCurrentGame = state.hasCurrentGame,
                onMainAction = if (state.hasCurrentGame) onContinueGameClick else onNewGameClick,
                onNewGameClick = onNewGameClick,
                onContinueGameClick = onContinueGameClick
            )
        }
    ) { paddingValues ->
        if (isLandscape) {
            // Landscape: Two columns
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.hasCurrentGame) {
                        ContinueGameCard(
                            progress = state.currentGameProgress,
                            onClick = onContinueGameClick
                        )
                    }
                    QuickActionsSection(
                        onLoadGameClick = onLoadGameClick,
                        onStatisticsClick = onStatisticsClick,
                        onTutorialClick = onTutorialClick
                    )
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    StatisticsOverviewCard(
                        gamesPlayed = state.gamesPlayed,
                        gamesWon = state.gamesWon,
                        currentScore = state.currentScore,
                        onClick = onStatisticsClick
                    )
                }
            }
        } else {
            // Portrait: Single column
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Continue Game Card
                if (state.hasCurrentGame) {
                    item {
                        ContinueGameCard(
                            progress = state.currentGameProgress,
                            onClick = onContinueGameClick
                        )
                    }
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        onLoadGameClick = onLoadGameClick,
                        onStatisticsClick = onStatisticsClick,
                        onTutorialClick = onTutorialClick
                    )
                }

                // Statistics Overview
                item {
                    StatisticsOverviewCard(
                        gamesPlayed = state.gamesPlayed,
                        gamesWon = state.gamesWon,
                        currentScore = state.currentScore,
                        onClick = onStatisticsClick
                    )
                }
            }
        }
    }
}

@Composable
fun SplitExtendedFAB(
    hasCurrentGame: Boolean,
    onMainAction: () -> Unit,
    onNewGameClick: () -> Unit,
    onContinueGameClick: () -> Unit
) {
    if (hasCurrentGame) {
        // Split button: Continue | New
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Continue button
                Surface(
                    onClick = onContinueGameClick,
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.sf_mainmenu_continue),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                )
                
                // New game button
                Surface(
                    onClick = onNewGameClick,
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.sf_mainmenu_new_sudoku),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    } else {
        // Single button: New Game
        ExtendedFloatingActionButton(
            onClick = onNewGameClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            text = {
                Text(stringResource(R.string.sf_mainmenu_new_sudoku))
            }
        )
    }
}

@Composable
fun ContinueGameCard(
    progress: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sf_mainmenu_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.game_in_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$progress% ${stringResource(R.string.completed)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuickActionsSection(
    onLoadGameClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.List,
                label = stringResource(R.string.sf_mainmenu_load_sudoku),
                onClick = onLoadGameClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionCard(
                icon = Icons.Default.BarChart,
                label = stringResource(R.string.sf_mainmenu_statistic),
                onClick = onStatisticsClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionCard(
                icon = Icons.Default.School,
                label = stringResource(R.string.sf_mainmenu_tutorial),
                onClick = onTutorialClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatisticsOverviewCard(
    gamesPlayed: Int,
    gamesWon: Int,
    currentScore: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sf_mainmenu_statistic),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = stringResource(R.string.games_played),
                    value = gamesPlayed.toString()
                )
                
                StatisticItem(
                    label = stringResource(R.string.games_won),
                    value = gamesWon.toString()
                )
                
                StatisticItem(
                    label = stringResource(R.string.score),
                    value = currentScore.toString()
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
