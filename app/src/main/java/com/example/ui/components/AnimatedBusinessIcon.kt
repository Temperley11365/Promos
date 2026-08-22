package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import kotlinx.coroutines.launch

/**
 * Animated and interactive business category icon.
 * Features:
 * - Dynamic pulse & glowing aura
 * - Interactive spring-loaded tap bounce
 * - Category vector icon & emoji styling
 * - Smooth rotation shimmer
 */
@Composable
fun AnimatedBusinessIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBadge: Boolean = true,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val tapRotation = remember { Animatable(0f) }
    val interactionSource = remember { MutableInteractionSource() }

    // Continuous subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "BusinessIconPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )

    val categoryColor = Color(category.colorHex)
    val iconVector = getCategoryIconVector(category)

    val combinedModifier = modifier
        .size(size)
        .scale(if (isHighlighted) pulseScale * bounceScale.value else bounceScale.value)
        .rotate(tapRotation.value)
        .testTag("animated_business_icon_${category.id}")
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    coroutineScope.launch {
                        // Interactive spring bounce + slight tilt
                        launch {
                            bounceScale.animateTo(0.82f, spring(dampingRatio = 0.5f, stiffness = 1200f))
                            bounceScale.animateTo(1.15f, spring(dampingRatio = 0.4f, stiffness = 800f))
                            bounceScale.animateTo(1.0f, spring(dampingRatio = 0.6f, stiffness = 600f))
                        }
                        launch {
                            tapRotation.animateTo(12f, tween(100))
                            tapRotation.animateTo(-10f, tween(120))
                            tapRotation.animateTo(0f, tween(100))
                        }
                    }
                    onClick()
                }
            } else Modifier
        )

    Box(
        modifier = combinedModifier,
        contentAlignment = Alignment.Center
    ) {
        // Glowing Background Aura
        Box(
            modifier = Modifier
                .size(size * 1.15f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = auraAlpha),
                            categoryColor.copy(alpha = 0f)
                        )
                    )
                )
        )

        // Main Icon Container
        Surface(
            shape = RoundedCornerShape(size * 0.32f),
            color = categoryColor,
            shadowElevation = if (isHighlighted) 8.dp else 4.dp,
            modifier = Modifier.size(size * 0.85f)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category.displayName,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.46f)
                )
            }
        }

        // Mini Emoji Pill Badge
        if (showBadge && size >= 40.dp) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.42f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = category.emoji,
                        fontSize = (size.value * 0.22f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Interactive animated category selection chip with bounce & icon
 */
@Composable
fun InteractiveCategoryChip(
    category: Category,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val categoryColor = Color(category.colorHex)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) categoryColor else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = modifier
            .scale(scale.value)
            .clickable {
                coroutineScope.launch {
                    scale.animateTo(0.9f, tween(80))
                    scale.animateTo(1.05f, tween(100))
                    scale.animateTo(1.0f, tween(80))
                }
                onSelect()
            }
            .testTag("category_chip_${category.id}")
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                categoryColor,
                                categoryColor.copy(alpha = 0.85f)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedBusinessIcon(
                    category = category,
                    size = 28.dp,
                    showBadge = false,
                    isHighlighted = isSelected
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun getCategoryIconVector(category: Category): ImageVector {
    return when (category) {
        Category.SUPERMARKET -> Icons.Default.ShoppingCart
        Category.LOCAL_STORE -> Icons.Default.Storefront
        Category.BAKERY -> Icons.Default.BakeryDining
        Category.MARKET_GROCERY -> Icons.Default.Store
        Category.FUEL -> Icons.Default.LocalGasStation
        Category.GASTRONOMY -> Icons.Default.Restaurant
        Category.PHARMACY -> Icons.Default.LocalPharmacy
        Category.SHOPPING -> Icons.Default.ShoppingBag
    }
}
