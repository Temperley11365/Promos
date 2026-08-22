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
import com.example.model.Promotion
import com.example.notification.NotificationHelper
import com.example.notification.ProximityAlertManager
import com.example.service.AppFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private val repository: AppRepository
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db)
        NotificationHelper.createNotificationChannels(application)

        viewModelScope.launch {
            try {
                repository.initDefaultCardsIfEmpty()
            } catch (e: Throwable) {
                Log.w(TAG, "Error initializing default cards: ${e.message}")
            }
        }

        // Initialize and listen to Firestore
        viewModelScope.launch {
            try {
                firestoreRepository.seedInitialDataIfEmpty(repository.gasStations, repository.promotions)
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore initial seed skipped: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                firestoreRepository.getStationsFlow().collect { stationsList ->
                    if (stationsList.isNotEmpty()) {
                        _firestoreStations.value = stationsList
                        _firestoreSyncStatus.value = "Sincronizado: ${stationsList.size} estaciones en Firestore"
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore stations flow error: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                firestoreRepository.getPromotionsFlow().collect { promoList ->
                    if (promoList.isNotEmpty()) {
                        _firestorePromotions.value = promoList
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore promotions flow error: ${e.message}")
            }
        }

        // Fetch FCM Push Registration Token safely
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        _fcmToken.value = token
                        Log.d(TAG, "FCM Token acquired: $token")
                    } else {
                        _fcmToken.value = AppFirebaseMessagingService.lastToken
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "FCM Token processing exception: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseMessaging token retrieval not available: ${e.message}")
        }
    }

    val userCards: StateFlow<List<UserCardEntity>> = repository.userCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<String>> = repository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cityZones = repository.cityZones

    // Active gas stations list (combines Firestore live items or default repo fallback)
    private val activeGasStations: StateFlow<List<GasStation>> = _firestoreStations
        .combine(MutableStateFlow(repository.gasStations)) { firestoreList, defaultList ->
            if (firestoreList.isNotEmpty()) firestoreList else defaultList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.gasStations)

    // Active promotions list (combines Firestore live items or default repo fallback)
    private val activePromotions: StateFlow<List<Promotion>> = _firestorePromotions
        .combine(MutableStateFlow(repository.promotions)) { firestoreList, defaultList ->
            if (firestoreList.isNotEmpty()) firestoreList else defaultList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.promotions)

    // UI States
    private val _selectedTab = MutableStateFlow(AppTab.MAP)
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

    private val _selectedDayFilter = MutableStateFlow(0) // 0 = Todos, 1 = Hoy, 2=Lun, 3=Mar...
    val selectedDayFilter = _selectedDayFilter.asStateFlow()

    private val _selectedFuelType = MutableStateFlow(FuelType.NAFTA_SUPER)
    val selectedFuelType = _selectedFuelType.asStateFlow()

    private val _fuelSortOption = MutableStateFlow(FuelSortOption.CHEAPEST)
    val fuelSortOption = _fuelSortOption.asStateFlow()

    private val _fuelTankLiters = MutableStateFlow(45)
    val fuelTankLiters = _fuelTankLiters.asStateFlow()

    private val _selectedMapItem = MutableStateFlow<MapItem?>(null)
    val selectedMapItem = _selectedMapItem.asStateFlow()

    // Calculator State
    private val _calcAmount = MutableStateFlow("45000")
    val calcAmount = _calcAmount.asStateFlow()

    private val _calcCategory = MutableStateFlow(Category.SUPERMARKET)
    val calcCategory = _calcCategory.asStateFlow()

    data class FuelFilterState(
        val loc: GeoPoint,
        val radius: Double,
        val fuelType: FuelType,
        val sortOpt: FuelSortOption,
        val liters: Int,
        val query: String
    )

    private val fuelFilterState: StateFlow<FuelFilterState> = combine(
        combine(currentLocation, searchRadiusKm, selectedFuelType) { loc, radius, fuelType ->
            Triple(loc, radius, fuelType)
        },
        combine(fuelSortOption, fuelTankLiters, searchQuery) { sortOpt, liters, query ->
            Triple(sortOpt, liters, query)
        }
    ) { (loc, radius, fuelType), (sortOpt, liters, query) ->
        FuelFilterState(loc, radius, fuelType, sortOpt, liters, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelFilterState(repository.cityZones[0].center, 5.0, FuelType.NAFTA_SUPER, FuelSortOption.CHEAPEST, 45, ""))

    // Filtered Gas Stations Flow
    val filteredStations: StateFlow<List<StationWithDistance>> = combine(
        fuelFilterState,
        userCards,
        activeGasStations
    ) { filter, cards, stationsList ->
        val list = stationsList.map { station ->
            val dist = repository.calculateDistanceKm(filter.loc.lat, filter.loc.lng, station.location.lat, station.location.lng)
            val (discountPct, promoDesc) = repository.getBestPromoForGasStation(station, cards)
            val basePrice = station.prices[filter.fuelType] ?: 1100.0
            val effectivePrice = basePrice * (1.0 - discountPct / 100.0)
            val totalTankRaw = basePrice * filter.liters
            val totalTankDiscounted = effectivePrice * filter.liters
            val savings = totalTankRaw - totalTankDiscounted

            StationWithDistance(
                station = station,
                distanceKm = dist,
                selectedFuelPrice = basePrice,
                cardDiscountPercent = discountPct,
                cardDiscountPromo = promoDesc,
                finalPricePerLiter = effectivePrice,
                tankFillCost = totalTankDiscounted,
                tankFillSavings = savings
            )
        }.filter {
            it.distanceKm <= (filter.radius * 1.8) &&
                    (filter.query.isBlank() || it.station.name.contains(filter.query, ignoreCase = true) || it.station.brand.displayName.contains(filter.query, ignoreCase = true) || it.station.address.contains(filter.query, ignoreCase = true))
        }

        when (filter.sortOpt) {
            FuelSortOption.CHEAPEST -> list.sortedBy { it.finalPricePerLiter }
            FuelSortOption.NEAREST -> list.sortedBy { it.distanceKm }
            FuelSortOption.BEST_DISCOUNT -> list.sortedByDescending { it.cardDiscountPercent }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class PromoFilterState(
        val loc: GeoPoint,
        val radius: Double,
        val cat: Category?,
        val myCardsOnly: Boolean,
        val dayFilter: Int,
        val query: String
    )

    private val promoFilterState: StateFlow<PromoFilterState> = combine(
        combine(currentLocation, searchRadiusKm, selectedCategory) { loc, radius, cat ->
            Triple(loc, radius, cat)
        },
        combine(filterMyCardsOnly, selectedDayFilter, searchQuery) { myCardsOnly, dayFilter, query ->
            Triple(myCardsOnly, dayFilter, query)
        }
    ) { (loc, radius, cat), (myCardsOnly, dayFilter, query) ->
        PromoFilterState(loc, radius, cat, myCardsOnly, dayFilter, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PromoFilterState(repository.cityZones[0].center, 5.0, null, false, 0, ""))

    // Filtered Promotions Flow
    val filteredPromotions: StateFlow<List<PromoWithDistance>> = combine(
        promoFilterState,
        userCards,
        activePromotions
    ) { filter, cards, promoList ->
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        promoList.map { promo ->
            val dist = repository.calculateDistanceKm(filter.loc.lat, filter.loc.lng, promo.location.lat, promo.location.lng)
            val matchesCards = repository.doesPromoMatchUserCards(promo, cards)
            val isToday = promo.daysValid.contains(currentDay)
            PromoWithDistance(
                promo = promo,
                distanceKm = dist,
                matchesUserCards = matchesCards,
                isTodayValid = isToday
            )
        }.filter { item ->
            val matchesRadius = item.distanceKm <= (filter.radius * 2.2)
            val matchesCat = filter.cat == null || item.promo.category == filter.cat
            val matchesMyCards = !filter.myCardsOnly || item.matchesUserCards
            val matchesDay = when (filter.dayFilter) {
                0 -> true // Todos los días
                -1 -> item.isTodayValid // Hoy
                else -> item.promo.daysValid.contains(filter.dayFilter) // 1=Domingo, 2=Lunes, 3=Martes, 4=Miércoles, 5=Jueves, 6=Viernes, 7=Sábado
            }
            val matchesQuery = filter.query.isBlank() ||
                    item.promo.title.contains(filter.query, ignoreCase = true) ||
                    item.promo.storeName.contains(filter.query, ignoreCase = true) ||
                    item.promo.bank.displayName.contains(filter.query, ignoreCase = true)

            matchesRadius && matchesCat && matchesMyCards && matchesDay && matchesQuery
        }.sortedWith(
            compareByDescending<PromoWithDistance> { it.matchesUserCards }
                .thenByDescending { it.promo.discountPercent }
                .thenBy { it.distanceKm }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Purchase Savings Simulation Result
    val savingsSimulationResults: StateFlow<List<CardSavingsRank>> = combine(
        calcAmount,
        calcCategory,
        userCards,
        activePromotions
    ) { amtStr, cat, cards, promoList ->
        val amount = amtStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || cards.isEmpty()) {
            emptyList()
        } else {
            repository.simulatePurchaseSavings(amount, cat, cards, promoList)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setLocation(point: GeoPoint, name: String, isGps: Boolean = false) {
        _currentLocation.value = point
        _locationModeName.value = name
        _isGpsActive.value = isGps
        _selectedMapItem.value = null

        // Check proximity notification when location changes
        if (_isProximityAlertsEnabled.value) {
            proximityAlertManager.checkProximity(point, activeGasStations.value, activePromotions.value)
        }
    }

    fun requestDeviceGpsLocation(
        onSuccess: (GeoPoint) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        locationHelper.fetchCurrentLocation(
            onSuccess = { geoPoint ->
                setLocation(geoPoint, "Mi Posición GPS (${geoPoint.name})", isGps = true)
                _locationErrorMessage.value = null
                onSuccess(geoPoint)
            },
            onError = { error ->
                _locationErrorMessage.value = error
                // Fallback to Palermo Soho
                val defaultZone = repository.cityZones[0]
                setLocation(defaultZone.center, "${defaultZone.name} (Modo Simulado)", isGps = false)
                onError(error)
            }
        )
    }

    fun startContinuousGpsUpdates() {
        locationHelper.startRealtimeLocationUpdates { geoPoint ->
            setLocation(geoPoint, "GPS en Vivo: ${geoPoint.name}", isGps = true)
        }
    }

    fun stopContinuousGpsUpdates() {
        locationHelper.stopLocationUpdates()
    }

    fun setProximityAlertsEnabled(enabled: Boolean) {
        _isProximityAlertsEnabled.value = enabled
        proximityAlertManager.isEnabled = enabled
    }

    fun triggerTestProximityAlert(stationName: String = "YPF Full - Palermo Soho", discountPercent: Double = 15.0) {
        proximityAlertManager.sendTestNotification(stationName, discountPercent)
    }

    fun triggerTestPushNotification(title: String, body: String) {
        NotificationHelper.showPushNotification(
            context = getApplication(),
            title = title,
            body = body,
            data = mapOf("action" to "FUEL_OFFER_ALERT")
        )
    }

    fun syncDataWithFirestore() {
        viewModelScope.launch {
            _firestoreSyncStatus.value = "Sincronizando con Firestore..."
            try {
                for (station in repository.gasStations) {
                    firestoreRepository.saveGasStation(station)
                }
                for (promo in repository.promotions) {
                    firestoreRepository.savePromotion(promo)
                }
                _firestoreSyncStatus.value = "¡Sincronización completada con éxito en la nube!"
            } catch (e: Exception) {
                _firestoreSyncStatus.value = "Error al sincronizar: ${e.message}"
            }
        }
    }

    fun setRadius(km: Double) {
        _searchRadiusKm.value = km
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: Category?) {
        _selectedCategory.value = category
    }

    fun setFilterMyCardsOnly(enabled: Boolean) {
        _filterMyCardsOnly.value = enabled
    }

    fun setDayFilter(dayIndex: Int) {
        _selectedDayFilter.value = dayIndex
    }

    fun setFuelType(type: FuelType) {
        _selectedFuelType.value = type
    }

    fun setFuelSortOption(option: FuelSortOption) {
        _fuelSortOption.value = option
    }

    fun setFuelTankLiters(liters: Int) {
        _fuelTankLiters.value = liters
    }

    fun setSelectedMapItem(item: MapItem?) {
        _selectedMapItem.value = item
    }

    fun setCalcAmount(amount: String) {
        _calcAmount.value = amount
    }

    fun setCalcCategory(cat: Category) {
        _calcCategory.value = cat
    }

    fun addCard(bank: Bank, cardType: CardType, cardNetwork: CardNetwork, cardName: String, last4: String) {
        viewModelScope.launch {
            repository.addCard(bank, cardType, cardNetwork, cardName, last4)
        }
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

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
    }
}

