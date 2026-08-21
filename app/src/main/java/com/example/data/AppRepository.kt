package com.example.data

import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.UserCardEntity
import com.example.model.Bank
import com.example.model.CardNetwork
import com.example.model.CardType
import com.example.model.CardSavingsRank
import com.example.model.Category
import com.example.model.CityZone
import com.example.model.FuelType
import com.example.model.GasStation
import com.example.model.GasStationBrand
import com.example.model.GeoPoint
import com.example.model.Promotion
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AppRepository(private val db: AppDatabase) {

    val userCardsFlow: Flow<List<UserCardEntity>> = db.userCardDao().getAllCards()
    val favoritesFlow: Flow<List<FavoriteEntity>> = db.favoriteDao().getAllFavorites()
    val favoriteIdsFlow: Flow<List<String>> = db.favoriteDao().getAllFavoriteIds()

    suspend fun initDefaultCardsIfEmpty() {
        if (db.userCardDao().getCardCount() == 0) {
            val sampleCards = listOf(
                UserCardEntity(
                    bankId = Bank.GALICIA.id,
                    bankName = Bank.GALICIA.displayName,
                    cardType = CardType.CREDIT.name,
                    cardNetwork = CardNetwork.VISA.name,
                    cardName = "Galicia Visa Signature",
                    last4 = "4821",
                    colorHex = 0xFFFF6600,
                    isDefault = true
                ),
                UserCardEntity(
                    bankId = Bank.SANTANDER.id,
                    bankName = Bank.SANTANDER.displayName,
                    cardType = CardType.DEBIT.name,
                    cardNetwork = CardNetwork.MASTERCARD.name,
                    cardName = "Santander Débito Black",
                    last4 = "9012",
                    colorHex = 0xFFEC0000,
                    isDefault = false
                ),
                UserCardEntity(
                    bankId = Bank.MERCADO_PAGO.id,
                    bankName = Bank.MERCADO_PAGO.displayName,
                    cardType = CardType.PREPAID.name,
                    cardNetwork = CardNetwork.MASTERCARD.name,
                    cardName = "Mercado Pago Mastercard",
                    last4 = "3310",
                    colorHex = 0xFF009EE3,
                    isDefault = false
                ),
                UserCardEntity(
                    bankId = Bank.PROVINCIA.id,
                    bankName = Bank.PROVINCIA.displayName,
                    cardType = CardType.DEBIT.name,
                    cardNetwork = CardNetwork.VISA.name,
                    cardName = "Cuenta DNI BAPRO",
                    last4 = "7749",
                    colorHex = 0xFF00A859,
                    isDefault = false
                )
            )
            db.userCardDao().insertCards(sampleCards)
        }
    }

    suspend fun addCard(
        bank: Bank,
        cardType: CardType,
        cardNetwork: CardNetwork,
        cardName: String,
        last4: String
    ) {
        val entity = UserCardEntity(
            bankId = bank.id,
            bankName = bank.displayName,
            cardType = cardType.name,
            cardNetwork = cardNetwork.name,
            cardName = cardName.ifBlank { "${bank.shortName} ${cardNetwork.displayName}" },
            last4 = last4.ifBlank { "0000" },
            colorHex = bank.primaryColorHex,
            isDefault = false
        )
        db.userCardDao().insertCard(entity)
    }

    suspend fun deleteCard(cardId: Int) {
        db.userCardDao().deleteCardById(cardId)
    }

    suspend fun toggleFavorite(itemId: String, itemType: String, title: String, subtitle: String, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            db.favoriteDao().deleteByItemId(itemId)
        } else {
            db.favoriteDao().insertFavorite(
                FavoriteEntity(
                    itemType = itemType,
                    itemId = itemId,
                    title = title,
                    subtitle = subtitle
                )
            )
        }
    }

    // City zones for quick location switching or user specification
    val cityZones = listOf(
        CityZone("palermo", "Palermo Soho", "CABA", GeoPoint(-34.5875, -58.4285, "Palermo Soho", "Plaza Serrano, CABA")),
        CityZone("belgrano", "Belgrano R", "CABA", GeoPoint(-34.5620, -58.4570, "Belgrano R", "Av. Cabildo & Juramento, CABA")),
        CityZone("recoleta", "Recoleta", "CABA", GeoPoint(-34.5880, -58.3930, "Recoleta", "Av. Santa Fe & Callao, CABA")),
        CityZone("madero", "Puerto Madero", "CABA", GeoPoint(-34.6080, -58.3625, "Puerto Madero", "Dique 3, CABA")),
        CityZone("microcentro", "Microcentro / Obelisco", "CABA", GeoPoint(-34.6037, -58.3816, "Microcentro", "Av. 9 de Julio & Corrientes, CABA")),
        CityZone("caballito", "Caballito", "CABA", GeoPoint(-34.6190, -58.4410, "Caballito", "Av. Rivadavia & Acoyte, CABA")),
        CityZone("cordoba", "Córdoba Capital", "Córdoba", GeoPoint(-31.4201, -64.1888, "Nueva Córdoba", "Plaza España, Córdoba")),
        CityZone("rosario", "Rosario", "Santa Fe", GeoPoint(-32.9468, -60.6393, "Rosario Centro", "Monumento a la Bandera, Rosario")),
        CityZone("mendoza", "Mendoza Ciudad", "Mendoza", GeoPoint(-32.8895, -68.8458, "Mendoza Centro", "Plaza Independencia, Mendoza"))
    )

    // Base Gas Stations dataset
    val gasStations: List<GasStation> = listOf(
        GasStation(
            id = "ypf_palermo_1",
            brand = GasStationBrand.YPF,
            name = "YPF Full - Palermo Soho",
            address = "Av. Scalabrini Ortiz 1950",
            location = GeoPoint(-34.5880, -58.4220, "YPF Palermo", "Av. Scalabrini Ortiz 1950"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1090.0,
                FuelType.NAFTA_PREMIUM to 1345.0,
                FuelType.DIESEL_COMUN to 1120.0,
                FuelType.DIESEL_PREMIUM to 1390.0,
                FuelType.GNC to 495.0
            ),
            amenities = listOf("Tienda Full 24hs", "Cajero Automático", "Inflador Digital", "GNC", "Lavadero"),
            rating = 4.8,
            open24hs = true,
            specialPromo = "15% reintegro pagando con App YPF y Banco Galicia",
            specialPromoBankId = Bank.GALICIA.id,
            promoDiscountPercent = 15.0
        ),
        GasStation(
            id = "shell_palermo_1",
            brand = GasStationBrand.SHELL,
            name = "Shell Select - Av. Córdoba",
            address = "Av. Córdoba 4580",
            location = GeoPoint(-34.5930, -58.4270, "Shell Av. Córdoba", "Av. Córdoba 4580"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1115.0,
                FuelType.NAFTA_PREMIUM to 1370.0,
                FuelType.DIESEL_COMUN to 1145.0,
                FuelType.DIESEL_PREMIUM to 1410.0,
                FuelType.GNC to 510.0
            ),
            amenities = listOf("Shell Select Café", "Shell Box App", "Inflador", "GNC"),
            rating = 4.7,
            open24hs = true,
            specialPromo = "10% de descuento con Santander Visa en V-Power",
            specialPromoBankId = Bank.SANTANDER.id,
            promoDiscountPercent = 10.0
        ),
        GasStation(
            id = "axion_palermo_1",
            brand = GasStationBrand.AXION,
            name = "Axion Energy Spot - Av. Santa Fe",
            address = "Av. Santa Fe 3750",
            location = GeoPoint(-34.5830, -58.4160, "Axion Santa Fe", "Av. Santa Fe 3750"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1085.0,
                FuelType.NAFTA_PREMIUM to 1335.0,
                FuelType.DIESEL_COMUN to 1110.0,
                FuelType.DIESEL_PREMIUM to 1380.0,
                FuelType.GNC to 490.0
            ),
            amenities = listOf("Spot! Café & Bakery", "App ON Axion", "Lavadero Exprés", "24hs"),
            rating = 4.6,
            open24hs = true,
            specialPromo = "10% off + 5% extra con BBVA Modo",
            specialPromoBankId = Bank.BBVA.id,
            promoDiscountPercent = 15.0
        ),
        GasStation(
            id = "puma_belgrano_1",
            brand = GasStationBrand.PUMA,
            name = "Puma Energy - Av. Cabildo",
            address = "Av. Cabildo 1820",
            location = GeoPoint(-34.5650, -58.4530, "Puma Cabildo", "Av. Cabildo 1820"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1075.0,
                FuelType.NAFTA_PREMIUM to 1320.0,
                FuelType.DIESEL_COMUN to 1098.0,
                FuelType.DIESEL_PREMIUM to 1365.0,
                FuelType.GNC to 480.0
            ),
            amenities = listOf("Puma Pris App", "Super 7 Store", "Inflador", "GNC"),
            rating = 4.5,
            open24hs = true,
            specialPromo = "10% de ahorro todos los miércoles con Banco Nación BNA+",
            specialPromoBankId = Bank.NACION.id,
            promoDiscountPercent = 10.0
        ),
        GasStation(
            id = "ypf_recoleta_1",
            brand = GasStationBrand.YPF,
            name = "YPF Full - Av. Las Heras",
            address = "Av. Las Heras 2300",
            location = GeoPoint(-34.5890, -58.3980, "YPF Recoleta", "Av. Las Heras 2300"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1090.0,
                FuelType.NAFTA_PREMIUM to 1345.0,
                FuelType.DIESEL_COMUN to 1120.0,
                FuelType.DIESEL_PREMIUM to 1390.0,
                FuelType.GNC to 495.0
            ),
            amenities = listOf("Tienda Full Gourmet", "Cajero Link/Banelco", "24hs", "Aire y Agua"),
            rating = 4.9,
            open24hs = true,
            specialPromo = "20% en Infinia con Cuenta DNI Banco Provincia los sábados",
            specialPromoBankId = Bank.PROVINCIA.id,
            promoDiscountPercent = 20.0
        ),
        GasStation(
            id = "gulf_caballito_1",
            brand = GasStationBrand.GULF,
            name = "Gulf Oil - Av. Gaona",
            address = "Av. Gaona 1450",
            location = GeoPoint(-34.6130, -58.4480, "Gulf Caballito", "Av. Gaona 1450"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1065.0,
                FuelType.NAFTA_PREMIUM to 1310.0,
                FuelType.DIESEL_COMUN to 1090.0,
                FuelType.DIESEL_PREMIUM to 1350.0,
                FuelType.GNC to 475.0
            ),
            amenities = listOf("Mini Market", "GNC Alto Flujo", "Inflador"),
            rating = 4.4,
            open24hs = false,
            specialPromo = "5% off directo con Mercado Pago QR",
            specialPromoBankId = Bank.MERCADO_PAGO.id,
            promoDiscountPercent = 5.0
        ),
        GasStation(
            id = "shell_madero_1",
            brand = GasStationBrand.SHELL,
            name = "Shell Dique - Puerto Madero",
            address = "Av. Alicia Moreau de Justo 1100",
            location = GeoPoint(-34.6060, -58.3650, "Shell Puerto Madero", "Av. Alicia Moreau de Justo 1100"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1125.0,
                FuelType.NAFTA_PREMIUM to 1385.0,
                FuelType.DIESEL_COMUN to 1150.0,
                FuelType.DIESEL_PREMIUM to 1420.0,
                FuelType.GNC to 520.0
            ),
            amenities = listOf("Shell Select Premium", "Cafetería Starbucks on the go", "Cajero Automático", "Lavadero"),
            rating = 4.9,
            open24hs = true,
            specialPromo = "15% off V-Power con Tarjeta ICBC Visa",
            specialPromoBankId = Bank.ICBC.id,
            promoDiscountPercent = 15.0
        ),
        GasStation(
            id = "ypf_microcentro_1",
            brand = GasStationBrand.YPF,
            name = "YPF Centro - Av. Belgrano",
            address = "Av. Belgrano 980",
            location = GeoPoint(-34.6110, -58.3800, "YPF Centro", "Av. Belgrano 980"),
            prices = mapOf(
                FuelType.NAFTA_SUPER to 1090.0,
                FuelType.NAFTA_PREMIUM to 1345.0,
                FuelType.DIESEL_COMUN to 1120.0,
                FuelType.DIESEL_PREMIUM to 1390.0,
                FuelType.GNC to 495.0
            ),
            amenities = listOf("Tienda Full 24hs", "GNC", "Cajero Automático"),
            rating = 4.6,
            open24hs = true,
            specialPromo = "15% de ahorro con Banco Macro y MODO",
            specialPromoBankId = Bank.MACRO.id,
            promoDiscountPercent = 15.0
        )
    )

    // Base Promotions dataset
    val promotions: List<Promotion> = listOf(
        Promotion(
            id = "promo_ypf_galicia",
            title = "15% de Ahorro en Naftas y Tienda Full",
            storeName = "YPF",
            category = Category.FUEL,
            bank = Bank.GALICIA,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.CREDIT,
            discountPercent = 15.0,
            cashbackCap = 15000.0,
            daysValid = listOf(2, 4, 6), // Lun, Mié, Vie
            description = "Ahorrá 15% pagando con Dinero en Cuenta o Tarjeta de Crédito Galicia Visa a través de App YPF o MODO.",
            qrBonusPercent = 5.0,
            installmentsNoInterest = null,
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5880, -58.4220, "YPF Palermo", "Av. Scalabrini Ortiz 1950"),
            address = "Av. Scalabrini Ortiz 1950, Palermo",
            rating = 4.9
        ),
        Promotion(
            id = "promo_coto_santander",
            title = "25% de Reintegro en Coto Digital y Sucursales",
            storeName = "Coto Supermercados",
            category = Category.SUPERMARKET,
            bank = Bank.SANTANDER,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.DEBIT,
            discountPercent = 25.0,
            cashbackCap = 20000.0,
            daysValid = listOf(4, 5), // Mié, Jue
            description = "25% de ahorro con Tarjetas Santander Débito y Crédito Visa. Tope de reintegro mensual $20.000 por cuenta.",
            installmentsNoInterest = 3,
            validUntil = "30 Nov 2026",
            location = GeoPoint(-34.5850, -58.4260, "Coto Palermo", "Honduras 3850"),
            address = "Honduras 3850, Palermo",
            rating = 4.8
        ),
        Promotion(
            id = "promo_carrefour_mercadopago",
            title = "20% OFF en Carrefour Express y Maxi",
            storeName = "Carrefour",
            category = Category.SUPERMARKET,
            bank = Bank.MERCADO_PAGO,
            cardNetwork = CardNetwork.MASTERCARD,
            cardType = CardType.PREPAID,
            discountPercent = 20.0,
            cashbackCap = 10000.0,
            daysValid = listOf(1, 2, 3, 4, 5, 6, 7), // Todos los días
            description = "20% de descuento escaneando código QR de Mercado Pago con dinero en cuenta o tarjeta prepaga Mastercard.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5905, -58.4310, "Carrefour Express", "Av. Córdoba 4100"),
            address = "Av. Córdoba 4100, Palermo",
            rating = 4.6
        ),
        Promotion(
            id = "promo_shell_bapro",
            title = "30% de Reintegro con Cuenta DNI",
            storeName = "Shell",
            category = Category.FUEL,
            bank = Bank.PROVINCIA,
            cardNetwork = null,
            cardType = CardType.DEBIT,
            discountPercent = 30.0,
            cashbackCap = 12000.0,
            daysValid = listOf(6, 7), // Vie, Sáb
            description = "30% de reintegro pagando con Cuenta DNI en estaciones Shell adheridas. Tope $12.000 unificado por persona y por semana.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5930, -58.4270, "Shell Av. Córdoba", "Av. Córdoba 4580"),
            address = "Av. Córdoba 4580, Palermo",
            rating = 4.9
        ),
        Promotion(
            id = "promo_starbucks_galicia",
            title = "30% OFF en Cafetería & Pastelería",
            storeName = "Starbucks Coffee",
            category = Category.GASTRONOMY,
            bank = Bank.GALICIA,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.CREDIT,
            discountPercent = 30.0,
            cashbackCap = 8000.0,
            daysValid = listOf(2, 3, 4, 5, 6), // Lun a Vie
            description = "Disfrutá 30% de ahorro en tu desayuno y merienda pagando con Galicia Éminent o Débito.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5870, -58.4240, "Starbucks Palermo", "Malabia 1720"),
            address = "Malabia 1720, Palermo Soho",
            rating = 4.8
        ),
        Promotion(
            id = "promo_mcdonalds_uala",
            title = "25% de Reintegro en Combos Cuarto de Libra",
            storeName = "McDonald's",
            category = Category.GASTRONOMY,
            bank = Bank.UALA,
            cardNetwork = CardNetwork.MASTERCARD,
            cardType = CardType.PREPAID,
            discountPercent = 25.0,
            cashbackCap = 6000.0,
            daysValid = listOf(1, 2, 3, 4, 5, 6, 7),
            description = "25% de cashback instantáneo en la app de Ualá para consumos en locales y AutoMac.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5840, -58.4200, "McDonald's Santa Fe", "Av. Santa Fe 3900"),
            address = "Av. Santa Fe 3900, Palermo",
            rating = 4.7
        ),
        Promotion(
            id = "promo_farmacity_bbva",
            title = "30% de Ahorro en Cuidado Personal y Belleza",
            storeName = "Farmacity",
            category = Category.PHARMACY,
            bank = Bank.BBVA,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.CREDIT,
            discountPercent = 30.0,
            cashbackCap = 15000.0,
            daysValid = listOf(3, 5), // Mar, Jue
            description = "30% de ahorro en productos seleccionados y 3 cuotas sin interés con BBVA Visa o Mastercard vía MODO.",
            installmentsNoInterest = 3,
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5860, -58.4280, "Farmacity Soho", "Jorge Luis Borges 2100"),
            address = "Jorge Luis Borges 2100, Palermo",
            rating = 4.7
        ),
        Promotion(
            id = "promo_zara_santander",
            title = "20% OFF + 6 Cuotas Sin Interés",
            storeName = "Zara",
            category = Category.SHOPPING,
            bank = Bank.SANTANDER,
            cardNetwork = CardNetwork.MASTERCARD,
            cardType = CardType.CREDIT,
            discountPercent = 20.0,
            cashbackCap = 25000.0,
            daysValid = listOf(5, 6, 7), // Jue, Vie, Sáb
            description = "20% de ahorro exclusivo para clientes Santander Women y Black en indumentaria de temporada.",
            installmentsNoInterest = 6,
            validUntil = "15 Dic 2026",
            location = GeoPoint(-34.5865, -58.4110, "Alto Palermo Shopping", "Av. Santa Fe 3253"),
            address = "Alto Palermo Shopping, Nivel 1",
            rating = 4.9
        ),
        Promotion(
            id = "promo_fravega_macro",
            title = "15% de Reintegro + 12 Cuotas Sin Interés",
            storeName = "Frávega",
            category = Category.TECH,
            bank = Bank.MACRO,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.CREDIT,
            discountPercent = 15.0,
            cashbackCap = 35000.0,
            daysValid = listOf(1, 2, 3, 4, 5, 6, 7),
            description = "15% de ahorro en Smart TVs, Celulares y Electrodomésticos con tarjetas Macro Selecta.",
            installmentsNoInterest = 12,
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5845, -58.4125, "Frávega Alto Palermo", "Av. Santa Fe 3360"),
            address = "Av. Santa Fe 3360, Palermo",
            rating = 4.5
        ),
        Promotion(
            id = "promo_cinemark_modo",
            title = "2x1 en Entradas 2D y 3D + 20% en Popcorn",
            storeName = "Cinemark Hoyts",
            category = Category.ENTERTAINMENT,
            bank = Bank.MODO,
            cardNetwork = null,
            cardType = CardType.ANY,
            discountPercent = 50.0,
            cashbackCap = 8000.0,
            daysValid = listOf(1, 2, 3, 4, 5, 6, 7),
            description = "2x1 en entradas todos los días pagando con cualquier banco a través de QR MODO.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5870, -58.4100, "Cinemark Palermo", "Beruti 3399"),
            address = "Beruti 3399, Palermo",
            rating = 4.8
        ),
        Promotion(
            id = "promo_axion_bbva",
            title = "15% de Descuento en Nafta Quantium",
            storeName = "Axion Energy",
            category = Category.FUEL,
            bank = Bank.BBVA,
            cardNetwork = CardNetwork.MASTERCARD,
            cardType = CardType.CREDIT,
            discountPercent = 15.0,
            cashbackCap = 12000.0,
            daysValid = listOf(3, 7), // Mar, Sáb
            description = "10% con ON Axion + 5% acumulable pagando con BBVA Mastercard a través de MODO.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5830, -58.4160, "Axion Santa Fe", "Av. Santa Fe 3750"),
            address = "Av. Santa Fe 3750, Palermo",
            rating = 4.6
        ),
        Promotion(
            id = "promo_jumbo_nacion",
            title = "30% OFF en Jumbo con BNA+ MODO",
            storeName = "Jumbo Hipermercado",
            category = Category.SUPERMARKET,
            bank = Bank.NACION,
            cardNetwork = CardNetwork.VISA,
            cardType = CardType.DEBIT,
            discountPercent = 30.0,
            cashbackCap = 25000.0,
            daysValid = listOf(4), // Miércoles
            description = "30% de reintegro abonando con tarjeta de débito o crédito del Banco Nación escaneando QR BNA+.",
            validUntil = "31 Dic 2026",
            location = GeoPoint(-34.5710, -58.4290, "Jumbo Palermo", "Av. Bullrich 345"),
            address = "Av. Int. Bullrich 345, Palermo",
            rating = 4.8
        )
    )

    // Haversine Distance in Kilometers
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Check if promo applies to any of user's registered cards
    fun doesPromoMatchUserCards(promo: Promotion, userCards: List<UserCardEntity>): Boolean {
        if (promo.bank == Bank.TODOS || promo.bank == Bank.MODO) return true
        return userCards.any { card ->
            val bankMatches = card.bankId.equals(promo.bank.id, ignoreCase = true)
            val networkMatches = promo.cardNetwork == null || card.cardNetwork.equals(promo.cardNetwork.name, ignoreCase = true)
            val typeMatches = promo.cardType == CardType.ANY || card.cardType.equals(promo.cardType.name, ignoreCase = true)
            bankMatches && networkMatches && typeMatches
        }
    }

    // Find best promo matching a gas station and user cards
    fun getBestPromoForGasStation(station: GasStation, userCards: List<UserCardEntity>): Pair<Double, String?> {
        val matchingStationPromos = promotions.filter {
            it.category == Category.FUEL &&
                    it.storeName.contains(station.brand.displayName, ignoreCase = true) &&
                    doesPromoMatchUserCards(it, userCards) &&
                    it.isValidToday()
        }

        val bestPromo = matchingStationPromos.maxByOrNull { it.discountPercent }
        return if (bestPromo != null) {
            Pair(bestPromo.discountPercent, bestPromo.title)
        } else if (station.specialPromoBankId != null && userCards.any { it.bankId == station.specialPromoBankId }) {
            Pair(station.promoDiscountPercent, station.specialPromo)
        } else {
            Pair(0.0, null)
        }
    }

    // Smart Purchase Savings Simulator
    fun simulatePurchaseSavings(
        amount: Double,
        category: Category,
        userCards: List<UserCardEntity>
    ): List<CardSavingsRank> {
        val matchingPromos = promotions.filter { it.category == category }
        val results = mutableListOf<CardSavingsRank>()

        for (card in userCards) {
            val bank = Bank.fromId(card.bankId)
            val network = CardNetwork.entries.firstOrNull { it.name.equals(card.cardNetwork, ignoreCase = true) } ?: CardNetwork.VISA
            val type = CardType.entries.firstOrNull { it.name.equals(card.cardType, ignoreCase = true) } ?: CardType.CREDIT

            // Check matching promos for this card
            val validPromos = matchingPromos.filter { promo ->
                (promo.bank == bank || promo.bank == Bank.TODOS || promo.bank == Bank.MODO) &&
                        (promo.cardNetwork == null || promo.cardNetwork == network) &&
                        (promo.cardType == CardType.ANY || promo.cardType == type)
            }

            if (validPromos.isNotEmpty()) {
                val best = validPromos.maxByOrNull { it.discountPercent }!!
                val rawSavings = amount * (best.discountPercent / 100.0)
                val isCapped = best.cashbackCap != null && rawSavings > best.cashbackCap
                val finalSavings = if (isCapped) best.cashbackCap!! else rawSavings
                val toPay = (amount - finalSavings).coerceAtLeast(0.0)

                results.add(
                    CardSavingsRank(
                        cardBank = bank,
                        cardNetwork = network,
                        cardType = type,
                        cardLast4 = card.last4,
                        promoTitle = best.title,
                        storeName = best.storeName,
                        discountPercent = best.discountPercent,
                        originalAmount = amount,
                        savingsAmount = finalSavings,
                        finalAmountToPay = toPay,
                        cashbackCap = best.cashbackCap,
                        isCapped = isCapped,
                        installments = best.installmentsNoInterest
                    )
                )
            } else {
                // Base payment with 0% discount
                results.add(
                    CardSavingsRank(
                        cardBank = bank,
                        cardNetwork = network,
                        cardType = type,
                        cardLast4 = card.last4,
                        promoTitle = "Sin descuento específico registrado para esta categoría",
                        storeName = "General",
                        discountPercent = 0.0,
                        originalAmount = amount,
                        savingsAmount = 0.0,
                        finalAmountToPay = amount,
                        cashbackCap = null,
                        isCapped = false
                    )
                )
            }
        }

        return results.sortedByDescending { it.savingsAmount }
    }
}
