package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.Category
import com.example.model.FuelType
import com.example.model.GeoPoint
import com.example.ui.MapItem
import com.example.ui.PromoWithDistance
import com.example.ui.StationWithDistance
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveMapCanvas(
    centerLocation: GeoPoint,
    searchRadiusKm: Double,
    stations: List<StationWithDistance>,
    promotions: List<PromoWithDistance>,
    selectedFuelType: FuelType,
    selectedMapItem: MapItem?,
    onSelectItem: (MapItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Zoom and pan state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pulsing animation for user location
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Background and map theme colors
    val mapBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val roadColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val mainAvenueColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val parkColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFD1FAE5)
    val waterColor = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.45f) else Color(0xFFBAE6FD)
    val gridLineColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mapBgColor)
            .testTag("interactive_map_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.6f, 3.5f)
                    panOffset += pan
                }
            }
            .pointerInput(stations, promotions, centerLocation, zoomScale, panOffset) {
                detectTapGestures { tapOffset ->
                    // Calculate tapped coordinate relative to center
                    val centerX = size.width / 2f + panOffset.x
                    val centerY = size.height / 2f + panOffset.y
                    val baseScale = (size.width / (searchRadiusKm * 2.2f)) * zoomScale

                    var clickedItem: MapItem? = null
                    var minDistanceSq = 1600f // 40px hit target radius squared

                    // Check stations
                    stations.forEach { stationItem ->
                        val dLat = (stationItem.station.location.lat - centerLocation.lat)
                        val dLng = (stationItem.station.location.lng - centerLocation.lng)
                        val screenX = centerX + (dLng * 85000f).toFloat() * (baseScale / 1000f).toFloat()
                        val screenY = centerY - (dLat * 111000f).toFloat() * (baseScale / 1000f).toFloat()

                        val distSq = (tapOffset.x - screenX) * (tapOffset.x - screenX) + (tapOffset.y - screenY) * (tapOffset.y - screenY)
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq
                            clickedItem = MapItem.StationItem(
                                station = stationItem.station,
                                priceForSelectedFuel = stationItem.selectedFuelPrice,
                                fuelType = selectedFuelType,
                                cardDiscountPercent = stationItem.cardDiscountPercent,
                                cardDiscountPromo = stationItem.cardDiscountPromo,
                                distanceKm = stationItem.distanceKm
                            )
                        }
                    }

                    // Check promos if no station hit
                    if (clickedItem == null) {
                        promotions.forEach { promoItem ->
                            val dLat = (promoItem.promo.location.lat - centerLocation.lat)
                            val dLng = (promoItem.promo.location.lng - centerLocation.lng)
                            val screenX = centerX + (dLng * 85000f).toFloat() * (baseScale / 1000f).toFloat()
                            val screenY = centerY - (dLat * 111000f).toFloat() * (baseScale / 1000f).toFloat()

                            val distSq = (tapOffset.x - screenX) * (tapOffset.x - screenX) + (tapOffset.y - screenY) * (tapOffset.y - screenY)
                            if (distSq < minDistanceSq) {
                                minDistanceSq = distSq
                                clickedItem = MapItem.PromoItem(
                                    promo = promoItem.promo,
                                    matchesUserCard = promoItem.matchesUserCards,
                                    distanceKm = promoItem.distanceKm
                                )
                            }
                        }
                    }

                    onSelectItem(clickedItem)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panOffset.x
            val centerY = height / 2f + panOffset.y
            val pixelsPerKm = (width / (searchRadiusKm * 2.2f).toFloat()) * zoomScale

            // 1. Draw Map Urban Background Elements (Parks, Rivers, Street Grid)
            drawMapFeatures(
                centerX = centerX,
                centerY = centerY,
                zoomScale = zoomScale,
                roadColor = roadColor,
                mainAvenueColor = mainAvenueColor,
                parkColor = parkColor,
                waterColor = waterColor,
                gridLineColor = gridLineColor
            )

            // 2. Draw Radius Circle from user location
            val radiusInPixels = (searchRadiusKm.toFloat() * pixelsPerKm)
            drawCircle(
                color = primaryColor.copy(alpha = 0.06f),
                radius = radiusInPixels,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = radiusInPixels,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5f)
            )

            // 3. Draw Promotions Markers
            promotions.forEach { promoItem ->
                val dLat = (promoItem.promo.location.lat - centerLocation.lat)
                val dLng = (promoItem.promo.location.lng - centerLocation.lng)
                val screenX = centerX + (dLng * 85000f).toFloat() * (pixelsPerKm / 1000f)
                val screenY = centerY - (dLat * 111000f).toFloat() * (pixelsPerKm / 1000f)

                val isSelected = selectedMapItem?.id == promoItem.promo.id
                drawPromoMarker(
                    x = screenX,
                    y = screenY,
                    promo = promoItem,
                    isSelected = isSelected,
                    surfaceColor = surfaceColor
                )
            }

            // 4. Draw Gas Station Markers
            stations.forEach { stationItem ->
                val dLat = (stationItem.station.location.lat - centerLocation.lat)
                val dLng = (stationItem.station.location.lng - centerLocation.lng)
                val screenX = centerX + (dLng * 85000f).toFloat() * (pixelsPerKm / 1000f)
                val screenY = centerY - (dLat * 111000f).toFloat() * (pixelsPerKm / 1000f)

                val isSelected = selectedMapItem?.id == stationItem.station.id
                drawGasStationMarker(
                    x = screenX,
                    y = screenY,
                    station = stationItem,
                    isSelected = isSelected,
                    surfaceColor = surfaceColor
                )
            }

            // 5. Draw User / Current Center Location with animated pulsing radar
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = pulseRadius * zoomScale.coerceIn(0.8f, 1.4f),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = primaryColor,
                radius = 9f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(centerX, centerY)
            )
        }
    }
}

private fun DrawScope.drawMapFeatures(
    centerX: Float,
    centerY: Float,
    zoomScale: Float,
    roadColor: Color,
    mainAvenueColor: Color,
    parkColor: Color,
    waterColor: Color,
    gridLineColor: Color
) {
    val step = 90f * zoomScale

    // Water River Band (Diagonal artistic scenic river)
    val waterPath = Path().apply {
        moveTo(centerX + 350f * zoomScale, centerY - 600f * zoomScale)
        cubicTo(
            centerX + 280f * zoomScale, centerY - 200f * zoomScale,
            centerX + 380f * zoomScale, centerY + 200f * zoomScale,
            centerX + 260f * zoomScale, centerY + 650f * zoomScale
        )
        lineTo(centerX + 650f * zoomScale, centerY + 650f * zoomScale)
        lineTo(centerX + 650f * zoomScale, centerY - 600f * zoomScale)
        close()
    }
    drawPath(waterPath, color = waterColor)

    // Green Parks & Plazas
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerX - 240f * zoomScale, centerY - 180f * zoomScale),
        size = Size(140f * zoomScale, 110f * zoomScale),
        cornerRadius = CornerRadius(16f * zoomScale)
    )
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerX + 60f * zoomScale, centerY + 90f * zoomScale),
        size = Size(160f * zoomScale, 130f * zoomScale),
        cornerRadius = CornerRadius(20f * zoomScale)
    )
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerX - 320f * zoomScale, centerY + 200f * zoomScale),
        size = Size(120f * zoomScale, 100f * zoomScale),
        cornerRadius = CornerRadius(14f * zoomScale)
    )

    // Minor Street Grid lines
    var x = centerX % step - step * 2
    while (x < size.width + step * 2) {
        drawLine(
            color = gridLineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 3f
        )
        x += step
    }

    var y = centerY % step - step * 2
    while (y < size.height + step * 2) {
        drawLine(
            color = gridLineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 3f
        )
        y += step
    }

    // Major Diagonal & Cross Avenues
    drawLine(
        color = mainAvenueColor,
        start = Offset(0f, centerY - 120f * zoomScale),
        end = Offset(size.width, centerY + 180f * zoomScale),
        strokeWidth = 14f * zoomScale.coerceIn(0.7f, 1.6f)
    )
    drawLine(
        color = roadColor,
        start = Offset(0f, centerY - 120f * zoomScale),
        end = Offset(size.width, centerY + 180f * zoomScale),
        strokeWidth = 10f * zoomScale.coerceIn(0.7f, 1.6f)
    )

    drawLine(
        color = mainAvenueColor,
        start = Offset(centerX - 80f * zoomScale, 0f),
        end = Offset(centerX - 80f * zoomScale, size.height),
        strokeWidth = 12f * zoomScale.coerceIn(0.7f, 1.6f)
    )
    drawLine(
        color = roadColor,
        start = Offset(centerX - 80f * zoomScale, 0f),
        end = Offset(centerX - 80f * zoomScale, size.height),
        strokeWidth = 8f * zoomScale.coerceIn(0.7f, 1.6f)
    )
}

private fun DrawScope.drawGasStationMarker(
    x: Float,
    y: Float,
    station: StationWithDistance,
    isSelected: Boolean,
    surfaceColor: Color
) {
    val brandColor = Color(station.station.brand.primaryColorHex)
    val scale = if (isSelected) 1.25f else 1.0f

    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = 22f * scale,
        center = Offset(x, y + 4f)
    )

    // Outer ring
    drawCircle(
        color = if (isSelected) Color(0xFFF59E0B) else Color.White,
        radius = 20f * scale,
        center = Offset(x, y)
    )

    // Inner brand color circle
    drawCircle(
        color = brandColor,
        radius = 16f * scale,
        center = Offset(x, y)
    )

    // Price Badge on top
    val priceText = "$${station.selectedFuelPrice.toInt()}"
    val badgeWidth = 62f * scale
    val badgeHeight = 22f * scale
    val badgeTop = y - (36f * scale)

    drawRoundRect(
        color = brandColor,
        topLeft = Offset(x - badgeWidth / 2, badgeTop),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(10f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x - badgeWidth / 2, badgeTop),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(10f),
        style = Stroke(width = 1.5f)
    )

    // Discount indicator dot if promo active
    if (station.cardDiscountPercent > 0) {
        drawCircle(
            color = Color(0xFF10B981),
            radius = 6f * scale,
            center = Offset(x + badgeWidth / 2 - 2f, badgeTop + 2f)
        )
    }

    // Render price text
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f * scale
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText(priceText, x, badgeTop + (16f * scale), paint)
    }
}

private fun DrawScope.drawPromoMarker(
    x: Float,
    y: Float,
    promo: PromoWithDistance,
    isSelected: Boolean,
    surfaceColor: Color
) {
    val catColor = Color(promo.promo.category.colorHex)
    val scale = if (isSelected) 1.25f else 1.0f

    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = 18f * scale,
        center = Offset(x, y + 3f)
    )

    // Outer circle
    drawCircle(
        color = if (promo.matchesUserCards) Color(0xFF10B981) else Color.White,
        radius = 16f * scale,
        center = Offset(x, y)
    )

    // Inner circle
    drawCircle(
        color = catColor,
        radius = 12.5f * scale,
        center = Offset(x, y)
    )

    // Discount % badge
    val discountText = "-${promo.promo.discountPercent.toInt()}%"
    val badgeWidth = 46f * scale
    val badgeHeight = 18f * scale
    val badgeTop = y - (30f * scale)

    drawRoundRect(
        color = if (promo.matchesUserCards) Color(0xFF059669) else Color(0xFFEF4444),
        topLeft = Offset(x - badgeWidth / 2, badgeTop),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(8f)
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f * scale
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText(discountText, x, badgeTop + (14f * scale), paint)
    }
}
