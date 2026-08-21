package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.FuelType
import com.example.model.GeoPoint
import com.example.ui.MapItem
import com.example.ui.PromoWithDistance
import com.example.ui.StationWithDistance
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.LocationPickerSheet
import com.example.ui.components.PromoDetailSheet
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    currentLocation: GeoPoint,
    locationModeName: String,
    isGpsActive: Boolean,
    searchRadiusKm: Double,
    searchQuery: String,
    selectedCategory: Category?,
    filterMyCardsOnly: Boolean,
    selectedFuelType: FuelType,
    stations: List<StationWithDistance>,
    promotions: List<PromoWithDistance>,
    selectedMapItem: MapItem?,
    favoriteIds: List<String>,
    cityZones: List<com.example.model.CityZone>,
    onSelectLocation: (GeoPoint, String, Boolean) -> Unit,
    onSetRadius: (Double) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onSetCategory: (Category?) -> Unit,
    onSetFilterMyCards: (Boolean) -> Unit,
    onSetFuelType: (FuelType) -> Unit,
    onSelectMapItem: (MapItem?) -> Unit,
    onToggleFavorite: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedPromoForDetail by remember { mutableStateOf<com.example.model.Promotion?>(null) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Custom Canvas Map
        InteractiveMapCanvas(
            centerLocation = currentLocation,
            searchRadiusKm = searchRadiusKm,
            stations = stations,
            promotions = promotions,
            selectedFuelType = selectedFuelType,
            selectedMapItem = selectedMapItem,
            onSelectItem = onSelectMapItem,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Floating Top Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            // Location Picker Pill + Search Toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLocationPicker = true }
                    .testTag("location_selector_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGpsActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGpsActive) Icons.Default.MyLocation else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isGpsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGpsActive) "Ubicación GPS Actual" else locationModeName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Radio radar: ${searchRadiusKm.toInt()} km • ${stations.size} estaciones • ${promotions.size} promos",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cambiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Radius Selection Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val radiusOptions = listOf(1.0, 3.0, 5.0, 10.0, 25.0)
                items(radiusOptions) { km ->
                    val isSelected = searchRadiusKm == km
                    Surface(
                        onClick = { onSetRadius(km) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("radius_chip_${km.toInt()}km")
                    ) {
                        Text(
                            text = "${km.toInt()} km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Map Filter Chips (Cards filter, Gas vs Categories)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    // Filter by user cards
                    Surface(
                        onClick = { onSetFilterMyCards(!filterMyCardsOnly) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (filterMyCardsOnly) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("filter_my_cards_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (filterMyCardsOnly) Color.White else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mis Tarjetas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (filterMyCardsOnly) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {
                    val isAllSelected = selectedCategory == null
                    Surface(
                        onClick = { onSetCategory(null) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("filter_all_categories")
                    ) {
                        Text(
                            text = "Todo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                items(Category.entries) { cat ->
                    val isCatSelected = selectedCategory == cat
                    Surface(
                        onClick = { onSetCategory(if (isCatSelected) null else cat) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCatSelected) Color(cat.colorHex) else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        modifier = Modifier.testTag("filter_cat_${cat.id}")
                    ) {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. Floating GPS Reset Button
        FloatingActionButton(
            onClick = {
                val palermo = cityZones.firstOrNull { it.id == "palermo" }?.center ?: currentLocation
                onSelectLocation(palermo, "GPS Simulado - Palermo", true)
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(48.dp)
                .testTag("recenter_gps_fab")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Centrar en mi ubicación",
                modifier = Modifier.size(24.dp)
            )
        }

        // 4. Bottom Selected Item Preview Card
        AnimatedVisibility(
            visible = selectedMapItem != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            if (selectedMapItem != null) {
                when (val item = selectedMapItem) {
                    is MapItem.StationItem -> {
                        val isFav = favoriteIds.contains(item.station.id)
                        ElevatedCard(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.elevatedCardElevation(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("station_map_preview_card")
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Top drag handle indicator
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                        .align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Header: Brand Name + Address + Big Discount Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.station.brand.displayName.uppercase(),
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-0.5).sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${item.station.name} • ${item.station.address}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            text = "A ${String.format(Locale.US, "%.1f", item.distanceKm)} KM DE TU UBICACIÓN",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    onToggleFavorite(
                                                        item.station.id,
                                                        "STATION",
                                                        item.station.name,
                                                        "${item.station.brand.displayName} • ${item.station.address}"
                                                    )
                                                },
                                                modifier = Modifier.size(32.dp).testTag("station_fav_icon")
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                    contentDescription = "Favorito",
                                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onSelectMapItem(null) },
                                                modifier = Modifier.size(32.dp).testTag("close_map_preview")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cerrar",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (item.cardDiscountPercent > 0) {
                                            Text(
                                                text = "-${item.cardDiscountPercent.toInt()}%",
                                                style = MaterialTheme.typography.displaySmall,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFB3261E),
                                                lineHeight = 32.sp
                                            )
                                            Text(
                                                text = "EXCLUSIVO TARJETA",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.2.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Grid of Fuel Prices with Bold Metric Cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Box 1: Selected Fuel Price
                                    val finalPrice = item.priceForSelectedFuel * (1.0 - item.cardDiscountPercent / 100.0)
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = item.fuelType.shortName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = currencyFormat.format(finalPrice),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.cardDiscountPercent > 0) {
                                                Text(
                                                    text = "Antes ${currencyFormat.format(item.priceForSelectedFuel)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Box 2: Premium or Alternative Fuel
                                    val altFuelType = if (item.fuelType == FuelType.NAFTA_SUPER) FuelType.NAFTA_PREMIUM else FuelType.NAFTA_SUPER
                                    val altPrice = item.station.prices[altFuelType] ?: (item.priceForSelectedFuel * 1.2)
                                    val altDiscounted = altPrice * (1.0 - item.cardDiscountPercent / 100.0)

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = altFuelType.shortName.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = currencyFormat.format(altDiscounted),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (item.cardDiscountPercent > 0) {
                                                Text(
                                                    text = "Antes ${currencyFormat.format(altPrice)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!item.cardDiscountPromo.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "💡 ${item.cardDiscountPromo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Bold Action Button
                                Button(
                                    onClick = { onSelectMapItem(null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("station_action_button"),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = "VER PROMOCIONES APLICABLES",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }

                    is MapItem.PromoItem -> {
                        val isFav = favoriteIds.contains(item.promo.id)
                        ElevatedCard(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.elevatedCardElevation(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPromoForDetail = item.promo }
                                .testTag("promo_map_preview_card")
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Subtle Top Handle
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                        .align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(item.promo.category.colorHex).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = item.promo.category.displayName.uppercase(),
                                                color = Color(item.promo.category.colorHex),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.promo.storeName.uppercase(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-0.5).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.promo.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    onToggleFavorite(
                                                        item.promo.id,
                                                        "PROMO",
                                                        item.promo.title,
                                                        "${item.promo.storeName} • ${item.promo.bank.displayName}"
                                                    )
                                                },
                                                modifier = Modifier.size(32.dp).testTag("promo_fav_icon")
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                    contentDescription = "Favorito",
                                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onSelectMapItem(null) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cerrar",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = "-${item.promo.discountPercent.toInt()}%",
                                            style = MaterialTheme.typography.displaySmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB3261E)
                                        )
                                        Text(
                                            text = item.promo.bank.displayName.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { selectedPromoForDetail = item.promo },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("promo_detail_action_button"),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(
                                        text = "VER DETALLES Y CONDICIONES",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }

                    null -> {}
                }
            }
        }
    }

    // Location Picker Bottom Sheet
    if (showLocationPicker) {
        LocationPickerSheet(
            cityZones = cityZones,
            currentLocationName = locationModeName,
            isGpsActive = isGpsActive,
            onSelectZone = { zone ->
                onSelectLocation(zone.center, "${zone.name}, ${zone.province}", false)
            },
            onSelectGps = {
                val palermo = cityZones.firstOrNull { it.id == "palermo" }?.center ?: currentLocation
                onSelectLocation(palermo, "Ubicación GPS Actual", true)
            },
            onDismiss = { showLocationPicker = false }
        )
    }

    // Promo Detail Modal
    if (selectedPromoForDetail != null) {
        val promo = selectedPromoForDetail!!
        val isFav = favoriteIds.contains(promo.id)
        PromoDetailSheet(
            promo = promo,
            matchesUserCards = promotions.firstOrNull { it.promo.id == promo.id }?.matchesUserCards ?: false,
            isFavorite = isFav,
            onToggleFavorite = {
                onToggleFavorite(
                    promo.id,
                    "PROMO",
                    promo.title,
                    "${promo.storeName} • ${promo.bank.displayName}"
                )
            },
            onDismiss = { selectedPromoForDetail = null }
        )
    }
}
