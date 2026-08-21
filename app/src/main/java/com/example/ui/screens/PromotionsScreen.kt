package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.FuelType
import com.example.model.GeoPoint
import com.example.model.Promotion
import com.example.ui.MapItem
import com.example.ui.PromoWithDistance
import com.example.ui.components.InteractiveMapCanvas
import com.example.ui.components.PromoDetailSheet
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class PromoDisplayMode {
    LIST,
    MAP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(
    currentLocation: GeoPoint = GeoPoint(-34.5885, -58.4306, "Palermo Soho, CABA"),
    searchRadiusKm: Double = 5.0,
    promotions: List<PromoWithDistance>,
    searchQuery: String,
    selectedCategory: Category?,
    filterMyCardsOnly: Boolean,
    selectedDayFilter: Int,
    favoriteIds: List<String>,
    locationModeName: String,
    onSetSearchQuery: (String) -> Unit,
    onSetCategory: (Category?) -> Unit,
    onSetFilterMyCards: (Boolean) -> Unit,
    onSetDayFilter: (Int) -> Unit,
    onToggleFavorite: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(PromoDisplayMode.LIST) }
    var selectedPromoForDetail by remember { mutableStateOf<Promotion?>(null) }
    var selectedMapItem by remember { mutableStateOf<MapItem?>(null) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    val todayCalendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayName = when (todayCalendarDay) {
        Calendar.MONDAY -> "Lunes"
        Calendar.TUESDAY -> "Martes"
        Calendar.WEDNESDAY -> "Miércoles"
        Calendar.THURSDAY -> "Jueves"
        Calendar.FRIDAY -> "Viernes"
        Calendar.SATURDAY -> "Sábado"
        Calendar.SUNDAY -> "Domingo"
        else -> "Hoy"
    }

    val dayOptions = listOf(
        Pair(0, "Todos"),
        Pair(-1, "⭐ Hoy ($todayName)"),
        Pair(2, "Lunes"),
        Pair(3, "Martes"),
        Pair(4, "Miércoles"),
        Pair(5, "Jueves"),
        Pair(6, "Viernes"),
        Pair(7, "Sábado"),
        Pair(1, "Domingo")
    )

    val currentDayLabel = when (selectedDayFilter) {
        0 -> "Todos los días"
        -1 -> "Hoy ($todayName)"
        2 -> "Lunes"
        3 -> "Martes"
        4 -> "Miércoles"
        5 -> "Jueves"
        6 -> "Viernes"
        7 -> "Sábado"
        1 -> "Domingo"
        else -> "Día seleccionado"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header with Title, Search & Mode Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: Title + "Mis Tarjetas" Filter Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OFERTAS & PROMOS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cerca de $locationModeName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Switch for "Solo mis tarjetas"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Mis Tarjetas",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.3.sp,
                        color = if (filterMyCardsOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Switch(
                        checked = filterMyCardsOnly,
                        onCheckedChange = onSetFilterMyCards,
                        modifier = Modifier.testTag("promos_filter_my_cards_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSetSearchQuery,
                placeholder = { Text("Buscar comercio, banco o marca...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSetSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("promos_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // View Mode Alternating Switch: [ 📋 Listado (N) | 🗺️ Mapa ]
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // List Mode Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewMode = PromoDisplayMode.LIST }
                            .testTag("promo_view_mode_list"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewMode == PromoDisplayMode.LIST) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shadowElevation = if (viewMode == PromoDisplayMode.LIST) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewMode == PromoDisplayMode.LIST) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                                contentDescription = null,
                                tint = if (viewMode == PromoDisplayMode.LIST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LISTA (${promotions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (viewMode == PromoDisplayMode.LIST) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Map Mode Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewMode = PromoDisplayMode.MAP }
                            .testTag("promo_view_mode_map"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (viewMode == PromoDisplayMode.MAP) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shadowElevation = if (viewMode == PromoDisplayMode.MAP) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewMode == PromoDisplayMode.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                                contentDescription = null,
                                tint = if (viewMode == PromoDisplayMode.MAP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MAPA INTERACTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (viewMode == PromoDisplayMode.MAP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isAllSelected = selectedCategory == null
                FilterChip(
                    selected = isAllSelected,
                    onClick = { onSetCategory(null) },
                    label = { Text("Todas", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("promo_cat_all")
                )
            }
            items(Category.entries) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetCategory(if (isSelected) null else cat) },
                    label = { Text(cat.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(cat.colorHex),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("promo_cat_${cat.id}")
                )
            }
        }

        // Days Filter Bar ("Según el Día")
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().testTag("promotions_day_filter_bar")
            ) {
                items(dayOptions) { (dayVal, dayLabel) ->
                    val isSelected = selectedDayFilter == dayVal
                    Surface(
                        onClick = { onSetDayFilter(dayVal) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("day_filter_$dayVal")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (dayVal == -1) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Small Day Context Indicator Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Promos para: $currentDayLabel".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${promotions.size} disponibles",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main Content: Toggle between Scrollable List and Interactive Map
        Crossfade(targetState = viewMode, label = "PromoViewModeCrossfade") { mode ->
            when (mode) {
                // ==================== 1. SCROLLABLE LIST VIEW ====================
                PromoDisplayMode.LIST -> {
                    if (promotions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No hay promociones para $currentDayLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Probá cambiando el día o ampliando el radio de búsqueda.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { onSetDayFilter(0) },
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Ver todos los días", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("promotions_list")
                        ) {
                            items(promotions) { item ->
                                val isFav = favoriteIds.contains(item.promo.id)
                                ElevatedCard(
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.elevatedCardElevation(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPromoForDetail = item.promo }
                                        .testTag("promo_card_${item.promo.id}")
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Top Row: Category tag + Distance + Favorite
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(item.promo.category.colorHex).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = item.promo.category.displayName.uppercase(),
                                                        color = Color(item.promo.category.colorHex),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.8.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "A ${String.format(Locale.US, "%.1f", item.distanceKm)} KM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Quick locate on map button
                                                IconButton(
                                                    onClick = {
                                                        selectedMapItem = MapItem.PromoItem(
                                                            promo = item.promo,
                                                            matchesUserCard = item.matchesUserCards,
                                                            distanceKm = item.distanceKm
                                                        )
                                                        viewMode = PromoDisplayMode.MAP
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("locate_map_${item.promo.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = "Ver en mapa",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                IconButton(
                                                    onClick = {
                                                        onToggleFavorite(
                                                            item.promo.id,
                                                            "PROMO",
                                                            item.promo.title,
                                                            "${item.promo.storeName} • ${item.promo.bank.displayName}"
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("fav_button_${item.promo.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                        contentDescription = "Favorito",
                                                        tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Main Content Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.promo.storeName.uppercase(),
                                                    style = MaterialTheme.typography.headlineSmall,
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
                                                Text(
                                                    text = item.promo.bank.displayName.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.5.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Discount text badge
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "-${item.promo.discountPercent.toInt()}%",
                                                    style = MaterialTheme.typography.displaySmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFB3261E),
                                                    lineHeight = 32.sp
                                                )
                                                Text(
                                                    text = "OFF",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Days Valid & Features Tags
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Days valid chip
                                            val daysText = formatDaysValidText(item.promo.daysValid)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (item.isTodayValid) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, if (item.isTodayValid) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = null,
                                                        tint = if (item.isTodayValid) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (item.isTodayValid) "VÁLIDO HOY" else daysText.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.4.sp,
                                                        color = if (item.isTodayValid) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (item.matchesUserCards) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Tu tarjeta",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }

                                            if (item.promo.cashbackCap != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                ) {
                                                    Text(
                                                        text = "Tope ${currencyFormat.format(item.promo.cashbackCap)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 2. INTERACTIVE MAP VIEW ====================
                PromoDisplayMode.MAP -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Embedded Interactive Map Canvas
                        InteractiveMapCanvas(
                            centerLocation = currentLocation,
                            searchRadiusKm = searchRadiusKm,
                            stations = emptyList(), // Show promotions only in this view
                            promotions = promotions,
                            selectedFuelType = FuelType.NAFTA_SUPER,
                            selectedMapItem = selectedMapItem,
                            onSelectItem = { selectedMapItem = it },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating Day & Count Badge on Top
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${promotions.size} PROMOS EN MAPA • $currentDayLabel".uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Bottom Overlay: Selected Promo Preview Card OR Horizontal Promo Carousel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            AnimatedVisibility(
                                visible = selectedMapItem is MapItem.PromoItem,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                            ) {
                                val promoItem = selectedMapItem as? MapItem.PromoItem
                                if (promoItem != null) {
                                    val promo = promoItem.promo
                                    ElevatedCard(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .testTag("map_promo_preview_card")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(promo.category.colorHex).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = promo.category.displayName.uppercase(),
                                                            color = Color(promo.category.colorHex),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 0.8.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "A ${String.format(Locale.US, "%.1f", promoItem.distanceKm)} KM",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { selectedMapItem = null },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = promo.storeName.uppercase(),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = promo.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${promo.bank.displayName.uppercase()} • ${formatDaysValidText(promo.daysValid)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "-${promo.discountPercent.toInt()}%",
                                                        style = MaterialTheme.typography.headlineMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFB3261E)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { selectedPromoForDetail = promo },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.weight(1f).testTag("map_promo_details_button")
                                                ) {
                                                    Text(
                                                        text = "VER DETALLES & REQUISITOS",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Horizontal Quick Browse Carousel
                            if (selectedMapItem == null && promotions.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("map_promo_horizontal_carousel")
                                ) {
                                    items(promotions) { item ->
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier
                                                .width(220.dp)
                                                .clickable {
                                                    selectedMapItem = MapItem.PromoItem(
                                                        promo = item.promo,
                                                        matchesUserCard = item.matchesUserCards,
                                                        distanceKm = item.distanceKm
                                                    )
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.promo.category.displayName.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 9.sp,
                                                        color = Color(item.promo.category.colorHex)
                                                    )
                                                    Text(
                                                        text = "-${item.promo.discountPercent.toInt()}%",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFB3261E)
                                                    )
                                                }
                                                Text(
                                                    text = item.promo.storeName.uppercase(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Black,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = item.promo.bank.shortName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "A ${String.format(Locale.US, "%.1f", item.distanceKm)} km • ${if (item.isTodayValid) "Hoy" else formatDaysValidText(item.promo.daysValid)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Sheet Dialog
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

private fun formatDaysValidText(days: List<Int>): String {
    if (days.size == 7) return "Todos los días"
    if (days.containsAll(listOf(2, 3, 4, 5, 6)) && days.size == 5) return "Lun a Vie"
    if (days.containsAll(listOf(7, 1)) && days.size == 2) return "Fines de semana"
    val dayNames = mapOf(
        2 to "Lun",
        3 to "Mar",
        4 to "Mié",
        5 to "Jue",
        6 to "Vie",
        7 to "Sáb",
        1 to "Dom"
    )
    return days.sortedBy { if (it == 1) 8 else it }.mapNotNull { dayNames[it] }.joinToString(", ")
}
