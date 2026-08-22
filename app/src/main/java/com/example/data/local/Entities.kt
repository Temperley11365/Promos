package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_cards")
data class UserCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bankId: String,
    val bankName: String,
    val cardType: String,
    val cardNetwork: String,
    val cardName: String,
    val last4: String,
    val colorHex: Long,
    val isDefault: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemType: String, // "PROMO" or "STATION"
    val itemId: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "promotion_reports")
data class PromotionReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val promoId: String,
    val promoTitle: String,
    val storeName: String,
    val bankName: String,
    val reason: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)

