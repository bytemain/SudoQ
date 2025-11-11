package de.sudoq.controller.menus.preferences

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.controller.menus.Utility
import de.sudoq.model.profile.ProfileManager
import de.sudoq.model.sudoku.sudokuTypes.SudokuTypes
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.view.theme.SudoQTheme
import de.sudoq.view.theme.ThemeColor
import de.sudoq.view.theme.ThemeManager

/**
 * Unified app settings activity combining theme and advanced settings
 */
class AppSettingsActivity : SudoqCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load current restricted types and debug setting
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val pm = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        pm.loadCurrentProfile()
        val initialRestrictedTypes = pm.assistances.wantedTypesList
        val initialDebugEnabled = pm.appSettings.isDebugSet
        val initialKeyboardLayout = pm.appSettings.keyboardLayoutMode
        
        setContent {
            var themeColor by remember { mutableStateOf(ThemeManager.loadThemeColor(this)) }
            var isDarkMode by remember { mutableStateOf(ThemeManager.loadDarkMode(this)) }
            var currentLanguage by remember { mutableStateOf(LanguageUtility.loadLanguageCodeFromPreferences(this)) }
            var keyboardLayoutMode by remember { mutableStateOf(initialKeyboardLayout) }
            var showRestrictDialog by remember { mutableStateOf(false) }
            var restrictedTypes by remember { mutableStateOf(initialRestrictedTypes) }
            var debugClickCount by remember { mutableStateOf(0) }
            var showDebugOption by remember { mutableStateOf(initialDebugEnabled) }
            var isDebugEnabled by remember { mutableStateOf(initialDebugEnabled) }
            
            SudoQTheme(
                themeColor = themeColor,
                darkTheme = isDarkMode
            ) {
                AppSettingsScreen(
                    themeColor = themeColor,
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    keyboardLayoutMode = keyboardLayoutMode,
                    showDebugOption = showDebugOption,
                    isDebugEnabled = isDebugEnabled,
                    onThemeColorChange = { color ->
                        themeColor = color
                        ThemeManager.saveThemeColor(this, color)
                    },
                    onDarkModeChange = { enabled ->
                        isDarkMode = enabled
                        ThemeManager.saveDarkMode(this, enabled)
                    },
                    onKeyboardLayoutChange = { mode ->
                        keyboardLayoutMode = mode
                        // Save to profile
                        val pm2 = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
                        pm2.loadCurrentProfile()
                        pm2.appSettings.keyboardLayoutMode = mode
                        pm2.saveChanges()
                    },
                    onDebugChange = { enabled ->
                        isDebugEnabled = enabled
                        // Save to profile
                        val pm2 = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
                        pm2.loadCurrentProfile()
                        pm2.setDebugActive(enabled)
                        pm2.saveChanges()
                    },
                    onLanguageChange = { language ->
                        currentLanguage = language
                        LanguageUtility.saveLanguageCodeToPreferences(this, language)
                        // Restart activity to apply language change
                        recreate()
                    },
                    onRestrictTypesClick = {
                        showRestrictDialog = true
                    },
                    onTitleClick = {
                        debugClickCount++
                        if (debugClickCount >= 10 && !showDebugOption) {
                            showDebugOption = true
                        }
                    },
                    onBackClick = { finish() }
                )
                
                // Restrict Types Dialog
                if (showRestrictDialog) {
                    RestrictTypesDialog(
                        currentTypes = restrictedTypes,
                        onDismiss = { showRestrictDialog = false },
                        onConfirm = { newTypes ->
                            restrictedTypes = newTypes
                            // Save to profile
                            val pm2 = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
                            pm2.loadCurrentProfile()
                            pm2.assistances.wantedTypesList.clear()
                            pm2.assistances.wantedTypesList.addAll(newTypes)
                            pm2.saveChanges()
                            showRestrictDialog = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    themeColor: ThemeColor,
    isDarkMode: Boolean,
    currentLanguage: LanguageCode,
    keyboardLayoutMode: String,
    showDebugOption: Boolean,
    isDebugEnabled: Boolean,
    onThemeColorChange: (ThemeColor) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onKeyboardLayoutChange: (String) -> Unit,
    onDebugChange: (Boolean) -> Unit,
    onLanguageChange: (LanguageCode) -> Unit,
    onRestrictTypesClick: () -> Unit,
    onTitleClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.app_settings),
                        modifier = Modifier.clickable(
                            onClick = onTitleClick,
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        )
                    )
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Appearance Section
            item {
                SettingsSectionHeader(stringResource(R.string.settings_appearance))
            }
            
            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.dark_mode),
                    subtitle = stringResource(R.string.dark_mode_desc),
                    checked = isDarkMode,
                    onCheckedChange = onDarkModeChange
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            item {
                ThemeColorPicker(
                    selectedColor = themeColor,
                    onColorSelect = onThemeColorChange
                )
            }
            
            // Game Settings Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(stringResource(R.string.settings_game))
            }
            
            item {
                KeyboardLayoutDropdown(
                    currentMode = keyboardLayoutMode,
                    onModeChange = onKeyboardLayoutChange
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            item {
                SettingsNavigationItem(
                    title = stringResource(R.string.restrict_sudoku_types),
                    subtitle = stringResource(R.string.restrict_sudoku_types_desc),
                    onClick = onRestrictTypesClick
                )
            }
            
            // Debug option (hidden by default, shown after 10 taps on title)
            if (showDebugOption) {
                item {
                    SettingsSwitchItem(
                        title = "Debug Mode",
                        subtitle = "Enable debug features",
                        checked = isDebugEnabled,
                        onCheckedChange = onDebugChange
                    )
                }
            }
            
            // Language Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(stringResource(R.string.settings_language))
            }
            
            item {
                LanguageDropdown(
                    currentLanguage = currentLanguage,
                    onLanguageChange = onLanguageChange
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeColorPicker(
    selectedColor: ThemeColor,
    onColorSelect: (ThemeColor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.theme_color),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.theme_color_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeColor.values().forEach { color ->
                ColorCircle(
                    themeColor = color,
                    isSelected = color == selectedColor,
                    onSelect = { onColorSelect(color) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ColorCircle(
    themeColor: ThemeColor,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Define colors for visual display (matching theme colors)
    val displayColor = when (themeColor) {
        ThemeColor.GREEN -> androidx.compose.ui.graphics.Color(0xFF2E7D32) // Material Green 800
        ThemeColor.BLUE -> androidx.compose.ui.graphics.Color(0xFF1565C0) // Material Blue 800
        ThemeColor.PURPLE -> androidx.compose.ui.graphics.Color(0xFF6750A4) // Purple
        ThemeColor.RED -> androidx.compose.ui.graphics.Color(0xFFC62828) // Material Red 800
        ThemeColor.ORANGE -> androidx.compose.ui.graphics.Color(0xFFE65100) // Material Orange 900
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onSelect),
            shape = MaterialTheme.shapes.extraLarge,
            color = displayColor,
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(
                    3.dp,
                    MaterialTheme.colorScheme.primary
                )
            } else {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            }
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        val colorName = when (themeColor) {
            ThemeColor.GREEN -> stringResource(R.string.color_green)
            ThemeColor.BLUE -> stringResource(R.string.color_blue)
            ThemeColor.PURPLE -> stringResource(R.string.color_purple)
            ThemeColor.RED -> stringResource(R.string.color_red)
            ThemeColor.ORANGE -> stringResource(R.string.color_orange)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    currentLanguage: LanguageCode,
    onLanguageChange: (LanguageCode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val languages = listOf(
        LanguageCode.system,
        LanguageCode.en,
        LanguageCode.de,
        LanguageCode.fr,
        LanguageCode.zh
    )
    
    val languageNames = mapOf(
        LanguageCode.system to R.string.language_system,
        LanguageCode.en to R.string.language_english,
        LanguageCode.de to R.string.language_german,
        LanguageCode.fr to R.string.language_french,
        LanguageCode.zh to R.string.language_chinese
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.language_settings),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.language_settings_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = stringResource(languageNames[currentLanguage] ?: R.string.language_system),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(languageNames[language] ?: R.string.language_system))
                        },
                        onClick = {
                            onLanguageChange(language)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * Dialog for selecting which sudoku types to show
 */
@Composable
fun RestrictTypesDialog(
    currentTypes: ArrayList<SudokuTypes>,
    onDismiss: () -> Unit,
    onConfirm: (ArrayList<SudokuTypes>) -> Unit
) {
    val context = LocalContext.current
    val allTypes = SudokuTypes.values().toList()
    
    // State to track selected types
    var selectedTypes by remember { mutableStateOf(currentTypes.toSet()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.restrict_sudoku_types))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = stringResource(R.string.restrict_sudoku_types_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(allTypes.size) { index ->
                        val type = allTypes[index]
                        val typeName = Utility.type2string(context, type) ?: type.name
                        val isChecked = selectedTypes.contains(type)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTypes = if (isChecked) {
                                        // Don't allow unchecking if it's the last item
                                        if (selectedTypes.size > 1) {
                                            selectedTypes - type
                                        } else {
                                            selectedTypes // Keep at least one type
                                        }
                                    } else {
                                        selectedTypes + type
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedTypes = if (checked) {
                                        selectedTypes + type
                                    } else {
                                        // Don't allow unchecking if it's the last item
                                        if (selectedTypes.size > 1) {
                                            selectedTypes - type
                                        } else {
                                            selectedTypes // Keep at least one type
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = typeName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(ArrayList(selectedTypes))
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun KeyboardLayoutDropdown(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("grid", "horizontal")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_keyboard_layout),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (currentMode) {
                    "horizontal" -> stringResource(R.string.keyboard_layout_horizontal)
                    else -> stringResource(R.string.keyboard_layout_grid)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (mode) {
                                "horizontal" -> stringResource(R.string.keyboard_layout_horizontal)
                                else -> stringResource(R.string.keyboard_layout_grid)
                            }
                        )
                    },
                    onClick = {
                        onModeChange(mode)
                        expanded = false
                    },
                    leadingIcon = if (mode == currentMode) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}
