package org.wheatgenetics.onekk.database.dao

import androidx.room.*
import org.wheatgenetics.onekk.database.models.CoinEntity

@Dao
interface CoinDao {

    /** View queries **/

    /** Select queries **/
    @Query("SELECT DISTINCT name FROM coin")
    suspend fun selectAllCoins(): List<String>

    @Query("SELECT DISTINCT * FROM coin")
    suspend fun coins(): List<CoinEntity>

    @Query("SELECT DISTINCT country FROM coin")
    suspend fun selectAllCountries(): List<String>

    @Query("SELECT * FROM coin WHERE country = :country")
    suspend fun selectAllCoinModels(country: String): List<CoinEntity>

    /**
     * Inserts
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCoinValue(model: CoinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coin: CoinEntity): Long

    /**
     * Deletes
     */

    @Query("DELETE FROM coin")
    suspend fun deleteAllCoin()
}