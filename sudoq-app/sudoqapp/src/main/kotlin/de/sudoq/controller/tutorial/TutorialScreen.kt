/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudoq.R

enum class TutorialSection {
    OVERVIEW,
    BASIC_SUDOKU,
    BASIC_ASSISTANCES,
    BASIC_ACTION_TREE,
    ADVANCED_TECHNIQUES
}

enum class TechniqueDetail {
    FULL_HOUSE,
    HIDDEN_SINGLE,
    NAKED_SINGLE,
    POINTING,
    CLAIMING,
    HIDDEN_PAIR,
    HIDDEN_TRIPLE,
    HIDDEN_QUADRUPLE,
    NAKED_PAIR,
    LOCKED_PAIR,
    NAKED_TRIPLE,
    LOCKED_TRIPLE,
    NAKED_QUADRUPLE
}

data class TechniqueItem(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val section: TutorialSection
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onBackPressed: () -> Unit
) {
    var currentSection by remember { mutableStateOf(TutorialSection.OVERVIEW) }
    var selectedTechnique by remember { mutableStateOf<TechniqueDetail?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            selectedTechnique != null -> getTechniqueTitle(selectedTechnique!!)
                            else -> when (currentSection) {
                                TutorialSection.OVERVIEW -> stringResource(R.string.sf_tutorial_title)
                                TutorialSection.BASIC_SUDOKU -> stringResource(R.string.sf_tutorial_sudoku_title)
                                TutorialSection.BASIC_ASSISTANCES -> stringResource(R.string.sf_tutorial_assistances_title)
                                TutorialSection.BASIC_ACTION_TREE -> stringResource(R.string.sf_tutorial_action_title)
                                TutorialSection.ADVANCED_TECHNIQUES -> stringResource(R.string.sf_tutorial_advanced_techniques_title)
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedTechnique != null -> selectedTechnique = null
                            currentSection == TutorialSection.OVERVIEW -> onBackPressed()
                            else -> currentSection = TutorialSection.OVERVIEW
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when {
            selectedTechnique != null -> TechniqueDetailScreen(
                technique = selectedTechnique!!,
                modifier = Modifier.padding(paddingValues)
            )
            else -> when (currentSection) {
                TutorialSection.OVERVIEW -> TutorialOverview(
                    modifier = Modifier.padding(paddingValues),
                    onSectionClick = { section -> currentSection = section }
                )
                TutorialSection.BASIC_SUDOKU -> BasicSudokuContent(
                    modifier = Modifier.padding(paddingValues)
                )
                TutorialSection.BASIC_ASSISTANCES -> BasicAssistancesContent(
                    modifier = Modifier.padding(paddingValues)
                )
                TutorialSection.BASIC_ACTION_TREE -> BasicActionTreeContent(
                    modifier = Modifier.padding(paddingValues)
                )
                TutorialSection.ADVANCED_TECHNIQUES -> AdvancedTechniquesList(
                    modifier = Modifier.padding(paddingValues),
                    onTechniqueClick = { technique -> selectedTechnique = technique }
                )
            }
        }
    }
}

@Composable
fun getTechniqueTitle(technique: TechniqueDetail): String {
    return when (technique) {
        TechniqueDetail.FULL_HOUSE -> stringResource(R.string.sf_tutorial_full_house_title)
        TechniqueDetail.HIDDEN_SINGLE -> stringResource(R.string.sf_tutorial_hidden_single_title)
        TechniqueDetail.NAKED_SINGLE -> stringResource(R.string.sf_tutorial_naked_single_title)
        TechniqueDetail.POINTING -> stringResource(R.string.sf_tutorial_pointing_title)
        TechniqueDetail.CLAIMING -> stringResource(R.string.sf_tutorial_claiming_title)
        TechniqueDetail.HIDDEN_PAIR -> stringResource(R.string.sf_tutorial_hidden_pair_title)
        TechniqueDetail.HIDDEN_TRIPLE -> stringResource(R.string.sf_tutorial_hidden_triple_title)
        TechniqueDetail.HIDDEN_QUADRUPLE -> stringResource(R.string.sf_tutorial_hidden_quadruple_title)
        TechniqueDetail.NAKED_PAIR -> stringResource(R.string.sf_tutorial_naked_pair_title)
        TechniqueDetail.LOCKED_PAIR -> stringResource(R.string.sf_tutorial_locked_pair_title)
        TechniqueDetail.NAKED_TRIPLE -> stringResource(R.string.sf_tutorial_naked_triple_title)
        TechniqueDetail.LOCKED_TRIPLE -> stringResource(R.string.sf_tutorial_locked_triple_title)
        TechniqueDetail.NAKED_QUADRUPLE -> stringResource(R.string.sf_tutorial_naked_quadruple_title)
    }
}

@Composable
fun TutorialOverview(
    modifier: Modifier = Modifier,
    onSectionClick: (TutorialSection) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Basic Tutorial Content
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_basic_content_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            BasicTutorialCard(
                title = stringResource(R.string.sf_tutorial_sudoku_title),
                icon = Icons.Default.Grid3x3,
                onClick = { onSectionClick(TutorialSection.BASIC_SUDOKU) }
            )
        }

        item {
            BasicTutorialCard(
                title = stringResource(R.string.sf_tutorial_assistances_title),
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = { onSectionClick(TutorialSection.BASIC_ASSISTANCES) }
            )
        }

        item {
            BasicTutorialCard(
                title = stringResource(R.string.sf_tutorial_action_title),
                icon = Icons.Default.History,
                onClick = { onSectionClick(TutorialSection.BASIC_ACTION_TREE) }
            )
        }

        // Advanced Techniques Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    
        // Introduction
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sf_tutorial_overview_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sf_tutorial_overview_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.sf_tutorial_advanced_techniques_title),
                description = stringResource(R.string.sf_tutorial_advanced_techniques_short_description),
                icon = Icons.Default.School,
                onClick = { onSectionClick(TutorialSection.ADVANCED_TECHNIQUES) }
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun BasicTutorialCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun BasicSudokuContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_sudoku_description1),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Text(
                text = stringResource(R.string.sf_tutorial_sudoku_description2),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Placeholder for Squiggly image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Squiggly Sudoku",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sf_tutorial_sudoku_squiggly),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Placeholder for X-Sudoku image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X-Sudoku",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sf_tutorial_sudoku_xsudoku),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BasicAssistancesContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_assistances_description1),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = stringResource(R.string.sf_tutorial_assistances_description2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Passive Assistances
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_assistances_passive_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Text(
                text = stringResource(R.string.sf_tutorial_assistances_passive_description1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_passive_markrowcolumn_title),
                description = stringResource(R.string.sf_tutorial_assistances_passive_markrowcolumn_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_passive_autoadjustnotes_title),
                description = stringResource(R.string.sf_tutorial_assistances_passive_autoadjustnotes_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_passive_markwrongsymbols_title),
                description = stringResource(R.string.sf_tutorial_assistances_passive_markwrongsymbols_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_passive_inputassistance_title),
                description = stringResource(R.string.sf_tutorial_assistances_passive_inputassistance_description1)
            )
        }

        // Active Assistances
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sf_tutorial_assistances_active_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Text(
                text = stringResource(R.string.sf_tutorial_assistances_active_description1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_active_solverandomcell_title),
                description = stringResource(R.string.sf_tutorial_assistances_active_solverandomcell_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_active_solvecurrentcell_title),
                description = stringResource(R.string.sf_tutorial_assistances_active_solvecurrentcell_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_active_check_title),
                description = stringResource(R.string.sf_tutorial_assistances_active_check_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_active_backtolastcorrect_title),
                description = stringResource(R.string.sf_tutorial_assistances_active_backtolastcorrect_description1)
            )
        }

        item {
            AssistanceItem(
                title = stringResource(R.string.sf_tutorial_assistances_active_surrender_title),
                description = stringResource(R.string.sf_tutorial_assistances_active_surrender_description1)
            )
        }
    }
}

@Composable
fun BasicActionTreeContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_action_description1),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.sf_tutorial_action_description2),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.sf_tutorial_action_description3),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            ActionTreeExampleCard(
                icon = Icons.Default.Circle,
                description = stringResource(R.string.sf_tutorial_action_state)
            )
        }

        item {
            ActionTreeExampleCard(
                icon = Icons.Default.Bookmark,
                description = stringResource(R.string.sf_tutorial_action_bookmark)
            )
        }

        item {
            ActionTreeExampleCard(
                icon = Icons.Default.CheckCircle,
                description = stringResource(R.string.sf_tutorial_action_current_state)
            )
        }

        item {
            ActionTreeExampleCard(
                icon = Icons.AutoMirrored.Filled.CallSplit,
                description = stringResource(R.string.sf_tutorial_action_branching)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sf_tutorial_action_redoundo_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.sf_tutorial_action_redoundo_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistanceItem(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionTreeExampleCard(
    icon: ImageVector,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdvancedTechniquesList(
    modifier: Modifier = Modifier,
    onTechniqueClick: (TechniqueDetail) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_advanced_techniques_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Singles
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_singles_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_full_house_title),
                description = stringResource(R.string.sf_tutorial_full_house_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_basic),
                onClick = { onTechniqueClick(TechniqueDetail.FULL_HOUSE) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_hidden_single_title),
                description = stringResource(R.string.sf_tutorial_hidden_single_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_basic),
                onClick = { onTechniqueClick(TechniqueDetail.HIDDEN_SINGLE) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_naked_single_title),
                description = stringResource(R.string.sf_tutorial_naked_single_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_basic),
                onClick = { onTechniqueClick(TechniqueDetail.NAKED_SINGLE) }
            )
        }

        // Intersections
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_intersections_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_pointing_title),
                description = stringResource(R.string.sf_tutorial_pointing_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_intermediate),
                onClick = { onTechniqueClick(TechniqueDetail.POINTING) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_claiming_title),
                description = stringResource(R.string.sf_tutorial_claiming_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_intermediate),
                onClick = { onTechniqueClick(TechniqueDetail.CLAIMING) }
            )
        }

        // Hidden Subsets
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_hidden_subsets_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_hidden_pair_title),
                description = stringResource(R.string.sf_tutorial_hidden_pair_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_intermediate),
                onClick = { onTechniqueClick(TechniqueDetail.HIDDEN_PAIR) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_hidden_triple_title),
                description = stringResource(R.string.sf_tutorial_hidden_triple_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_advanced),
                onClick = { onTechniqueClick(TechniqueDetail.HIDDEN_TRIPLE) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_hidden_quadruple_title),
                description = stringResource(R.string.sf_tutorial_hidden_quadruple_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_advanced),
                onClick = { onTechniqueClick(TechniqueDetail.HIDDEN_QUADRUPLE) }
            )
        }

        // Naked Subsets
        item {
            Text(
                text = stringResource(R.string.sf_tutorial_naked_subsets_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_naked_pair_title),
                description = stringResource(R.string.sf_tutorial_naked_pair_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_intermediate),
                onClick = { onTechniqueClick(TechniqueDetail.NAKED_PAIR) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_locked_pair_title),
                description = stringResource(R.string.sf_tutorial_locked_pair_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_intermediate),
                onClick = { onTechniqueClick(TechniqueDetail.LOCKED_PAIR) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_naked_triple_title),
                description = stringResource(R.string.sf_tutorial_naked_triple_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_advanced),
                onClick = { onTechniqueClick(TechniqueDetail.NAKED_TRIPLE) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_locked_triple_title),
                description = stringResource(R.string.sf_tutorial_locked_triple_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_advanced),
                onClick = { onTechniqueClick(TechniqueDetail.LOCKED_TRIPLE) }
            )
        }

        item {
            TechniqueCardWithDescription(
                title = stringResource(R.string.sf_tutorial_naked_quadruple_title),
                description = stringResource(R.string.sf_tutorial_naked_quadruple_short),
                difficulty = stringResource(R.string.sf_tutorial_difficulty_expert),
                onClick = { onTechniqueClick(TechniqueDetail.NAKED_QUADRUPLE) }
            )
        }
    }
}

@Composable
fun TechniqueCardWithDescription(
    title: String,
    description: String,
    difficulty: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (difficulty) {
                        "Basic", "基础", "Grundlegend", "Base" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        "Intermediate", "中级", "Mittelstufe", "Intermédiaire" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        "Advanced", "高级", "Fortgeschritten", "Avancé" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        text = difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2
            )
        }
    }
}

@Composable
fun TechniqueDetailScreen(
    technique: TechniqueDetail,
    modifier: Modifier = Modifier
) {
    val (titleRes, descriptionRes, difficulty) = when (technique) {
        TechniqueDetail.FULL_HOUSE -> Triple(
            R.string.sf_tutorial_full_house_title,
            R.string.sf_tutorial_full_house_description,
            R.string.sf_tutorial_difficulty_basic
        )
        TechniqueDetail.HIDDEN_SINGLE -> Triple(
            R.string.sf_tutorial_hidden_single_title,
            R.string.sf_tutorial_hidden_single_description,
            R.string.sf_tutorial_difficulty_basic
        )
        TechniqueDetail.NAKED_SINGLE -> Triple(
            R.string.sf_tutorial_naked_single_title,
            R.string.sf_tutorial_naked_single_description,
            R.string.sf_tutorial_difficulty_basic
        )
        TechniqueDetail.POINTING -> Triple(
            R.string.sf_tutorial_pointing_title,
            R.string.sf_tutorial_pointing_description,
            R.string.sf_tutorial_difficulty_intermediate
        )
        TechniqueDetail.CLAIMING -> Triple(
            R.string.sf_tutorial_claiming_title,
            R.string.sf_tutorial_claiming_description,
            R.string.sf_tutorial_difficulty_intermediate
        )
        TechniqueDetail.HIDDEN_PAIR -> Triple(
            R.string.sf_tutorial_hidden_pair_title,
            R.string.sf_tutorial_hidden_pair_description,
            R.string.sf_tutorial_difficulty_intermediate
        )
        TechniqueDetail.HIDDEN_TRIPLE -> Triple(
            R.string.sf_tutorial_hidden_triple_title,
            R.string.sf_tutorial_hidden_triple_description,
            R.string.sf_tutorial_difficulty_advanced
        )
        TechniqueDetail.HIDDEN_QUADRUPLE -> Triple(
            R.string.sf_tutorial_hidden_quadruple_title,
            R.string.sf_tutorial_hidden_quadruple_description,
            R.string.sf_tutorial_difficulty_advanced
        )
        TechniqueDetail.NAKED_PAIR -> Triple(
            R.string.sf_tutorial_naked_pair_title,
            R.string.sf_tutorial_naked_pair_description,
            R.string.sf_tutorial_difficulty_intermediate
        )
        TechniqueDetail.LOCKED_PAIR -> Triple(
            R.string.sf_tutorial_locked_pair_title,
            R.string.sf_tutorial_locked_pair_description,
            R.string.sf_tutorial_difficulty_intermediate
        )
        TechniqueDetail.NAKED_TRIPLE -> Triple(
            R.string.sf_tutorial_naked_triple_title,
            R.string.sf_tutorial_naked_triple_description,
            R.string.sf_tutorial_difficulty_advanced
        )
        TechniqueDetail.LOCKED_TRIPLE -> Triple(
            R.string.sf_tutorial_locked_triple_title,
            R.string.sf_tutorial_locked_triple_description,
            R.string.sf_tutorial_difficulty_advanced
        )
        TechniqueDetail.NAKED_QUADRUPLE -> Triple(
            R.string.sf_tutorial_naked_quadruple_title,
            R.string.sf_tutorial_naked_quadruple_description,
            R.string.sf_tutorial_difficulty_expert
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (stringResource(difficulty)) {
                    "Basic", "基础", "Grundlegend", "Base" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    "Intermediate", "中级", "Mittelstufe", "Intermédiaire" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    "Advanced", "高级", "Fortgeschritten", "Avancé" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                }
            ) {
                Text(
                    text = stringResource(difficulty),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
