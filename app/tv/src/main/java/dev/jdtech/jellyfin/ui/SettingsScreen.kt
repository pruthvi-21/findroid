package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.models.Preference
import dev.jdtech.jellyfin.models.PreferenceCategory
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.models.PreferenceSwitch
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.SettingsCategoryCard
import dev.jdtech.jellyfin.ui.components.SettingsSelectCard
import dev.jdtech.jellyfin.ui.components.SettingsSwitchCard
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.SettingsEvent
import dev.jdtech.jellyfin.viewmodels.SettingsViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsScreen(
    navigateToSubSettings: (indexes: IntArray, title: Int) -> Unit,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        settingsViewModel.loadPreferences(intArrayOf())
    }

    ObserveAsEvents(settingsViewModel.eventsChannelFlow) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> navigateToSubSettings(event.indexes, event.title)
            is SettingsEvent.NavigateToUsers -> navigateToUsers()
            is SettingsEvent.NavigateToServers -> navigateToServers()
        }
    }

    val delegatedUiState by settingsViewModel.uiState.collectAsState()

    SettingsScreenLayout(delegatedUiState) { preference ->
        when (preference) {
            is PreferenceSwitch -> {
                settingsViewModel.setBoolean(preference.backendName, preference.value)
            }
            is PreferenceSelect -> {
                settingsViewModel.setString(preference.backendName, preference.value)
            }
        }
        settingsViewModel.loadPreferences(intArrayOf())
    }
}

@Composable
private fun SettingsScreenLayout(
    uiState: SettingsViewModel.UiState,
    onUpdate: (Preference) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SettingsViewModel.UiState.Normal -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
            ) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    Text(
                        text = stringResource(id = CoreR.string.title_settings),
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
                items(uiState.preferences) { preference ->
                    when (preference) {
                        is PreferenceCategory -> SettingsCategoryCard(preference = preference)
                        is PreferenceSwitch -> {
                            SettingsSwitchCard(preference = preference) {
                                onUpdate(preference.copy(value = !preference.value))
                            }
                        }
                        is PreferenceSelect -> {
                            val options = stringArrayResource(id = preference.options)
                            SettingsSelectCard(preference = preference) {
                                val currentIndex = options.indexOf(preference.value)
                                val newIndex = if (currentIndex == options.count() - 1) {
                                    0
                                } else {
                                    currentIndex + 1
                                }
                                onUpdate(preference.copy(value = options[newIndex]))
                            }
                        }
                    }
                }
            }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }
        is SettingsViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        SettingsScreenLayout(
            uiState = SettingsViewModel.UiState.Normal(
                listOf(
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                    ),
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                    ),
                ),
            ),
            onUpdate = {},
        )
    }
}
