package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val locationModeName by viewModel.locationModeName.collectAsStateWithLifecycle()
    val isGpsActive by viewModel.isGpsActive.collectAsStateWithLifecycle()
    val searchRadiusKm by viewModel.searchRadiusKm.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val filterMyCardsOnly by viewModel.filterMyCardsOnly.collectAsStateWithLifecycle()
    val selectedDayFilter by viewModel.selectedDayFilter.collectAsStateWithLifecycle()
    val showAllPromotionsUnfiltered by viewModel.showAllPromotionsUnfiltered.collectAsStateWithLifecycle()
    val selectedBankFilter by viewModel.selectedBankFilter.collectAsStateWithLifecycle()
    val selectedBankIds by viewModel.selectedBankIds.collectAsStateWithLifecycle()
    val selectedFuelType by viewModel.selectedFuelType.collectAsStateWithLifecycle()
    val fuelSortOption by viewModel.fuelSortOption.collectAsStateWithLifecycle()
    val fuelTankLiters by viewModel.fuelTankLiters.collectAsStateWithLifecycle()
    val selectedMapItem by viewModel.selectedMapItem.collectAsStateWithLifecycle()

    val userCards by viewModel.userCards.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val reportedPromoIds by viewModel.reportedPromoIds.collectAsStateWithLifecycle()
    val stations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val promotions by viewModel.filteredPromotions.collectAsStateWithLifecycle()
    val savingsRankings by viewModel.savingsSimulationResults.collectAsStateWithLifecycle()
    val calcAmount by viewModel.calcAmount.collectAsStateWithLifecycle()
    val calcCategory by viewModel.calcCategory.collectAsStateWithLifecycle()
    val isProximityAlertsEnabled by viewModel.isProximityAlertsEnabled.collectAsStateWithLifecycle()
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()
    val internetSearchState by viewModel.internetSearchState.collectAsStateWithLifecycle()
    val isLocationLoading by viewModel.isLocationLoading.collectAsStateWithLifecycle()
    val isGpsPermissionGranted by viewModel.isGpsPermissionGranted.collectAsStateWithLifecycle()

    // Permissions Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineLocationGranted || coarseLocationGranted

        viewModel.updatePermissionStatus(isGranted)

        if (isGranted) {
            viewModel.requestDeviceGpsLocation(
                onSuccess = { geo ->
                    Toast.makeText(context, "Ubicación GPS obtenida: ${geo.name}", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "Aviso GPS: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Modo zona predeterminada activo (GPS no autorizado)", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Alertas de proximidad habilitadas", Toast.LENGTH_SHORT).show()
        }
    }

    // Check permissions and initialize GPS automatically on app startup safely
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasLocation = hasFine || hasCoarse

        viewModel.updatePermissionStatus(hasLocation)

        if (hasLocation) {
            viewModel.requestDeviceGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun requestGpsLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.requestDeviceGpsLocation(
                onSuccess = {
                    Toast.makeText(context, "Ubicación GPS actualizada", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "GPS: $error", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

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
                // Tab 1: Ofertas & Promociones (Principal)
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

                // Tab 2: Mapa
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
                        onRequestGpsLocation = {
                            requestGpsLocation()
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
                        currentLocation = currentLocation,
                        searchRadiusKm = searchRadiusKm,
                        promotions = promotions,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        filterMyCardsOnly = filterMyCardsOnly,
                        showAllPromotionsUnfiltered = showAllPromotionsUnfiltered,
                        selectedBankFilter = selectedBankFilter,
                        selectedDayFilter = selectedDayFilter,
                        favoriteIds = favoriteIds,
                        reportedPromoIds = reportedPromoIds,
                        locationModeName = locationModeName,
                        internetSearchState = internetSearchState,
                        onSetSearchQuery = { viewModel.setSearchQuery(it) },
                        onSetCategory = { viewModel.setCategory(it) },
                        onSetFilterMyCards = { viewModel.setFilterMyCardsOnly(it) },
                        onSetShowAllPromotionsUnfiltered = { viewModel.setShowAllPromotionsUnfiltered(it) },
                        onSetSelectedBankFilter = { viewModel.setSelectedBankFilter(it) },
                        onClearAllFilters = { viewModel.clearAllFilters() },
                        onSetDayFilter = { viewModel.setDayFilter(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        },
                        onSubmitReportPromo = { promo, reason, details ->
                            viewModel.submitPromotionReport(promo, reason, details)
                            Toast.makeText(context, "Reporte enviado para ${promo.storeName}. ¡Gracias!", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerInternetSearch = { viewModel.triggerManualInternetSearch() }
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
                        selectedBankIds = selectedBankIds,
                        internetSearchState = internetSearchState,
                        isProximityAlertsEnabled = isProximityAlertsEnabled,
                        fcmToken = fcmToken,
                        firestoreSyncStatus = firestoreSyncStatus,
                        onSetCalcAmount = { viewModel.setCalcAmount(it) },
                        onSetCalcCategory = { viewModel.setCalcCategory(it) },
                        onAddCard = { bank, type, net, name, last4 ->
                            viewModel.addCard(bank, type, net, name, last4)
                        },
                        onDeleteCard = { viewModel.deleteCard(it) },
                        onToggleBankSelection = { viewModel.toggleBankSelection(it) },
                        onSelectAllBanks = { viewModel.setSelectedBankIds(it) },
                        onToggleFavorite = { id, type, title, sub ->
                            viewModel.toggleFavorite(id, type, title, sub)
                        },
                        onTriggerInternetSearch = { viewModel.triggerManualInternetSearch() },
                        onToggleProximityAlerts = { viewModel.setProximityAlertsEnabled(it) },
                        onTriggerTestProximityAlert = {
                            viewModel.triggerTestProximityAlert()
                            Toast.makeText(context, "Notificación local enviada", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerTestPush = { title, body ->
                            viewModel.triggerTestPushNotification(title, body)
                            Toast.makeText(context, "Notificación push simulada enviada", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerTestMatchingCardPush = {
                            viewModel.testMatchingPushNotification()
                            Toast.makeText(context, "Notificación push enviada para tus tarjetas", Toast.LENGTH_SHORT).show()
                        },
                        onSyncFirestore = {
                            viewModel.syncDataWithFirestore()
                            Toast.makeText(context, "Sincronizando datos con Firestore...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

