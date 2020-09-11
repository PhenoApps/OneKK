package org.wheatgenetics.onekk.database

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wheatgenetics.onekk.database.dao.CoinDao
import org.wheatgenetics.onekk.database.dao.OnekkDao
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.ImageEntity

class OnekkRepository
    private constructor(
            private val dao: OnekkDao, private val coinDao: CoinDao) {

    suspend fun selectAllAnalysis(exp: ExperimentEntity): Array<ImageEntity> {

        return selectAllAnalysisAsync(exp).await()
    }

    private suspend fun selectAllAnalysisAsync(exp: ExperimentEntity): Deferred<Array<ImageEntity>> = withContext(Dispatchers.IO) {

        return@withContext async {

            return@async dao.selectAllAnalysis()

        }
    }

    suspend fun dropAnalysis() = withContext(Dispatchers.IO) {
        dao.dropAnalysis()
    }

    suspend fun dropExperiment() = withContext(Dispatchers.IO) {
        dao.dropExperiment()
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