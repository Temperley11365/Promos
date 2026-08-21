package com.example.data.firestore

import android.util.Log
import com.example.model.GasStation
import com.example.model.Promotion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val stationsCollection = firestore.collection("gas_stations")
    private val promotionsCollection = firestore.collection("promotions")

    companion object {
        private const val TAG = "FirestoreRepository"
    }

    /**
     * Observes real-time updates for all gas stations in Firestore.
     */
    fun getStationsFlow(): Flow<List<GasStation>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = stationsCollection.addSnapshotListener { snapshot, error ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Error registering stations listener", e)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Observes real-time updates for all promotions in Firestore.
     */
    fun getPromotionsFlow(): Flow<List<Promotion>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = promotionsCollection.addSnapshotListener { snapshot, error ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Error registering promotions listener", e)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Saves or updates a gas station in Firestore.
     */
    suspend fun saveGasStation(station: GasStation): Result<Unit> = runCatching {
        val dto = FirestoreGasStation.fromDomain(station)
        stationsCollection.document(station.id).set(dto, SetOptions.merge()).await()
    }

    /**
     * Saves or updates a promotion in Firestore.
     */
    suspend fun savePromotion(promotion: Promotion): Result<Unit> = runCatching {
        val dto = FirestorePromotion.fromDomain(promotion)
        promotionsCollection.document(promotion.id).set(dto, SetOptions.merge()).await()
    }

    /**
     * Deletes a promotion from Firestore.
     */
    suspend fun deletePromotion(promoId: String): Result<Unit> = runCatching {
        promotionsCollection.document(promoId).delete().await()
    }

    /**
     * Syncs initial seed list to Firestore if the collection is empty.
     */
    suspend fun seedInitialDataIfEmpty(
        defaultStations: List<GasStation>,
        defaultPromos: List<Promotion>
    ) {
        try {
            val stationsSnapshot = stationsCollection.limit(1).get().await()
            if (stationsSnapshot.isEmpty) {
                Log.d(TAG, "Seeding default gas stations to Firestore...")
                for (station in defaultStations) {
                    saveGasStation(station)
                }
            }

            val promosSnapshot = promotionsCollection.limit(1).get().await()
            if (promosSnapshot.isEmpty) {
                Log.d(TAG, "Seeding default promotions to Firestore...")
                for (promo in defaultPromos) {
                    savePromotion(promo)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed Firestore (offline or unauthenticated)", e)
        }
    }
}
