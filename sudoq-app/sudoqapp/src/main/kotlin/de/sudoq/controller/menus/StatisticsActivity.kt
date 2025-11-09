/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2012  Heiko Klare, Julian Geppert, Jan-Bernhard Kordaß, Jonathan Kieling, Tim Zeitz, Timo Abele, Jiacheng Li
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import de.sudoq.R
import de.sudoq.controller.SudoqCompatActivity
import de.sudoq.controller.sudoku.SudokuActivity.Companion.getTimeString
import de.sudoq.model.profile.ProfileManager
import de.sudoq.model.profile.Statistics
import de.sudoq.persistence.profile.ProfileRepo
import de.sudoq.persistence.profile.ProfilesListRepo

/**
 * This class demonstrates how to migrate traditional XML layouts to Jetpack Compose
 * 
 * Original implementation: XML layout file (statistics.xml) + findViewById
 * New implementation: Jetpack Compose composable functions
 * 
 * Migration advantages:
 * 1. More concise code - no need for findViewById
 * 2. Declarative UI - directly describe what the UI should look like
 * 3. Real-time preview - see effects immediately in Android Studio
 * 4. Better type safety
 */
class StatisticsActivity : SudoqCompatActivity() {
    /** Methods  */
    
    /**
     * Get statistics data
     */
    private fun getStatisticsData(): StatisticsData {
        val profilesDir = getDir(getString(R.string.path_rel_profiles), MODE_PRIVATE)
        val pm = ProfileManager(profilesDir, ProfileRepo(profilesDir), ProfilesListRepo(profilesDir))
        check(!pm.noProfiles()) { 
            "there are no profiles. this is unexpected. they should be initialized in splashActivity" 
        }
        pm.loadCurrentProfile()
        
        val timeRecordInSecs = pm.getStatistic(Statistics.fastestSolvingTime)
        val timeString = if (timeRecordInSecs != ProfileManager.INITIAL_TIME_RECORD) {
            getTimeString(timeRecordInSecs)
        } else {
            "---"
        }
        
        return StatisticsData(
            playedSudokus = pm.getStatistic(Statistics.playedSudokus).toString(),
            playedEasySudokus = pm.getStatistic(Statistics.playedEasySudokus).toString(),
            playedMediumSudokus = pm.getStatistic(Statistics.playedMediumSudokus).toString(),
            playedDifficultSudokus = pm.getStatistic(Statistics.playedDifficultSudokus).toString(),
            playedInfernalSudokus = pm.getStatistic(Statistics.playedInfernalSudokus).toString(),
            score = pm.getStatistic(Statistics.maximumPoints).toString(),
            fastestSolvingTime = timeString
        )
    }

    /**
     * Called when the activity is starting
     * 
     * Comparison with the original implementation:
     * - Before: setContentView(R.layout.statistics) + multiple findViewById + setText calls
     * - Now: setContent { } directly using Compose composable functions
     * 
     * This is the beauty of Compose - more concise and intuitive!
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set content using Compose
        setContent {
            MaterialTheme {
                StatisticsScreen(data = getStatisticsData())
            }
        }
        
        // Note: We still need to set the ActionBar as it's an Activity-level feature
        // For a fully Compose solution, we could use Scaffold and TopAppBar
        // But to maintain compatibility with existing code, we keep the XML toolbar for now
        // TODO: Consider using Compose's Scaffold + TopAppBar to fully replace this in the future
    }
    
    /**
     * {@inheritDoc}
     */
    //@Override
    /*public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}*/
}