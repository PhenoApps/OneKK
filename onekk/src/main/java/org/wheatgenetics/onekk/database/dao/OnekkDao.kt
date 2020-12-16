package org.wheatgenetics.onekk.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.wheatgenetics.onekk.database.models.*

@Dao
interface OnekkDao {

    /** View queries **/

    /** Select queries **/

    @Query("SELECT url FROM image WHERE aid = :aid LIMIT 1")
    fun selectSourceImage(aid: Int): LiveData<String>

    @Query("SELECT * FROM analysis WHERE aid = :aid LIMIT 1")
    fun getAnalysis(aid: Int): LiveData<AnalysisEntity>

    @Query("SELECT * FROM analysis")
    fun getAllAnalysis(): LiveData<List<AnalysisEntity>>

    @Query("SELECT * FROM image WHERE aid = :aid")
    suspend fun selectAllAnalysis(aid: Int): List<ImageEntity>

    @Query("SELECT * FROM contour WHERE aid = :aid")
    fun selectAllContours(aid: Int): LiveData<List<ContourEntity>>

    /** Updates **/
    @Query("UPDATE contour SET selected = :selected WHERE aid = :aid AND cid = :cid")
    suspend fun switchSelectedContour(aid: Int, cid: Int, selected: Boolean)

    @Query("UPDATE analysis SET weight = :weight WHERE aid = :aid")
    suspend fun updateAnalysisWeight(aid: Int, weight: Double?)

    @Query("UPDATE analysis SET count = :count WHERE aid = :aid")
    suspend fun updateAnalysisCount(aid: Int, count: Int)

    @Query("UPDATE analysis SET selected = :selected WHERE aid = :aid")
    suspend fun updateAnalysisSelected(aid: Int, selected: Boolean)

    /** Inserts **/
    @Insert
    suspend fun insert(analysis: AnalysisEntity): Long
    @Insert
    suspend fun insert(image: ImageEntity): Long
    @Insert
    suspend fun insert(contour: ContourEntity): Long

    /**
     * Deletes
     */
    @Query("DELETE FROM contour WHERE aid = :aid AND cid = :cid")
    suspend fun deleteContour(aid: Int, cid: Int)

    @Query("DELETE FROM analysis WHERE selected = 1")
    suspend fun deleteAllAnalysis()
}