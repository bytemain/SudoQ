/*
 * SudoQ is a Sudoku-App for Adroid Devices with Version 2.2 at least.
 * Copyright (C) 2025  Jiacheng Li
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.sudoq.controller.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.sudoq.R

/**
 * Data class to hold statistics information
 */
data class StatisticsData(
    val playedSudokus: String,
    val playedEasySudokus: String,
    val playedMediumSudokus: String,
    val playedDifficultSudokus: String,
    val playedInfernalSudokus: String,
    val score: String,
    val fastestSolvingTime: String
)

/**
 * Main statistics screen composable
 * Reimplements the original statistics.xml layout using Jetpack Compose
 */
@Composable
fun StatisticsScreen(data: StatisticsData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Counters section
        SectionTitle(text = stringResource(R.string.statistics_counters))
        
        StatisticItem(
            label = stringResource(R.string.statistics_played_sudokus),
            value = data.playedSudokus
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_played_easy_sudokus),
            value = data.playedEasySudokus
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_played_medium_sudokus),
            value = data.playedMediumSudokus
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_played_difficult_sudokus),
            value = data.playedDifficultSudokus
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_played_infernal_sudokus),
            value = data.playedInfernalSudokus
        )
        
        // Records section
        SectionTitle(
            text = stringResource(R.string.statistics_records),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_score),
            value = data.score
        )
        
        StatisticItem(
            label = stringResource(R.string.statistics_fastest_solving_time),
            value = data.fastestSolvingTime
        )
    }
}

/**
 * Section title composable
 * Similar to the large title TextView in XML with textAppearanceMedium style
 */
@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        color = Color.Black
    )
}

/**
 * Single statistic item composable
 * Corresponds to each statistics TextView in the XML layout
 * 
 * Following the tutorial style:
 * - Text style is MaterialTheme.typography.bodyMedium (similar to XML's textAppearanceSmall)
 * - Uses padding modifier to apply left margin (similar to XML's marginLeft)
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 10.dp),
        color = Color.Black
    )
}

/**
 * Preview function - allows real-time preview in Android Studio
 * Similar to PlantNamePreview in the tutorial
 */
@Preview(showBackground = true)
@Composable
private fun StatisticsScreenPreview() {
    MaterialTheme {
        StatisticsScreen(
            data = StatisticsData(
                playedSudokus = "42",
                playedEasySudokus = "10",
                playedMediumSudokus = "15",
                playedDifficultSudokus = "12",
                playedInfernalSudokus = "5",
                score = "12500",
                fastestSolvingTime = "05:23"
            )
        )
    }
}

/**
 * Preview for a single statistic item
 */
@Preview(showBackground = true)
@Composable
private fun StatisticItemPreview() {
    MaterialTheme {
        StatisticItem(
            label = "Played sudokus",
            value = "42"
        )
    }
}
