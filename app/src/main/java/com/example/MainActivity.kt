package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.screens.FuelPricesScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.PromotionsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PromoCombustibleApp()
            }
        }
    }
}

@Composable
fun PromoCombustibleApp(
    viewModel: MainViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val locationModeName by viewModel.locationModeName.collectAsStateWithLifecycle()
    val isGpsActive by viewModel.isGpsActive.collectAsStateWithLifecycle()
    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filterMyCardsOnly by viewModel.filterMyCardsOnly.collectAsStateWithLifecycle()
    val selectedDayFilter by viewModel.selectedDayFilter.collectAsStateWithLifecycle()
    val selectedFuelType by viewModel.selectedFuelType.collectAsStateWithLifecycle()
    val fuelSortOption by viewModel.fuelSortOption.collectAsStateWithLifecycle()
    val fuelTankLiters by viewModel.fuelTankLiters.collectAsStateWithLifecycle()
    val selectedMapItem by viewModel.selectedMapItem.collectAsStateWithLifecycle()

    val userCards by viewModel.userCards.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val stations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val promotions by viewModel.filteredPromotions.collectAsStateWithLifecycle()
    val savingsRankings by viewModel.savingsSimulationResults.collectAsStateWithLifecycle()
    val calcAmount by viewModel.calcAmount.collectAsStateWithLifecycle()
    val calcCategory by viewModel.calcCategory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                // Tab 1: Mapa
                NavigationBarItem(
                    selected = selectedTab == AppTab.MAP,
                    onClick = { viewModel.setTab(AppTab.MAP) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Mapa"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.MAP.title,
                            fontWeight = if (selectedTab == AppTab.MAP) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_map")
                )

                // Tab 2: Ofertas & Promociones
                NavigationBarItem(
                    selected = selectedTab == AppTab.PROMOTIONS,
                    onClick = { viewModel.setTab(AppTab.PROMOTIONS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.PROMOTIONS) Icons.Filled.Percent else Icons.Outlined.Percent,
                            contentDescription = "Ofertas"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.PROMOTIONS.title,
                            fontWeight = if (selectedTab == AppTab.PROMOTIONS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_promotions")
                )

                // Tab 3: Combustibles
                NavigationBarItem(
                    selected = selectedTab == AppTab.FUEL,
                    onClick = { viewModel.setTab(AppTab.FUEL) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.FUEL) Icons.Filled.LocalGasStation else Icons.Outlined.LocalGasStation,
                            contentDescription = "Combustibles"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.FUEL.title,
                            fontWeight = if (selectedTab == AppTab.FUEL) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_fuel")
                )

                // Tab 4: Billetera & Calculadora
                NavigationBarItem(
                    selected = selectedTab == AppTab.WALLET,
                    onClick = { viewModel.setTab(AppTab.WALLET) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.WALLET) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Billetera"
                        )
                    },
                    label = {
                        Text(
                            text = AppTab.WALLET.title,
                            fontWeight = if (selectedTab == AppTab.WALLET) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_wallet")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.MAP -> {
                    MapScreen(
                        currentLocation = currentLocation,
                        locationModeName = locationModeName,
                        isGpsActive = isGpsActive,
                        searchRadiusKm = searchRadiusKm,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        filterMyCardsOnly = filterMyCardsOnly,
                        selectedFuelType = selectedFuelType,
                        stations = stations,
                        promotions = promotions,
                        selectedMapItem = selectedMapItem,
                        favoriteIds = favoriteIds,
                        cityZones = viewModel.cityZones,
                        onSelectLocation = { point, name, isGps ->
                            viewModel.setLocation(point, name, isGps)
                        },
                        onSetRadius = { viewModel.setRadius(it) },
                        onSetSearchQuery = { viewModel.setSearchQuery(it) },
                        onSetCategory = { viewModel.setCategory(it) },
                        onSetFilterMyCards = { viewModel.setFilterMyCardsOnly(it) },
                        onSetFuelType = { viewModel.setFuelType(it) },
                        onSelectMapItem = { viewModel.setSelectedMapItem(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        }
                    )
                }

                AppTab.PROMOTIONS -> {
                    PromotionsScreen(
                        promotions = promotions,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        filterMyCardsOnly = filterMyCardsOnly,
                        selectedDayFilter = selectedDayFilter,
                        favoriteIds = favoriteIds,
                        locationModeName = locationModeName,
                        onSetSearchQuery = { viewModel.setSearchQuery(it) },
                        onSetCategory = { viewModel.setCategory(it) },
                        onSetFilterMyCards = { viewModel.setFilterMyCardsOnly(it) },
                        onSetDayFilter = { viewModel.setDayFilter(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        }
                    )
                }

                AppTab.FUEL -> {
                    FuelPricesScreen(
                        stations = stations,
                        selectedFuelType = selectedFuelType,
                        fuelSortOption = fuelSortOption,
                        fuelTankLiters = fuelTankLiters,
                        favoriteIds = favoriteIds,
                        locationModeName = locationModeName,
                        onSetFuelType = { viewModel.setFuelType(it) },
                        onSetSortOption = { viewModel.setFuelSortOption(it) },
                        onSetTankLiters = { viewModel.setFuelTankLiters(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        }
                    )
                }

                AppTab.WALLET -> {
                    WalletScreen(
                        userCards = userCards,
                        calcAmount = calcAmount,
                        calcCategory = calcCategory,
                        savingsRankings = savingsRankings,
                        favoriteIds = favoriteIds,
                        onSetCalcAmount = { viewModel.setCalcAmount(it) },
                        onSetCalcCategory = { viewModel.setCalcCategory(it) },
                        onAddCard = { bank, type, net, name, last4 ->
                            viewModel.addCard(bank, type, net, name, last4)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        }
                    )
                }
            }
        }
    }
}

