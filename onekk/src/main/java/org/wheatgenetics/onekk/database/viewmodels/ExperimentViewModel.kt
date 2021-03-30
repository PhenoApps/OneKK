package org.wheatgenetics.onekk.database.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode

class ExperimentViewModel(
        private val repo: OnekkRepository): ViewModel() {

    suspend fun deleteSelectedAnalysis() {

        coroutineScope {

            withContext(Dispatchers.Default) {

                deleteAllAnalysis()

            }
        }
    }

//    fun deleteAnalysis(aid: Int) = viewModelScope.launch {
//        repo.deleteAnalysis(aid)
//    }

    fun updateSelectAllAnalysis(selected: Boolean) = viewModelScope.launch {
        repo.updateSelectAllAnalysis(selected)
    }

    private fun deleteAllAnalysis() = viewModelScope.launch {
        repo.deleteAllAnalysis()
    }

    fun insert(contour: ContourEntity) = viewModelScope.launch {
        repo.insert(contour.apply {
            this.contour?.let {
                //truncate decimal places
                this.contour?.area = (it.area ?: 0.0).truncate(2)
                this.contour?.maxAxis = (it.maxAxis ?: 0.0).truncate(2)
                this.contour?.minAxis = (it.minAxis ?: 0.0).truncate(2)
                this.contour?.x = it.x.truncate(2)
                this.contour?.y = it.y.truncate(2)
            }
        })
    }

    fun insert(img: ImageEntity) = viewModelScope.launch {
        repo.insert(img)
    }

    fun updateCoinValue(country: String, name: String, value: Double) = viewModelScope.launch {
        repo.updateCoinValue(country, name, value)
    }

    fun updateAnalysisWeight(aid: Int, weight: Double?) = viewModelScope.launch {
        repo.updateAnalysisWeight(aid, weight)
    }

//    fun updateAnalysisTkw(aid: Int, tkw: Double) = viewModelScope.launch {
//        repo.updateAnalysisTkw(aid, tkw)
//    }

    fun updateAnalysisCount(aid: Int, count: Int) = viewModelScope.launch {
        repo.updateAnalysisCount(aid, count)
    }

    private fun Double.truncate(scale: Int) = try {
        BigDecimal(this).setScale(scale, RoundingMode.HALF_EVEN).toDouble()
    } catch (e: Exception) {
        this
    }

    fun updateAnalysisData(aid: Int, totalArea: Double,
                           minAxisAvg: Double, minAxisVar: Double, minAxisCv: Double,
                           maxAxisAvg: Double, maxAxisVar: Double, maxAxisCv: Double) = viewModelScope.launch {

        //todo maybe make this precision a preference
        //round all saved values to 2 decimal places
        repo.updateAnalysisData(aid, totalArea.truncate(2),
            minAxisAvg.truncate(2),
            minAxisVar.truncate(2),
            minAxisCv.truncate(2),
            maxAxisAvg.truncate(2),
            maxAxisVar.truncate(2),
            maxAxisCv.truncate(2))
    }

    fun updateAnalysisSelected(aid: Int, selected: Boolean) = viewModelScope.launch {
        repo.updateAnalysisSelected(aid, selected)
    }

//    fun clearAll() = viewModelScope.launch {
//
//    }

//    fun deleteContour(aid: Int, cid: Int) = viewModelScope.launch {
//        repo.deleteContour(aid, cid)
//    }

    fun countries() = liveData {

        val result = repo.selectAllCountries()

        emit(result)

    }

    fun coins() = liveData {

        val result = repo.selectAllCoins()

        emit(result)

    }

    fun coinModels(country: String) = liveData {

        val result = repo.selectAllCoinModels(country)

        emit(result)

    }

    fun analysis() = repo.selectAllAnalysis()

    fun selectAllContours() = repo.selectAllContours()

    fun contours(aid: Int) = repo.selectContoursById(aid)

    suspend fun updateContourCount(cid: Int, count: Int) = repo.updateContourCount(cid, count)

    suspend fun switchSelectedContour(aid: Int, id: Int, choice: Boolean) = repo.switchSelectedContour(aid, id, choice)

    fun getSourceImage(aid: Int) = repo.selectSourceImage(aid)
    fun getExampleImages() = repo.selectExampleImages()

    fun getAnalysis(aid: Int) = liveData {

        val data = repo.getAnalysis(aid)

        emit(data)
    }

    suspend fun insert(analysis: AnalysisEntity): Long = viewModelScope.async {
        return@async repo.insert(analysis)
    }.await()

    /**
        lines of the csv file are expected to have 7 values like:
        UK,Pound,0.01,Penny,1,20.3,1 Penny
     */
    fun loadCoinDatabaseAsync(csv: InputStream) = viewModelScope.async {

        repo.deleteAllCoins()

        var skipHeaders = true

        csv.reader().readLines().forEach {

            if (!skipHeaders) {

                val tokens = it.split(",")

                println(tokens[0])
                //country, diameter, and name respectively
                repo.insert(tokens[0], tokens[5], tokens[6])

            }

            skipHeaders = false
        }
    }

    fun diffCoinDatabaseAsync(csv: InputStream) = viewModelScope.async {

        val changedCoins = ArrayList<String>()

        val coins = repo.coins()

        var skipHeaders = true

        csv.reader().readLines().forEach {

            if (!skipHeaders) {

                val tokens = it.split(",")

                val changed = coins.find { coin ->
                    coin.country == tokens[0]
                            && coin.name == tokens[6]
                            && coin.diameter != "NA"
                            && coin.diameter.toDouble() != tokens[5].toDouble()
                }

                if (changed != null) {

                    changedCoins.add("${changed.country} ${changed.name} ${changed.diameter} -> ${tokens[5]}")

                }
            }

            skipHeaders = false
        }

        return@async changedCoins
    }
}