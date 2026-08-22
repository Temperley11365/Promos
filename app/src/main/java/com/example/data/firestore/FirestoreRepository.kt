package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.model.GasStation
import com.example.model.Promotion
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val context: Context? = null) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Throwable) {
                    Log.w(TAG, "FirebaseApp.initializeApp skipped or failed: ${e.message}")
                }
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore not initialized or unavailable: ${e.message}")
            null
        }
    }

    private val stationsCollection by lazy {
        firestore?.collection("gas_stations")
    }

    private val promotionsCollection by lazy {
        firestore?.collection("promotions")
    }

    private val reportsCollection by lazy {
        firestore?.collection("promotion_reports")
    }

    companion object {
        private const val TAG = "FirestoreRepository"
    }

    /**
     * Observes real-time updates for all gas stations in Firestore.
     */
    fun getStationsFlow(): Flow<List<GasStation>> {
        val targetCollection = stationsCollection ?: return flowOf(emptyList())

        return callbackFlow {
            var listener: ListenerRegistration? = null
            try {
                listener = targetCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for gas_stations", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val stations = snapshot.documents.mapNotNull { doc ->
                            FirestoreGasStation.fromSnapshot(doc)?.toDomain()
                        }
                        trySend(stations)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error registering stations listener: ${e.message}")
            }

            awaitClose {
                try {
                    listener?.remove()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error removing listener: ${e.message}")
                }
            }
        }
    }

    /**
     * Observes real-time updates for all promotions in Firestore.
     */
    fun getPromotionsFlow(): Flow<List<Promotion>> {
        val targetCollection = promotionsCollection ?: return flowOf(emptyList())

        return callbackFlow {
            var listener: ListenerRegistration? = null
            try {
                listener = targetCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for promotions", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val promos = snapshot.documents.mapNotNull { doc ->
                            FirestorePromotion.fromSnapshot(doc)?.toDomain()
                        }
                        trySend(promos)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error registering promotions listener: ${e.message}")
            }

            awaitClose {
                try {
                    listener?.remove()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error removing listener: ${e.message}")
                }
            }
        }
    }

    /**
     * Saves or updates a gas station in Firestore.
     */
    suspend fun saveGasStation(station: GasStation): Result<Unit> = runCatching {
        val collection = stationsCollection ?: throw IllegalStateException("Firestore unavailable")
        val dto = FirestoreGasStation.fromDomain(station)
        collection.document(station.id).set(dto, SetOptions.merge()).await()
    }

    /**
     * Saves or updates a promotion in Firestore.
     */
    suspend fun savePromotion(promotion: Promotion): Result<Unit> = runCatching {
        val collection = promotionsCollection ?: throw IllegalStateException("Firestore unavailable")
        val dto = FirestorePromotion.fromDomain(promotion)
        collection.document(promotion.id).set(dto, SetOptions.merge()).await()
    }

    /**
     * Deletes a promotion from Firestore.
     */
    suspend fun deletePromotion(promoId: String): Result<Unit> = runCatching {
        val collection = promotionsCollection ?: throw IllegalStateException("Firestore unavailable")
        collection.document(promoId).delete().await()
    }

    /**
     * Submits a community report for an incorrect or expired promotion.
     */
    suspend fun submitPromotionReport(
        promoId: String,
        promoTitle: String,
        storeName: String,
        bankName: String,
        reason: String,
        details: String
    ): Result<Unit> = runCatching {
        val collection = reportsCollection ?: throw IllegalStateException("Firestore unavailable")
        val reportData = hashMapOf(
            "promoId" to promoId,
            "promoTitle" to promoTitle,
            "storeName" to storeName,
            "bankName" to bankName,
            "reason" to reason,
            "details" to details,
            "timestamp" to System.currentTimeMillis(),
            "status" to "PENDING"
        )
        collection.add(reportData).await()
    }

    /**
     * Syncs initial seed list to Firestore if the collection is empty.
     */
    suspend fun seedInitialDataIfEmpty(
        defaultStations: List<GasStation>,
        defaultPromos: List<Promotion>
    ) {
        try {
            val sCollection = stationsCollection ?: return
            val pCollection = promotionsCollection ?: return

            val stationsSnapshot = sCollection.limit(1).get().await()
            if (stationsSnapshot.isEmpty) {
                Log.d(TAG, "Seeding default gas stations to Firestore...")
                for (station in defaultStations) {
                    saveGasStation(station)
                }
            }

            val promosSnapshot = pCollection.limit(1).get().await()
            if (promosSnapshot.isEmpty) {
                Log.d(TAG, "Seeding default promotions to Firestore...")
                for (promo in defaultPromos) {
                    savePromotion(promo)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not seed Firestore (offline or unauthenticated): ${e.message}")
        }
    }
}
