package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.firestore.FirestoreRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserCardEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.location.LocationHelper
import com.example.model.Bank
import com.example.model.CardNetwork
import com.example.model.CardSavingsRank
import com.example.model.CardType
import com.example.model.Category
import com.example.model.CityZone
import com.example.model.FuelType
import com.example.model.GasStation
import com.example.model.GeoPoint
import com.example.model.InternetSearchState
import com.example.model.Promotion
import com.example.notification.NotificationHelper
import com.example.notification.ProximityAlertManager
import com.example.service.AppFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    MAP("Mapa"),
    PROMOTIONS("Ofertas"),
    FUEL("Combustibles"),
    WALLET("Billetera")
}

enum class FuelSortOption(val displayName: String) {
    CHEAPEST("Más Barato"),
    NEAREST("Más Cercano"),
    BEST_DISCOUNT("Mayor Descuento")
}

sealed interface MapItem {
    val id: String
    val title: String
    val address: String
    val location: GeoPoint
    val distanceKm: Double

    data class StationItem(
        val station: GasStation,
        val priceForSelectedFuel: Double,
        val fuelType: FuelType,
        val cardDiscountPercent: Double,
        val cardDiscountPromo: String?,
        override val distanceKm: Double
    ) : MapItem {
        override val id: String get() = station.id
        override val title: String get() = station.name
        override val address: String get() = station.address
        override val location: GeoPoint get() = station.location
    }

    data class PromoItem(
        val promo: Promotion,
        val matchesUserCard: Boolean,
        override val distanceKm: Double
    ) : MapItem {
        override val id: String get() = promo.id
        override val title: String get() = promo.title
        override val address: String get() = promo.address
        override val location: GeoPoint get() = promo.location
    }
}

data class StationWithDistance(
    val station: GasStation,
    val distanceKm: Double,
    val selectedFuelPrice: Double,
    val cardDiscountPercent: Double,
    val cardDiscountPromo: String?,
    val finalPricePerLiter: Double,
    val tankFillCost: Double,
    val tankFillSavings: Double
)

data class PromoWithDistance(
    val promo: Promotion,
    val distanceKm: Double,
    val matchesUserCards: Boolean,
    val isTodayValid: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository: AppRepository = AppRepository(db)
    private val userPreferencesRepo: UserPreferencesRepository = UserPreferencesRepository(application)
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(application)
    private val locationHelper: LocationHelper = LocationHelper(application)
    private val proximityAlertManager: ProximityAlertManager = ProximityAlertManager(application)

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _firestoreStations = MutableStateFlow<List<GasStation>>(emptyList())
    private val _firestorePromotions = MutableStateFlow<List<Promotion>>(emptyList())

    private val _isProximityAlertsEnabled = MutableStateFlow(true)
    val isProximityAlertsEnabled = _isProximityAlertsEnabled.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken = _fcmToken.asStateFlow()

    private val _firestoreSyncStatus = MutableStateFlow("Modo local y nube activo")
    val firestoreSyncStatus = _firestoreSyncStatus.asStateFlow()

    private val _locationErrorMessage = MutableStateFlow<String?>(null)
    val locationErrorMessage = _locationErrorMessage.asStateFlow()

    private val _isLocationLoading = MutableStateFlow(false)
    val isLocationLoading = _isLocationLoading.asStateFlow()

    private val _isGpsPermissionGranted = MutableStateFlow(LocationHelper.hasLocationPermission(application))
    val isGpsPermissionGranted = _isGpsPermissionGranted.asStateFlow()

    fun updatePermissionStatus(isGranted: Boolean) {
        _isGpsPermissionGranted.value = isGranted
    }

    // User DataStore Preferences Flow
    val userPreferences = userPreferencesRepo.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    // Internet live search state
    private val _internetSearchState = MutableStateFlow(InternetSearchState())
    val internetSearchState = _internetSearchState.asStateFlow()

    val userCards: StateFlow<List<UserCardEntity>> = repository.userCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<String>> = repository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportedPromoIds: StateFlow<List<String>> = repository.reportedPromoIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cityZones = repository.cityZones

    // Active gas stations list (combines Firestore live items or default repo fallback)
    private val activeGasStations: StateFlow<List<GasStation>> = _firestoreStations
        .combine(MutableStateFlow(repository.gasStations)) { firestoreList, defaultList ->
            if (firestoreList.isNotEmpty()) firestoreList else defaultList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.gasStations)

    // Active promotions list (combines Firestore live items, dynamic online discovered promos, and default repo)
    private val activePromotions: StateFlow<List<Promotion>> = combine(
        _firestorePromotions,
        repository.dynamicOnlinePromotions,
        MutableStateFlow(repository.promotions)
    ) { firestoreList, dynamicList, defaultList ->
        val merged = mutableListOf<Promotion>()
        merged.addAll(dynamicList)
        if (firestoreList.isNotEmpty()) {
            merged.addAll(firestoreList)
        } else {
            merged.addAll(defaultList)
        }
        merged.distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.getAllMergedPromotions())

    // UI States
    private val _selectedTab = MutableStateFlow(AppTab.PROMOTIONS)
    val selectedTab = _selectedTab.asStateFlow()

    private val _currentLocation = MutableStateFlow(repository.cityZones[0].center) // Default Palermo Soho
    val currentLocation = _currentLocation.asStateFlow()

    private val _locationModeName = MutableStateFlow("Palermo Soho, CABA")
    val locationModeName = _locationModeName.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive = _isGpsActive.asStateFlow()

    private val _searchRadiusKm = MutableStateFlow(5.0)
    val searchRadiusKm = _searchRadiusKm.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _filterMyCardsOnly = MutableStateFlow(false)
    val filterMyCardsOnly = _filterMyCardsOnly.asStateFlow()

    private val _selectedDayFilter = MutableStateFlow<Int?>(null)
    val selectedDayFilter = _selectedDayFilter.asStateFlow()

    private val _showAllPromotionsUnfiltered = MutableStateFlow(false)
    val showAllPromotionsUnfiltered = _showAllPromotionsUnfiltered.asStateFlow()

    private val _selectedBankFilter = MutableStateFlow<Bank?>(null)
    val selectedBankFilter = _selectedBankFilter.asStateFlow()

    private val _selectedBankIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBankIds = _selectedBankIds.asStateFlow()

    private val _selectedFuelType = MutableStateFlow<FuelType?>(null)
    val selectedFuelType = _selectedFuelType.asStateFlow()

    private val _fuelSortOption = MutableStateFlow(FuelSortOption.CHEAPEST)
    val fuelSortOption = _fuelSortOption.asStateFlow()

    private val _fuelTankLiters = MutableStateFlow(50.0)
    val fuelTankLiters = _fuelTankLiters.asStateFlow()

    private val _selectedMapItem = MutableStateFlow<MapItem?>(null)
    val selectedMapItem = _selectedMapItem.asStateFlow()

    private val _calcAmount = MutableStateFlow(1000.0)
    val calcAmount = _calcAmount.asStateFlow()

    private val _calcCategory = MutableStateFlow(Category.SUPERMARKET)
    val calcCategory = _calcCategory.asStateFlow()

    // Filtered & computed states
    val filteredStations: StateFlow<List<StationWithDistance>> = combine(
        activeGasStations,
        currentLocation,
        searchRadiusKm,
        selectedFuelType,
        userCards,
        selectedTab
    ) { stations, location, radius, fuelType, cards, _ ->
        stations.filter { station ->
            val dist = distanceBetween(location.lat, location.lng, station.location.lat, station.location.lng)
            dist <= radius
        }.map { station ->
            val dist = distanceBetween(location.lat, location.lng, station.location.lat, station.location.lng)
            val selectedFuel = fuelType ?: FuelType.NAFTA_SUPER
            val price = station.prices[selectedFuel] ?: 0.0
            val (discount, promoText) = repository.getBestPromoForGasStation(station, cards)
            StationWithDistance(
                station = station,
                distanceKm = dist,
                selectedFuelPrice = price,
                cardDiscountPercent = discount,
                cardDiscountPromo = promoText,
                finalPricePerLiter = price * (1 - discount / 100),
                tankFillCost = price * fuelTankLiters.value,
                tankFillSavings = price * fuelTankLiters.value * (discount / 100)
            )
        }.sortedBy { it.distanceKm }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPromotions: StateFlow<List<PromoWithDistance>> = combine(
        activePromotions,
        currentLocation,
        searchRadiusKm,
        searchQuery,
        selectedCategory,
        filterMyCardsOnly,
        selectedDayFilter,
        showAllPromotionsUnfiltered,
        selectedBankFilter,
        userCards
    ) { promos, location, radius, query, category, filterMyCards, dayFilter, showAll, bankFilter, cards ->
        var filtered = promos
        if (!showAll) {
            filtered = filtered.filter { promo ->
                val dist = distanceBetween(location.lat, location.lng, promo.location.lat, promo.location.lng)
                dist <= radius
            }
            if (category != null) {
                filtered = filtered.filter { it.category == category }
            }
            if (query.isNotEmpty()) {
                filtered = filtered.filter {
                    it.storeName.contains(query, ignoreCase = true) ||
                            it.title.contains(query, ignoreCase = true)
                }
            }
            if (filterMyCards) {
                filtered = filtered.filter { promo ->
                    repository.doesPromoMatchUserCards(promo, cards)
                }
            }
            if (dayFilter != null) {
                filtered = filtered.filter { it.daysValid.contains(dayFilter) }
            }
            if (bankFilter != null) {
                filtered = filtered.filter { it.bank == bankFilter || it.bank == Bank.TODOS }
            }
        }
        filtered.map { promo ->
            val dist = distanceBetween(location.lat, location.lng, promo.location.lat, promo.location.lng)
            val matches = repository.doesPromoMatchUserCards(promo, cards)
            val isValid = promo.isValidToday()
            PromoWithDistance(promo, dist, matches, isValid)
        }.sortedBy { it.distanceKm }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsSimulationResults: StateFlow<List<CardSavingsRank>> = combine(
        userCards,
        calcAmount,
        calcCategory
    ) { cards, amount, cat ->
        repository.simulatePurchaseSavings(amount, cat, cards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setLocation(point: GeoPoint, name: String, isGps: Boolean) {
        _currentLocation.value = point
        _locationModeName.value = name
        _isGpsActive.value = isGps
    }

    fun setRadius(km: Double) {
        _searchRadiusKm.value = km
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(cat: Category?) {
        _selectedCategory.value = cat
    }

    fun setFilterMyCardsOnly(filter: Boolean) {
        _filterMyCardsOnly.value = filter
    }

    fun setDayFilter(day: Int?) {
        _selectedDayFilter.value = day
    }

    fun setShowAllPromotionsUnfiltered(show: Boolean) {
        _showAllPromotionsUnfiltered.value = show
    }

    fun setSelectedBankFilter(bank: Bank?) {
        _selectedBankFilter.value = bank
    }

    fun setSelectedBankIds(ids: Set<String>) {
        _selectedBankIds.value = ids
    }

    fun toggleBankSelection(bankId: String) {
        _selectedBankIds.update { currentSet ->
            if (currentSet.contains(bankId)) {
                currentSet - bankId
            } else {
                currentSet + bankId
            }
        }
    }

    fun setFuelType(fuelType: FuelType?) {
        _selectedFuelType.value = fuelType
    }

    fun setFuelSortOption(option: FuelSortOption) {
        _fuelSortOption.value = option
    }

    fun setFuelTankLiters(liters: Double) {
        _fuelTankLiters.value = liters
    }

    fun setSelectedMapItem(item: MapItem?) {
        _selectedMapItem.value = item
    }

    fun setCalcAmount(amount: Double) {
        _calcAmount.value = amount
    }

    fun setCalcCategory(cat: Category) {
        _calcCategory.value = cat
    }

    fun requestDeviceGpsLocation(onSuccess: ((GeoPoint) -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        _isLocationLoading.value = true
        locationHelper.requestLocationUpdates(
            onLocationUpdate = { latitude, longitude, locationName ->
                _currentLocation.value = GeoPoint(latitude, longitude, locationName)
                _locationModeName.value = locationName
                _isGpsActive.value = true
                _isLocationLoading.value = false
                onSuccess?.invoke(GeoPoint(latitude, longitude, locationName))
            },
            onError = { error ->
                _isLocationLoading.value = false
                _locationErrorMessage.value = error
                onError?.invoke(error)
            }
        )
    }

    fun addCard(bank: Bank, cardType: CardType, network: CardNetwork, name: String, last4: String) {
        viewModelScope.launch {
            val entity = UserCardEntity(
                cardName = name,
                bankId = bank.id,
                bankName = bank.displayName,
                cardType = cardType.name,
                cardNetwork = network.name,
                last4Digits = last4
            )
            repository.saveCard(entity)
            triggerManualInternetSearch()
        }
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _filterMyCardsOnly.value = false
        _selectedDayFilter.value = null
        _selectedBankFilter.value = null
    }

    fun searchInternetForCardsAndBanks(
        cards: List<UserCardEntity>,
        selectedBankIds: Set<String>,
        isContinuousMonitoring: Boolean = false
    ) {
        viewModelScope.launch {
            if (cards.isEmpty() && selectedBankIds.isEmpty()) {
                _internetSearchState.update {
                    it.copy(
                        isSearching = false,
                        lastSearchTime = Calendar.getInstance().time,
                        statusMessage = "No hay tarjetas registradas para buscar"
                    )
                }
                return@launch
            }

            _internetSearchState.update { it.copy(isSearching = true, statusMessage = "🔍 Buscando promociones...") }

            try {
                val foundPromos = mutableListOf<Promotion>()

                // Search by registered cards
                if (cards.isNotEmpty()) {
                    val searchResults = repository.searchOnlineForCards(cards, currentLocation.value)
                    foundPromos.addAll(searchResults)
                }

                // Search by selected banks (even without cards)
                if (selectedBankIds.isNotEmpty()) {
                    val bankResults = repository.searchOnlineForCardsAndBanks(
                        cards.filter { selectedBankIds.contains(it.bankId) },
                        selectedBankIds,
                        currentLocation.value
                    )
                    foundPromos.addAll(bankResults)
                }

                _internetSearchState.update {
                    it.copy(
                        isSearching = false,
                        lastSearchTime = Calendar.getInstance().time,
                        foundCount = foundPromos.size,
                        statusMessage = if (isContinuousMonitoring) "🌐 Monitoreo continuo de promociones activo" else "✓ Búsqueda completada"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching internet for promos: ${e.message}")
                _internetSearchState.update {
                    it.copy(
                        isSearching = false,
                        statusMessage = "⚠️ Error en búsqueda: ${e.message}"
                    )
                }
            }
        }
    }

    fun searchInternetForCards(cards: List<UserCardEntity> = userCards.value, isContinuousMonitoring: Boolean = false) {
        searchInternetForCardsAndBanks(cards, _selectedBankIds.value, isContinuousMonitoring)
    }

    fun triggerManualInternetSearch() {
        searchInternetForCardsAndBanks(userCards.value, _selectedBankIds.value, isContinuousMonitoring = false)
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun toggleFavorite(itemId: String, itemType: String, title: String, subtitle: String) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(itemId)
            repository.toggleFavorite(itemId, itemType, title, subtitle, isFav)
        }
    }

    fun submitPromotionReport(promo: Promotion, reason: String, details: String) {
        viewModelScope.launch {
            try {
                repository.submitPromotionReport(
                    promoId = promo.id,
                    promoTitle = promo.title,
                    storeName = promo.storeName,
                    bankName = promo.bank.displayName,
                    reason = reason,
                    details = details
                )
                firestoreRepository.submitPromotionReport(
                    promoId = promo.id,
                    promoTitle = promo.title,
                    storeName = promo.storeName,
                    bankName = promo.bank.displayName,
                    reason = reason,
                    details = details
                )
                Log.d(TAG, "Promotion report submitted successfully for ${promo.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting promo report: ${e.message}")
            }
        }
    }

    // Helper function to check if a promo matches user cards
    private fun doesPromoMatchCards(promo: Promotion): Boolean {
        return repository.doesPromoMatchUserCards(promo, userCards.value)
    }

    fun testMatchingPushNotification(targetPromo: Promotion? = null) {
        val promo = targetPromo ?: activePromotions.value.firstOrNull { doesPromoMatchCards(it) }
            ?: activePromotions.value.firstOrNull()
            ?: return

        val cards = userCards.value
        val matchingCard = cards.firstOrNull { card ->
            val bankMatches = card.bankId.equals(promo.bank.id, ignoreCase = true)
            val netMatches = promo.cardNetwork == null || card.cardNetwork.equals(promo.cardNetwork.name, ignoreCase = true)
            val typeMatches = promo.cardType == CardType.ANY || card.cardType.equals(promo.cardType.name, ignoreCase = true)
            bankMatches && (netMatches || typeMatches)
        }

        if (matchingCard != null) {
            NotificationHelper.showCardMatchedPromoNotification(
                context = getApplication(),
                cardName = matchingCard.cardName,
                bankName = matchingCard.bankName,
                promoTitle = promo.title,
                discountPercent = promo.discountPercent,
                storeName = promo.storeName,
                promoId = promo.id
            )
        } else {
            NotificationHelper.showPushNotification(
                context = getApplication(),
                title = "🔥 ¡Nueva promoción en ${promo.storeName}!",
                body = "${promo.discountPercent.toInt()}% OFF con ${promo.bank.displayName} (${promo.category.displayName}). ¡Aprovechala!",
                data = mapOf(
                    "promo_id" to promo.id,
                    "bank_id" to promo.bank.id,
                    "store_name" to promo.storeName,
                    "title" to promo.title,
                    "discount_percent" to promo.discountPercent.toString()
                )
            )
        }
    }

    fun triggerTestProximityAlert() {
        val nearestStation = filteredStations.value.firstOrNull() ?: return
        NotificationHelper.showProximityAlert(
            context = getApplication(),
            stationName = nearestStation.station.name,
            promoText = nearestStation.cardDiscountPromo ?: "Descuento disponible",
            distanceKm = nearestStation.distanceKm,
            discountPercent = nearestStation.cardDiscountPercent
        )
    }

    fun syncDataWithFirestore() {
        viewModelScope.launch {
            try {
                _firestoreSyncStatus.value = "📤 Sincronizando..."
                firestoreRepository.uploadUserCards(userCards.value)
                firestoreRepository.uploadFavorites(favoriteIds.value)
                _firestoreSyncStatus.value = "✓ Sincronización completada"
            } catch (e: Exception) {
                _firestoreSyncStatus.value = "⚠️ Error en sincronización"
                Log.e(TAG, "Sync error: ${e.message}")
            }
        }
    }

    fun setProximityAlertsEnabled(enabled: Boolean) {
        _isProximityAlertsEnabled.value = enabled
        if (enabled) {
            proximityAlertManager.startMonitoring(filteredStations.value.map { it.station })
        } else {
            proximityAlertManager.stopMonitoring()
        }
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
    }
}
