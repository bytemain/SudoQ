package de.sudoq.controller.menus.preferences

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.view.theme.SudoQTheme
import de.sudoq.view.theme.ThemeColor
import de.sudoq.view.theme.ThemeManager

/**
 * Activity for theme customization
 */
class ThemePreferencesActivity : SudoqCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var selectedColor by remember { mutableStateOf(ThemeManager.loadThemeColor(this)) }
            var isDarkMode by remember { mutableStateOf(ThemeManager.loadDarkMode(this)) }
            
            SudoQTheme(
                themeColor = selectedColor,
                darkTheme = isDarkMode
            ) {
                ThemePreferencesScreen(
                    selectedColor = selectedColor,
                    isDarkMode = isDarkMode,
                    onColorSelect = { color ->
                        selectedColor = color
                        ThemeManager.saveThemeColor(this, color)
                    },
                    onDarkModeToggle = { enabled ->
                        isDarkMode = enabled
                        ThemeManager.saveDarkMode(this, enabled)
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePreferencesScreen(
    selectedColor: ThemeColor,
    isDarkMode: Boolean,
    onColorSelect: (ThemeColor) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dark Mode Toggle
            Card(
                modifier = Modifier.fillMaxWidth()
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
                            text = stringResource(R.string.dark_mode),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.dark_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeToggle
                    )
                }
            }
            
            // Theme Color Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.theme_color),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = stringResource(R.string.theme_color_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Color grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(ThemeColor.values().toList()) { color ->
                            ColorOption(
                                themeColor = color,
                                isSelected = color == selectedColor,
                                onSelect = { onColorSelect(color) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    themeColor: ThemeColor,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colorPreview = when (themeColor) {
        ThemeColor.GREEN -> Color(0xFF2E7D32)
        ThemeColor.BLUE -> Color(0xFF1565C0)
        ThemeColor.PURPLE -> Color(0xFF6750A4)
        ThemeColor.RED -> Color(0xFFC62828)
        ThemeColor.ORANGE -> Color(0xFFE65100)
    }
    
    val colorName = when (themeColor) {
        ThemeColor.GREEN -> stringResource(R.string.color_green)
        ThemeColor.BLUE -> stringResource(R.string.color_blue)
        ThemeColor.PURPLE -> stringResource(R.string.color_purple)
        ThemeColor.RED -> stringResource(R.string.color_red)
        ThemeColor.ORANGE -> stringResource(R.string.color_orange)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorPreview)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .clickable { onSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Text(
            text = colorName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
