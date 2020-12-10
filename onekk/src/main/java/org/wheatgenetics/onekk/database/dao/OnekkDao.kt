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

    @Query("SELECT * FROM image WHERE aid = :aid")
    suspend fun selectAllAnalysis(aid: Int): List<ImageEntity>

    @Query("SELECT * FROM contour WHERE aid = :aid")
    suspend fun selectAllContours(aid: Int): List<ContourEntity>

    /** Updates **/
    @Query("UPDATE contour SET selected = :selected WHERE aid = :aid AND cid = :cid")
    suspend fun switchSelectedContour(aid: Int, cid: Int, selected: Boolean)

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

    @Query("DELETE FROM analysis")
    suspend fun dropAnalysis()

}