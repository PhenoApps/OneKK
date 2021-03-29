package org.wheatgenetics.onekk.database.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.CoinEntity
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

    fun deleteAnalysis(aid: Int) = viewModelScope.launch {
        repo.deleteAnalysis(aid)
    }

    fun updateSelectAllAnalysis(selected: Boolean) = viewModelScope.launch {
        repo.updateSelectAllAnalysis(selected)
    }

    fun deleteAllAnalysis() = viewModelScope.launch {
        repo.deleteAllAnalysis()
    }

    fun insert(contour: ContourEntity) = viewModelScope.launch {
        repo.insert(contour.apply {
            this.contour?.let {
                //truncate decimal places
                this.contour?.area = BigDecimal(it.area ?: 0.0).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                this.contour?.maxAxis = BigDecimal(it.maxAxis ?: 0.0).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                this.contour?.minAxis = BigDecimal(it.minAxis ?: 0.0).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                this.contour?.x = BigDecimal(it.x).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                this.contour?.y = BigDecimal(it.y).setScale(2, RoundingMode.HALF_EVEN).toDouble()
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

    fun updateAnalysisTkw(aid: Int, tkw: Double) = viewModelScope.launch {
        repo.updateAnalysisTkw(aid, tkw)
    }

    fun updateAnalysisCount(aid: Int, count: Int) = viewModelScope.launch {
        repo.updateAnalysisCount(aid, count)
    }

    fun updateAnalysisData(aid: Int, totalArea: Double,
                           minAxisAvg: Double, minAxisVar: Double, minAxisCv: Double,
                           maxAxisAvg: Double, maxAxisVar: Double, maxAxisCv: Double) = viewModelScope.launch {

        //todo maybe make this precision a preference
        //round all saved values to 2 decimal places
        val totalArea = BigDecimal(totalArea).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val minAxisA = BigDecimal(minAxisAvg).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val minAxisV = BigDecimal(minAxisVar).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val minAxisC = BigDecimal(minAxisCv).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val maxAxisA = BigDecimal(maxAxisAvg).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val maxAxisV = BigDecimal(maxAxisVar).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        val maxAxisC = BigDecimal(maxAxisCv).setScale(2, RoundingMode.HALF_EVEN).toDouble()

        repo.updateAnalysisData(aid, totalArea, minAxisA, minAxisV, minAxisC, maxAxisA, maxAxisV, maxAxisC)
    }

    fun updateAnalysisSelected(aid: Int, selected: Boolean) = viewModelScope.launch {
        repo.updateAnalysisSelected(aid, selected)
    }

    fun clearAll() = viewModelScope.launch {

    }

    fun deleteContour(aid: Int, cid: Int) = viewModelScope.launch {
        repo.deleteContour(aid, cid)
    }

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
    fun loadCoinDatabase(csv: InputStream) = viewModelScope.async {

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

    fun diffCoinDatabase(csv: InputStream) = viewModelScope.async {

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