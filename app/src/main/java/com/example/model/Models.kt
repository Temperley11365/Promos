package com.example.model

import java.util.Calendar

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val name: String,
    val address: String = ""
)

enum class Bank(
    val id: String,
    val displayName: String,
    val shortName: String,
    val primaryColorHex: Long
) {
    SANTANDER("santander", "Banco Santander", "Santander", 0xFFEC0000),
    GALICIA("galicia", "Banco Galicia", "Galicia", 0xFFFF6600),
    BBVA("bbva", "Banco BBVA", "BBVA", 0xFF004481),
    MACRO("macro", "Banco Macro", "Macro", 0xFF002B49),
    NACION("nacion", "Banco Nación", "BNA", 0xFF0072CE),
    PROVINCIA("provincia", "Banco Provincia (Cuenta DNI)", "BAPRO", 0xFF00A859),
    CIUDAD("ciudad", "Banco Ciudad", "Ciudad", 0xFFE30613),
    ICBC("icbc", "Banco ICBC", "ICBC", 0xFFC8102E),
    HSBC("hsbc", "Banco HSBC", "HSBC", 0xFFDB0011),
    BRUBANK("brubank", "Brubank", "Brubank", 0xFF5D1049),
    UALA("uala", "Ualá", "Ualá", 0xFFFF3366),
    MERCADO_PAGO("mercadopago", "Mercado Pago", "MP", 0xFF009EE3),
    MODO("modo", "MODO Billetera", "MODO", 0xFF00C853),
    TODOS("todos", "Cualquier Tarjeta / Efectivo", "General", 0xFF475569);

    companion object {
        fun fromId(id: String): Bank = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TODOS
    }
}

enum class CardNetwork(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    CABAL("Cabal"),
    MAESTRO("Maestro")
}

enum class CardType(val displayName: String) {
    CREDIT("Crédito"),
    DEBIT("Débito"),
    PREPAID("Prepaga / Virtual"),
    ANY("Cualquier Tipo")
}

enum class Category(
    val id: String,
    val displayName: String,
    val iconName: String,
    val colorHex: Long
) {
    FUEL("fuel", "Combustibles", "local_gas_station", 0xFF0284C7),
    SUPERMARKET("supermarket", "Supermercados", "shopping_cart", 0xFF10B981),
    GASTRONOMY("gastronomy", "Gastronomía & Bares", "restaurant", 0xFFF59E0B),
    SHOPPING("shopping", "Moda & Shopping", "shopping_bag", 0xFFEC4899),
    PHARMACY("pharmacy", "Farmacias & Salud", "local_pharmacy", 0xFF8B5CF6),
    TECH("tech", "Tecnología & Hogar", "devices", 0xFF3B82F6),
    ENTERTAINMENT("entertainment", "Cine & Salidas", "movie", 0xFFEF4444),
    SERVICES("services", "Servicios & Auto", "build", 0xFF64748B);

    companion object {
        fun fromId(id: String): Category = entries.firstOrNull { it.id == id } ?: FUEL
    }
}

enum class GasStationBrand(
    val id: String,
    val displayName: String,
    val primaryColorHex: Long,
    val appDiscountInfo: String
) {
    YPF("ypf", "YPF", 0xFF005BAC, "App YPF: hasta 15% off"),
    SHELL("shell", "Shell", 0xFFDD1D21, "Shell Box: hasta 10% off"),
    AXION("axion", "Axion Energy", 0xFF8B1D78, "ON Axion: hasta 10% off"),
    PUMA("puma", "Puma Energy", 0xFF007A3D, "Puma Pris: hasta 10% off"),
    GULF("gulf", "Gulf Oil", 0xFFF26522, "Gulf Rewards: 5% off")
}

enum class FuelType(val displayName: String, val shortName: String) {
    NAFTA_SUPER("Nafta Súper", "Súper"),
    NAFTA_PREMIUM("Nafta Premium", "Premium"),
    DIESEL_COMUN("Diesel Común", "Diesel"),
    DIESEL_PREMIUM("Diesel Premium", "Diesel+"),
    GNC("GNC (m³)", "GNC")
}

data class GasStation(
    val id: String,
    val brand: GasStationBrand,
    val name: String,
    val address: String,
    val location: GeoPoint,
    val prices: Map<FuelType, Double>,
    val amenities: List<String>,
    val rating: Double,
    val open24hs: Boolean,
    val specialPromo: String? = null,
    val specialPromoBankId: String? = null,
    val promoDiscountPercent: Double = 0.0
)

data class Promotion(
    val id: String,
    val title: String,
    val storeName: String,
    val category: Category,
    val bank: Bank,
    val cardNetwork: CardNetwork? = null,
    val cardType: CardType = CardType.ANY,
    val discountPercent: Double,
    val cashbackCap: Double? = null, // Tope de reintegro en $
    val daysValid: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Sunday, 2=Monday, ..., 7=Saturday
    val description: String,
    val qrBonusPercent: Double? = null,
    val installmentsNoInterest: Int? = null,
    val validUntil: String,
    val location: GeoPoint,
    val address: String,
    val rating: Double = 4.7
) {
    fun isValidToday(): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        return daysValid.contains(currentDayOfWeek)
    }

    fun getDaysSummary(): String {
        if (daysValid.size >= 7) return "Todos los días"
        val dayNames = daysValid.map {
            when (it) {
                1 -> "Dom"
                2 -> "Lun"
                3 -> "Mar"
                4 -> "Mié"
                5 -> "Jue"
                6 -> "Vie"
                7 -> "Sáb"
                else -> ""
            }
        }
        return dayNames.joinToString(", ")
    }
}

data class CardSavingsRank(
    val cardBank: Bank,
    val cardNetwork: CardNetwork,
    val cardType: CardType,
    val cardLast4: String,
    val promoTitle: String,
    val storeName: String,
    val discountPercent: Double,
    val originalAmount: Double,
    val savingsAmount: Double,
    val finalAmountToPay: Double,
    val cashbackCap: Double?,
    val isCapped: Boolean,
    val installments: Int? = null
)

data class CityZone(
    val id: String,
    val name: String,
    val province: String,
    val center: GeoPoint
)
