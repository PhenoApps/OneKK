package org.wheatgenetics.onekk.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.CoinEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.ImageEntity

@Dao
interface OnekkDao {

    /** View queries **/

    /** Select queries **/
    @Query("SELECT * FROM image WHERE eid = 1")
    suspend fun selectAllAnalysis(): Array<ImageEntity>

    /** Inserts **/

    @Insert
    suspend fun insert(exp: ExperimentEntity): Long
    @Insert
    suspend fun insert(analysis: AnalysisEntity): Long
    @Insert
    suspend fun insert(image: ImageEntity): Long
    @Insert
    suspend fun insert(coin: CoinEntity): Long

    /**
     * Deletes
     */
    @Query("DELETE FROM analysis")
    suspend fun dropAnalysis()

    @Query("DELETE FROM experiment")
    suspend fun dropExperiment()
}