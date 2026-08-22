package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCardDao {
    @Query("SELECT * FROM user_cards ORDER BY isDefault DESC, id DESC")
    fun getAllCards(): Flow<List<UserCardEntity>>

    @Query("SELECT * FROM user_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Int): UserCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: UserCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<UserCardEntity>)

    @Update
    suspend fun updateCard(card: UserCardEntity)

    @Delete
    suspend fun deleteCard(card: UserCardEntity)

    @Query("DELETE FROM user_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)

    @Query("SELECT COUNT(*) FROM user_cards")
    suspend fun getCardCount(): Int
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT itemId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE itemId = :itemId LIMIT 1)")
    fun isFavorite(itemId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)
}

@Dao
interface PromotionReportDao {
    @Query("SELECT * FROM promotion_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<PromotionReportEntity>>

    @Query("SELECT promoId FROM promotion_reports")
    fun getReportedPromoIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: PromotionReportEntity): Long

    @Query("DELETE FROM promotion_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)
}

