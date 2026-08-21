package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.data.local.UserCardEntity
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db)
        viewModelScope.launch {
            repository.initDefaultCardsIfEmpty()
        }
    }

    val userCards: StateFlow<List<UserCardEntity>> = repository.userCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<String>> = repository.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cityZones = repository.cityZones

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
        userCards
    ) { filter, cards ->
        val list = repository.gasStations.map { station ->
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
        userCards
    ) { filter, cards ->
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        repository.promotions.map { promo ->
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
                0 -> true // All days
                1 -> item.isTodayValid // Today
                else -> item.promo.daysValid.contains(filter.dayFilter)
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
        userCards
    ) { amtStr, cat, cards ->
        val amount = amtStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0 || cards.isEmpty()) {
            emptyList()
        } else {
            repository.simulatePurchaseSavings(amount, cat, cards)
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
}
