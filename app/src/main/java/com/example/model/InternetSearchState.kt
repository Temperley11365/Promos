package com.example.model

/**
 * State representing continuous online search for banking promotions and deals.
 */
data class InternetSearchState(
    val isSearching: Boolean = false,
    val lastSearchedBank: String? = null,
    val totalOnlinePromosFound: Int = 0,
    val lastSearchTime: String = "Iniciando...",
    val statusMessage: String = "Monitoreo continuo de promociones en internet activo",
    val newlyFoundCount: Int = 0,
    val latestFoundPromos: List<Promotion> = emptyList()
)
