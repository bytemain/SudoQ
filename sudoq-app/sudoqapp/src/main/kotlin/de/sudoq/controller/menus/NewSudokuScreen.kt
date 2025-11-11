package de.sudoq.controller.menus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.sudoq.R
import de.sudoq.model.sudoku.complexity.Complexity
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes

data class NewSudokuState(
    val selectedType: SudokuTypes? = null,
    val selectedComplexity: Complexity? = null,
    val availableTypes: List<SudokuTypes> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSudokuScreen(
    state: NewSudokuState,
    onTypeSelected: (SudokuTypes) -> Unit,
    onComplexitySelected: (Complexity) -> Unit,
    onStartGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.sf_sudokupreferences_title))
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.sf_mainmenu_preferences)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sudoku Type Selection
            SudokuTypeSection(
                availableTypes = state.availableTypes,
                selectedType = state.selectedType,
                onTypeSelected = onTypeSelected
            )
            
            // Complexity Selection
            ComplexitySection(
                selectedComplexity = state.selectedComplexity,
                onComplexitySelected = onComplexitySelected
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Start Game Button
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.selectedType != null && state.selectedComplexity != null && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sf_sudokupreferences_start),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SudokuTypeSection(
    availableTypes: List<SudokuTypes>,
    selectedType: SudokuTypes?,
    onTypeSelected: (SudokuTypes) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.sf_sudokupreferences_type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        if (availableTypes.isEmpty()) {
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SudokuTypeDropdown(
                availableTypes = availableTypes,
                selectedType = selectedType,
                onTypeSelected = onTypeSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuTypeDropdown(
    availableTypes: List<SudokuTypes>,
    selectedType: SudokuTypes?,
    onTypeSelected: (SudokuTypes) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedType?.let { Utility.type2string(context, it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.select_sudoku_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(Utility.type2string(context, type) ?: type.name) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun ComplexitySection(
    selectedComplexity: Complexity?,
    onComplexitySelected: (Complexity) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.sf_sudokupreferences_complexity),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        ComplexitySelector(
            selectedComplexity = selectedComplexity,
            onComplexitySelected = onComplexitySelected
        )
    }
}

@Composable
fun ComplexitySelector(
    selectedComplexity: Complexity?,
    onComplexitySelected: (Complexity) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Complexity.entries.forEach { complexity ->
            ComplexityCard(
                complexity = complexity,
                isSelected = complexity == selectedComplexity,
                onClick = { onComplexitySelected(complexity) }
            )
        }
    }
}

@Composable
fun ComplexityCard(
    complexity: Complexity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = getComplexityString(complexity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = getComplexityDescription(complexity),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected) {
                RadioButton(
                    selected = true,
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun getComplexityString(complexity: Complexity): String {
    return stringResource(
        when (complexity) {
            Complexity.easy -> R.string.complexity_easy
            Complexity.medium -> R.string.complexity_medium
            Complexity.difficult -> R.string.complexity_difficult
            Complexity.infernal -> R.string.complexity_infernal
            Complexity.arbitrary -> R.string.complexity_arbitrary
        }
    )
}

@Composable
private fun getComplexityDescription(complexity: Complexity): String {
    return stringResource(
        when (complexity) {
            Complexity.easy -> R.string.complexity_easy_desc
            Complexity.medium -> R.string.complexity_medium_desc
            Complexity.difficult -> R.string.complexity_difficult_desc
            Complexity.infernal -> R.string.complexity_infernal_desc
            Complexity.arbitrary -> R.string.complexity_arbitrary_desc
        }
    )
}
