package org.wheatgenetics.onekk.database

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wheatgenetics.onekk.database.dao.CoinDao
import org.wheatgenetics.onekk.database.dao.OnekkDao
import org.wheatgenetics.onekk.database.models.*

class OnekkRepository
    private constructor(
            private val dao: OnekkDao, private val coinDao: CoinDao) {

    fun selectAllExperiment() = dao.selectAllExperiment()

    fun selectSourceImage(aid: Int) = dao.selectSourceImage(aid)

    suspend fun selectAllCountries(): List<String> {

        return selectAllCountriesAsync().await()

    }

    suspend fun selectAllCoins(country: String): List<String> {

        return selectAllCoinsAsync(country).await()
    }

    suspend fun selectAllCoinModels(country: String): List<CoinEntity> {

        return selectAllCoinModelsAsync(country).await()
    }

    private suspend fun selectAllCoinModelsAsync(country: String): Deferred<List<CoinEntity>> = withContext(Dispatchers.IO) {

        async {

            coinDao.selectAllCoinModels(country)

        }

    }

    private suspend fun selectAllCoinsAsync(country: String): Deferred<List<String>> = withContext(Dispatchers.IO) {

        async {

            coinDao.selectAllCoins(country)

        }

    }

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

    private suspend fun selectAllCountriesAsync(): Deferred<List<String>> = withContext(Dispatchers.IO) {

        return@withContext async {

            return@async coinDao.selectAllCountries()

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

    suspend fun switchSelectedContour(aid: Int, id: Int, choice: Boolean) = withContext(Dispatchers.IO) {
        dao.switchSelectedContour(aid, id, choice)
    }

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

    suspend fun updateCoinValue(country: String, name: String, value: Double) = withContext(Dispatchers.IO) {
        coinDao.updateCoinValue(CoinEntity(country, value.toString(), name))
    }

    suspend fun insert(contour: ContourEntity) = withContext(Dispatchers.IO) {
        dao.insert(contour)
    }

    suspend fun insert(analysis: AnalysisEntity) = withContext(Dispatchers.IO) {
        dao.insert(analysis)
    }

    suspend fun insert(country: String, diameter: String, name: String) = withContext(Dispatchers.IO) {
        coinDao.insert(CoinEntity(country, diameter, name))
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