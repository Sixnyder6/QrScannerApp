package com.example.qrscannerapp.features.inventory.ui.storage.hub

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrscannerapp.QrScannerViewModel
import com.example.qrscannerapp.StardustGlassBg
import com.example.qrscannerapp.StardustPrimary
import com.example.qrscannerapp.StardustTextPrimary
import com.example.qrscannerapp.StardustTextSecondary
import com.example.qrscannerapp.features.inventory.ui.storage.hub.sections.HubFrameSection
import com.example.qrscannerapp.features.inventory.ui.storage.hub.sections.HubIotSection
import com.example.qrscannerapp.features.inventory.ui.storage.hub.sections.HubListsSection
import com.example.qrscannerapp.features.inventory.ui.storage.utils.HubCategory

@Composable
fun StorageHubScreen(
    viewModel: QrScannerViewModel
) {
    val uiState by viewModel.storageState.collectAsState()
    var selectedCategory by remember { mutableStateOf(HubCategory.FRAME) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = StardustPrimary,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedCategory.ordinal < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                        color = StardustPrimary
                    )
                }
            }
        ) {
            HubCategory.entries.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            text = category.title,
                            color = if (selectedCategory == category) StardustTextPrimary else StardustTextSecondary
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedCategory) {
                HubCategory.FRAME -> HubFrameSection(
                    history = uiState.hubHistory,
                    onSaveEntry = { viewModel.saveHubEntry(it) }
                )
                HubCategory.IOT -> HubIotSection(
                    history = uiState.hubHistory,
                    onSaveEntry = { viewModel.saveHubEntry(it) }
                )
                HubCategory.LISTS -> HubListsSection()
            }
        }
    }
}
