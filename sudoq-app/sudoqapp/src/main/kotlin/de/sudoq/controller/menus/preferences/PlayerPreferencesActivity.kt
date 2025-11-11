/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus.preferences

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import de.sudoq.R
import de.sudoq.model.game.Assistances
import de.sudoq.model.profile.ProfileManager
import de.sudoq.model.profile.ProfileSingleton.Companion.getInstance
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo
import de.sudoq.view.theme.SudoQTheme
import de.sudoq.view.theme.ThemeManager

/**
 * Activity um Profile zu bearbeiten und zu verwalten
 * aufgerufen im Hauptmenü 4. Button
 * 
 * Migrated to Jetpack Compose for modern UI
 */
class PlayerPreferencesActivity : PreferencesActivity() {
    
    /**
     * Wird aufgerufen, falls die Activity zum ersten Mal gestartet wird.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val profile = getInstance(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        profile.registerListener(this)
        
        setContent {
            val themeColor = ThemeManager.loadThemeColor(this)
            val darkMode = ThemeManager.loadDarkMode(this)
            var showProfileDialog by remember { mutableStateOf(false) }
            
            SudoQTheme(
                themeColor = themeColor,
                darkTheme = darkMode
            ) {
                var preferencesData by remember { mutableStateOf(loadPreferencesData()) }
                
                PlayerPreferencesScreen(
                    data = preferencesData,
                    onProfileNameChange = { newName ->
                        profile.name = newName
                        preferencesData = preferencesData.copy(profileName = newName)
                    },
                    onGestureChange = { value ->
                        profile.isGestureActive = value
                        preferencesData = preferencesData.copy(gestureActive = value)
                    },
                    onAutoAdjustNotesChange = { value ->
                        profile.setAssistance(Assistances.autoAdjustNotes, value)
                        preferencesData = preferencesData.copy(autoAdjustNotes = value)
                    },
                    onMarkRowColumnChange = { value ->
                        profile.setAssistance(Assistances.markRowColumn, value)
                        preferencesData = preferencesData.copy(markRowColumn = value)
                    },
                    onMarkWrongSymbolChange = { value ->
                        profile.setAssistance(Assistances.markWrongSymbol, value)
                        preferencesData = preferencesData.copy(markWrongSymbol = value)
                    },
                    onRestrictCandidatesChange = { value ->
                        profile.setAssistance(Assistances.restrictCandidates, value)
                        preferencesData = preferencesData.copy(restrictCandidates = value)
                    },
                    onAutoFillUniqueCandidatesChange = { value ->
                        profile.setAssistance(Assistances.autoFillUniqueCandidates, value)
                        preferencesData = preferencesData.copy(autoFillUniqueCandidates = value)
                    },
                    onShowCompletedDigitsChange = { value ->
                        profile.setAssistance(Assistances.showCompletedDigits, value)
                        preferencesData = preferencesData.copy(showCompletedDigits = value)
                    },
                    onProvideHintsChange = { value ->
                        profile.setAssistance(Assistances.provideHints, value)
                        preferencesData = preferencesData.copy(provideHints = value)
                    },
                    onBackClick = {
                        profile.saveChanges()
                        finish()
                    },
                    onNewProfileClick = {
                        createProfile()
                        preferencesData = loadPreferencesData()
                    },
                    onDeleteProfileClick = {
                        deleteProfile()
                        preferencesData = loadPreferencesData()
                    },
                    onSwitchProfileClick = {
                        showProfileDialog = true
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, AppSettingsActivity::class.java))
                    }
                )
                
                // Profile selection dialog
                if (showProfileDialog) {
                    val profiles = loadProfilesList()
                    val currentProfileId = profile.currentProfileID
                    
                    de.sudoq.controller.menus.ProfileSelectionDialog(
                        profiles = profiles,
                        currentProfileId = currentProfileId,
                        onProfileSelected = { profileId ->
                            profile.changeProfile(profileId)
                            preferencesData = loadPreferencesData()
                        },
                        onDismiss = { showProfileDialog = false }
                    )
                }
            }
        }
    }
    /**
     * Load current preferences data from profile
     */
    private fun loadPreferencesData(): PlayerPreferencesData {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val profile = getInstance(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        
        return PlayerPreferencesData(
            profileName = profile.name ?: "",
            gestureActive = profile.isGestureActive,
            autoAdjustNotes = profile.getAssistance(Assistances.autoAdjustNotes),
            markRowColumn = profile.getAssistance(Assistances.markRowColumn),
            markWrongSymbol = profile.getAssistance(Assistances.markWrongSymbol),
            restrictCandidates = profile.getAssistance(Assistances.restrictCandidates),
            autoFillUniqueCandidates = profile.getAssistance(Assistances.autoFillUniqueCandidates),
            showCompletedDigits = profile.getAssistance(Assistances.showCompletedDigits),
            provideHints = profile.getAssistance(Assistances.provideHints),
            canDeleteProfile = profile.numberOfAvailableProfiles > 1,
            canSwitchProfile = profile.numberOfAvailableProfiles > 1
        )
    }
    
    /**
     * Load list of all profiles
     */
    private fun loadProfilesList(): List<de.sudoq.controller.menus.ProfileInfo> {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val pm = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        pm.loadCurrentProfile()
        
        val ids = pm.profilesIdList
        val names = pm.profilesNameList
        
        return ids.zip(names).map { (id, name) ->
            de.sudoq.controller.menus.ProfileInfo(id, name)
        }
    }
    
    override fun refreshValues() {
        // No longer needed with Compose state management
    }
    
    override fun adjustValuesAndSave() {
        saveToProfile()
    }
    
    override fun saveToProfile() {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val profile = getInstance(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        profile.saveChanges()
    }

    /**
     * Erstellt ein neues Profil
     */
    private fun createProfile() {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val p = getInstance(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        
        var newProfileName = getString(R.string.profile_preference_new_profile)
        var newIndex = 0
        
        // Find the next available profile number
        val l: List<String> = p.profilesNameList
        for (s in l) {
            if (s.startsWith(newProfileName)) {
                val currentIndex = s.substring(newProfileName.length)
                try {
                    val otherIndex = if (currentIndex == "") 0 else currentIndex.toInt()
                    newIndex = if (newIndex <= otherIndex) otherIndex + 1 else newIndex
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        }
        
        if (newIndex != 0) newProfileName += newIndex
        p.createAnotherProfile()
        p.name = newProfileName
        p.saveChanges()
    }
    /**
     * Löscht das ausgewählte Profil
     */
    private fun deleteProfile() {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val p = getInstance(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        p.deleteProfile()
    }

    companion object {
        /**
         * Konstante um anzuzeigen, dass nur die Assistences konfiguriert werden sollen
         */
        const val INTENT_ONLYASSISTANCES = "only_assistances"

        /**
         * Konstante um anzuzeigen, dass nur ein neues Profil erzeugt werden soll
         */
        const val INTENT_CREATEPROFILE = "create_profile"
    }
}
