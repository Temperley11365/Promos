package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "promocombustible_user_preferences")

data class UserPreferences(
    val selectedBankIds: Set<String> = emptySet(),
    val filterMyCardsOnly: Boolean = false,
    val showAllPromotionsUnfiltered: Boolean = false,
    val selectedBankFilter: String = "ALL",
    val selectedCategoryId: String? = null,
    val selectedDayFilter: Int = 0,
    val searchRadiusKm: Double = 5.0,
    val isProximityAlertsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_BANK_IDS = stringSetPreferencesKey("selected_bank_ids")
        val FILTER_MY_CARDS_ONLY = booleanPreferencesKey("filter_my_cards_only")
        val SHOW_ALL_PROMOTIONS_UNFILTERED = booleanPreferencesKey("show_all_promos_unfiltered")
        val SELECTED_BANK_FILTER = stringPreferencesKey("selected_bank_filter")
        val SELECTED_CATEGORY_ID = stringPreferencesKey("selected_category_id")
        val SELECTED_DAY_FILTER = intPreferencesKey("selected_day_filter")
        val SEARCH_RADIUS_KM = doublePreferencesKey("search_radius_km")
        val PROXIMITY_ALERTS_ENABLED = booleanPreferencesKey("proximity_alerts_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val bankIds = preferences[PreferencesKeys.SELECTED_BANK_IDS] ?: emptySet()
            val filterMyCards = preferences[PreferencesKeys.FILTER_MY_CARDS_ONLY] ?: false
            val showAllUnfiltered = preferences[PreferencesKeys.SHOW_ALL_PROMOTIONS_UNFILTERED] ?: false
            val bankFilter = preferences[PreferencesKeys.SELECTED_BANK_FILTER] ?: "ALL"
            val categoryId = preferences[PreferencesKeys.SELECTED_CATEGORY_ID]
            val dayFilter = preferences[PreferencesKeys.SELECTED_DAY_FILTER] ?: 0
            val radius = preferences[PreferencesKeys.SEARCH_RADIUS_KM] ?: 5.0
            val proximityEnabled = preferences[PreferencesKeys.PROXIMITY_ALERTS_ENABLED] ?: true

            UserPreferences(
                selectedBankIds = bankIds,
                filterMyCardsOnly = filterMyCards,
                showAllPromotionsUnfiltered = showAllUnfiltered,
                selectedBankFilter = bankFilter,
                selectedCategoryId = categoryId,
                selectedDayFilter = dayFilter,
                searchRadiusKm = radius,
                isProximityAlertsEnabled = proximityEnabled
            )
        }

    suspend fun setSelectedBankIds(bankIds: Set<String>) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_BANK_IDS] = bankIds
        }
    }

    suspend fun toggleBankSelection(bankId: String) {
        context.userDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SELECTED_BANK_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(bankId)) {
                current.remove(bankId)
            } else {
                current.add(bankId)
            }
            preferences[PreferencesKeys.SELECTED_BANK_IDS] = current
        }
    }

    suspend fun setFilterMyCardsOnly(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.FILTER_MY_CARDS_ONLY] = enabled
            if (enabled) {
                preferences[PreferencesKeys.SHOW_ALL_PROMOTIONS_UNFILTERED] = false
            }
        }
    }

    suspend fun setShowAllPromotionsUnfiltered(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ALL_PROMOTIONS_UNFILTERED] = enabled
            if (enabled) {
                preferences[PreferencesKeys.FILTER_MY_CARDS_ONLY] = false
                preferences[PreferencesKeys.SELECTED_BANK_FILTER] = "ALL"
            }
        }
    }

    suspend fun setSelectedBankFilter(bankId: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_BANK_FILTER] = bankId
            if (bankId != "ALL") {
                preferences[PreferencesKeys.SHOW_ALL_PROMOTIONS_UNFILTERED] = false
            }
        }
    }

    suspend fun setSelectedCategoryId(categoryId: String?) {
        context.userDataStore.edit { preferences ->
            if (categoryId == null) {
                preferences.remove(PreferencesKeys.SELECTED_CATEGORY_ID)
            } else {
                preferences[PreferencesKeys.SELECTED_CATEGORY_ID] = categoryId
            }
        }
    }

    suspend fun setSelectedDayFilter(day: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_DAY_FILTER] = day
        }
    }

    suspend fun setSearchRadiusKm(radius: Double) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_RADIUS_KM] = radius
        }
    }

    suspend fun setProximityAlertsEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PROXIMITY_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun clearAllFilters() {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ALL_PROMOTIONS_UNFILTERED] = true
            preferences[PreferencesKeys.FILTER_MY_CARDS_ONLY] = false
            preferences[PreferencesKeys.SELECTED_BANK_FILTER] = "ALL"
            preferences.remove(PreferencesKeys.SELECTED_CATEGORY_ID)
            preferences[PreferencesKeys.SELECTED_DAY_FILTER] = 0
        }
    }
}
