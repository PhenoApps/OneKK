package org.wheatgenetics.onekk.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wheatgenetics.onekk.database.dao.CoinDao
import org.wheatgenetics.onekk.database.dao.OnekkDao
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.ImageEntity

class OnekkRepository
    private constructor(
            private val dao: OnekkDao, private val coinDao: CoinDao) {

    fun selectAllExperiment() = dao.selectAllExperiment()

    fun selectSourceImage(aid: Int) = dao.selectSourceImage(aid)

    suspend fun selectAllContours(aid: Int): List<ContourEntity> {

        return selectAllContoursAsync(aid).await()
    }

//    suspend fun selectAllAnalysis(exp: ExperimentEntity): List<ImageEntity> {
//
//        return selectAllAnalysisAsync(exp).await()
//    }

    private suspend fun selectAllContoursAsync(aid: Int): Deferred<List<ContourEntity>> = withContext(Dispatchers.IO) {

        return@withContext async {

            return@async dao.selectAllContours(aid)

        }
    }

//    private suspend fun selectAllAnalysisAsync(exp: ExperimentEntity): Deferred<List<ImageEntity>> = withContext(Dispatchers.IO) {
//
//        return@withContext async {
//
//           // return@async dao.selectAllAnalysis(exp.eid)
//
//        }
//    }

    suspend fun dropAnalysis() = withContext(Dispatchers.IO) {
        dao.dropAnalysis()
    }

    suspend fun deleteContour(aid: Int, cid: Int) = withContext(Dispatchers.IO) {
        dao.deleteContour(aid, cid)
    }

    suspend fun deleteExperiment(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteExperiment(id)
    }

    suspend fun dropExperiment() = withContext(Dispatchers.IO) {
        dao.dropExperiment()
    }

    suspend fun insert(contour: ContourEntity) = withContext(Dispatchers.IO) {
        dao.insert(contour)
    }

    suspend fun insert(analysis: AnalysisEntity) = withContext(Dispatchers.IO) {
        dao.insert(analysis)
    }

    suspend fun insert(exp: ExperimentEntity) = withContext(Dispatchers.IO) {
        dao.insert(exp)
    }

    suspend fun insert(img: ImageEntity) = withContext(Dispatchers.IO) {
        dao.insert(img)
    }

    companion object {

        @Volatile private var instance: OnekkRepository? = null

        fun getInstance(onekk: OnekkDao, coinDao: CoinDao) =
                instance ?: synchronized(this) {
                    instance ?: OnekkRepository(onekk, coinDao)
                        .also { instance = it }
                }
    }
}