package org.wheatgenetics.onekk.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity

@Dao
interface OnekkDao {

    /** View queries **/

    /** Select queries **/

    @Query("SELECT uri FROM image WHERE aid = :aid LIMIT 1")
    fun selectSourceImage(aid: Int): LiveData<String>

    @Query("SELECT * FROM image")
    fun selectExampleImages(): LiveData<List<ImageEntity>>

    @Query("SELECT * FROM analysis WHERE aid = :aid LIMIT 1")
    suspend fun getAnalysis(aid: Int): AnalysisEntity

    @Query("SELECT * FROM analysis")
    fun getAllAnalysis(): LiveData<List<AnalysisEntity>>

//    @Query("SELECT * FROM image WHERE aid = :aid")
//    suspend fun selectAllAnalysis(aid: Int): List<ImageEntity>

    @Query("SELECT * FROM contour WHERE aid = :aid")
    fun selectContoursById(aid: Int): LiveData<List<ContourEntity>>

    @Query("SELECT * FROM contour")
    fun selectAllContours(): LiveData<List<ContourEntity>>

    /** Updates **/
    @Query("UPDATE contour SET selected = :selected WHERE aid = :aid AND cid = :cid")
    suspend fun switchSelectedContour(aid: Int, cid: Int, selected: Boolean)

    @Query("UPDATE contour SET count = :count WHERE cid = :cid")
    suspend fun updateContourCount(cid: Int, count: Int)

    @Query("UPDATE analysis SET weight = :weight, tkw = (:weight / count) * 1000.0 WHERE aid = :aid")
    suspend fun updateAnalysisWeight(aid: Int, weight: Double?)

    @Query("UPDATE analysis SET count = :count, tkw = (weight / :count) * 1000.0 WHERE aid = :aid")
    suspend fun updateAnalysisCount(aid: Int, count: Int)

    @Query("UPDATE analysis SET tkw = :tkw WHERE aid = :aid")
    suspend fun updateAnalysisTkw(aid: Int, tkw: Double)

    @Query("UPDATE analysis SET totalArea = :totalArea, minAxisAvg = :minAxisAvg, minAxisVar = :minAxisVar, minAxisCv = :minAxisCv, maxAxisAvg = :maxAxisAvg, maxAxisVar = :maxAxisVar, maxAxisCv = :maxAxisCv WHERE aid = :aid")
    suspend fun updateAnalysisData(aid: Int, totalArea: Double, minAxisAvg: Double, minAxisVar: Double, minAxisCv: Double,
                                   maxAxisAvg: Double, maxAxisVar: Double, maxAxisCv: Double)

    @Query("UPDATE analysis SET selected = :selected WHERE aid = :aid")
    suspend fun updateAnalysisSelected(aid: Int, selected: Boolean)

    @Query("UPDATE analysis SET selected = :selected")
    fun updateSelectAllAnalysis(selected: Boolean)

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

    @Query("DELETE FROM analysis WHERE aid = :aid")
    suspend fun deleteAnalysis(aid: Int)

    @Query("DELETE FROM analysis WHERE selected = 1")
    suspend fun deleteAllAnalysis()

}