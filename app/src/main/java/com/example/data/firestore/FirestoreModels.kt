package com.example.data.firestore

import com.example.model.Bank
import com.example.model.CardNetwork
import com.example.model.CardType
import com.example.model.Category
import com.example.model.FuelType
import com.example.model.GasStation
import com.example.model.GasStationBrand
import com.example.model.GeoPoint
import com.example.model.Promotion
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Firestore DTO models for storing and synchronizing Gas Stations and Promotions.
 */
@IgnoreExtraProperties
data class FirestoreGeoPoint(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var name: String = "",
    var address: String = ""
) {
    fun toDomain(): GeoPoint = GeoPoint(lat = lat, lng = lng, name = name, address = address)

    companion object {
        fun fromDomain(geo: GeoPoint): FirestoreGeoPoint =
            FirestoreGeoPoint(lat = geo.lat, lng = geo.lng, name = geo.name, address = geo.address)
    }
}

@IgnoreExtraProperties
data class FirestoreGasStation(
    var id: String = "",
    var brand: String = "ypf",
    var name: String = "",
    var address: String = "",
    var location: FirestoreGeoPoint = FirestoreGeoPoint(),
    var prices: Map<String, Double> = emptyMap(),
    var amenities: List<String> = emptyList(),
    var rating: Double = 4.5,
    var open24hs: Boolean = true,
    var specialPromo: String? = null,
    var specialPromoBankId: String? = null,
    var promoDiscountPercent: Double = 0.0,
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): GasStation {
        val brandEnum = GasStationBrand.entries.firstOrNull { it.id.equals(brand, ignoreCase = true) } ?: GasStationBrand.YPF
        val parsedPrices = prices.mapNotNull { (key, value) ->
            val fuelType = FuelType.entries.firstOrNull { it.name.equals(key, ignoreCase = true) || it.shortName.equals(key, ignoreCase = true) }
            fuelType?.let { it to value }
        }.toMap()

        return GasStation(
            id = id,
            brand = brandEnum,
            name = name,
            address = address,
            location = location.toDomain(),
            prices = if (parsedPrices.isNotEmpty()) parsedPrices else mapOf(
                FuelType.NAFTA_SUPER to 1050.0,
                FuelType.NAFTA_PREMIUM to 1290.0,
                FuelType.DIESEL_COMUN to 1100.0,
                FuelType.DIESEL_PREMIUM to 1350.0,
                FuelType.GNC to 550.0
            ),
            amenities = amenities,
            rating = rating,
            open24hs = open24hs,
            specialPromo = specialPromo,
            specialPromoBankId = specialPromoBankId,
            promoDiscountPercent = promoDiscountPercent
        )
    }

    companion object {
        fun fromDomain(station: GasStation): FirestoreGasStation =
            FirestoreGasStation(
                id = station.id,
                brand = station.brand.id,
                name = station.name,
                address = station.address,
                location = FirestoreGeoPoint.fromDomain(station.location),
                prices = station.prices.mapKeys { it.key.name },
                amenities = station.amenities,
                rating = station.rating,
                open24hs = station.open24hs,
                specialPromo = station.specialPromo,
                specialPromoBankId = station.specialPromoBankId,
                promoDiscountPercent = station.promoDiscountPercent,
                updatedAt = System.currentTimeMillis()
            )

        fun fromSnapshot(doc: DocumentSnapshot): FirestoreGasStation? {
            return try {
                doc.toObject(FirestoreGasStation::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@IgnoreExtraProperties
data class FirestorePromotion(
    var id: String = "",
    var title: String = "",
    var storeName: String = "",
    var categoryId: String = "fuel",
    var bankId: String = "santander",
    var cardNetwork: String? = null,
    var cardType: String = "ANY",
    var discountPercent: Double = 0.0,
    var cashbackCap: Double? = null,
    var daysValid: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    var description: String = "",
    var qrBonusPercent: Double? = null,
    var installmentsNoInterest: Int? = null,
    var validUntil: String = "",
    var location: FirestoreGeoPoint = FirestoreGeoPoint(),
    var address: String = "",
    var rating: Double = 4.8,
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Promotion {
        val categoryEnum = Category.fromId(categoryId)
        val bankEnum = Bank.fromId(bankId)
        val networkEnum = cardNetwork?.let { net ->
            CardNetwork.entries.firstOrNull { it.name.equals(net, ignoreCase = true) || it.displayName.equals(net, ignoreCase = true) }
        }
        val typeEnum = CardType.entries.firstOrNull { it.name.equals(cardType, ignoreCase = true) } ?: CardType.ANY

        return Promotion(
            id = id,
            title = title,
            storeName = storeName,
            category = categoryEnum,
            bank = bankEnum,
            cardNetwork = networkEnum,
            cardType = typeEnum,
            discountPercent = discountPercent,
            cashbackCap = cashbackCap,
            daysValid = if (daysValid.isNotEmpty()) daysValid else listOf(1, 2, 3, 4, 5, 6, 7),
            description = description,
            qrBonusPercent = qrBonusPercent,
            installmentsNoInterest = installmentsNoInterest,
            validUntil = validUntil,
            location = location.toDomain(),
            address = address,
            rating = rating
        )
    }

    companion object {
        fun fromDomain(promo: Promotion): FirestorePromotion =
            FirestorePromotion(
                id = promo.id,
                title = promo.title,
                storeName = promo.storeName,
                categoryId = promo.category.id,
                bankId = promo.bank.id,
                cardNetwork = promo.cardNetwork?.name,
                cardType = promo.cardType.name,
                discountPercent = promo.discountPercent,
                cashbackCap = promo.cashbackCap,
                daysValid = promo.daysValid,
                description = promo.description,
                qrBonusPercent = promo.qrBonusPercent,
                installmentsNoInterest = promo.installmentsNoInterest,
                validUntil = promo.validUntil,
                location = FirestoreGeoPoint.fromDomain(promo.location),
                address = promo.address,
                rating = promo.rating,
                updatedAt = System.currentTimeMillis()
            )

        fun fromSnapshot(doc: DocumentSnapshot): FirestorePromotion? {
            return try {
                doc.toObject(FirestorePromotion::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
    }
}
