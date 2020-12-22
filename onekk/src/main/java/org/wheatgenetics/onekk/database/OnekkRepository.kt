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

    fun selectSourceImage(aid: Int) = dao.selectSourceImage(aid)

    fun getAnalysis(aid: Int) = dao.getAnalysis(aid)

    fun selectAllAnalysis() = dao.getAllAnalysis()

    suspend fun deleteAnalysis(aid: Int) = dao.deleteAnalysis(aid)

    suspend fun deleteAllAnalysis() = dao.deleteAllAnalysis()

    suspend fun updateSelectAllAnalysis() = dao.updateSelectAllAnalysis()

    suspend fun selectAllCountries(): List<String> {

        return selectAllCountriesAsync().await()

    }

    suspend fun selectAllCoinModels(country: String): List<CoinEntity> {

        return selectAllCoinModelsAsync(country).await()
    }

    private suspend fun selectAllCoinModelsAsync(country: String): Deferred<List<CoinEntity>> = withContext(Dispatchers.IO) {

        async {

            coinDao.selectAllCoinModels(country)

        }

    }

    fun selectAllContours() = dao.selectAllContours()

    fun selectContoursById(aid: Int) = dao.selectContoursById(aid)

//    suspend fun selectAllAnalysis(exp: ExperimentEntity): List<ImageEntity> {
//
//        return selectAllAnalysisAsync(exp).await()
//    }

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

    suspend fun updateContourCount(cid: Int, count: Int) = withContext(Dispatchers.IO) {
        dao.updateContourCount(cid, count)
    }

    suspend fun switchSelectedContour(aid: Int, id: Int, choice: Boolean) = withContext(Dispatchers.IO) {
        dao.switchSelectedContour(aid, id, choice)
    }

    suspend fun deleteContour(aid: Int, cid: Int) = withContext(Dispatchers.IO) {
        dao.deleteContour(aid, cid)
    }

    suspend fun updateCoinValue(country: String, name: String, value: Double) = withContext(Dispatchers.IO) {
        coinDao.updateCoinValue(CoinEntity(country, value.toString(), name))
    }

    suspend fun updateAnalysisWeight(aid: Int, weight: Double?) = withContext(Dispatchers.IO) {
        dao.updateAnalysisWeight(aid, weight)
    }

    suspend fun updateAnalysisCount(aid: Int, count: Int) = withContext(Dispatchers.IO) {
        dao.updateAnalysisCount(aid, count)
    }

    suspend fun updateAnalysisData(aid: Int,
                                         minAxisAvg: Double, minAxisVar: Double, minAxisCv: Double,
                                         maxAxisAvg: Double, maxAxisVar: Double, maxAxisCv: Double) = withContext(Dispatchers.IO) {
        dao.updateAnalysisData(aid, minAxisAvg, minAxisVar, minAxisCv, maxAxisAvg, maxAxisVar, maxAxisCv)
    }

    suspend fun updateAnalysisSelected(aid: Int, selected: Boolean) = withContext(Dispatchers.IO) {
        dao.updateAnalysisSelected(aid, selected)
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