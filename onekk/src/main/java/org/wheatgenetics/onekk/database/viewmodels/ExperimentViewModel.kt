package org.wheatgenetics.onekk.database.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import java.io.InputStream

class ExperimentViewModel(
        private val repo: OnekkRepository): ViewModel() {

    fun insert(contour: ContourEntity) = viewModelScope.launch {
        repo.insert(contour)
    }

    fun insert(img: ImageEntity) = viewModelScope.launch {
        repo.insert(img)
    }

    fun updateCoinValue(country: String, name: String, value: Double) = viewModelScope.launch {
        repo.updateCoinValue(country, name, value)
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

    fun coinModels(country: String) = liveData {

        val result = repo.selectAllCoinModels(country)

        emit(result)

    }

    fun contours(aid: Int) = liveData {

        val result = repo.selectAllContours(aid)

        emit(result)

    }

    suspend fun switchSelectedContour(aid: Int, id: Int, choice: Boolean) = repo.switchSelectedContour(aid, id, choice)

    fun getSourceImage(aid: Int) = repo.selectSourceImage(aid)

    suspend fun insert(analysis: AnalysisEntity): Long = viewModelScope.async {
        return@async repo.insert(analysis)
    }.await()

    fun dropAll() = viewModelScope.launch {
        repo.dropAnalysis()
    }

    /**
        lines of the csv file are expected to have 7 values like:
        UK,Pound,0.01,Penny,1,20.3,1 Penny
     */
    fun loadCoinDatabase(csv: InputStream) = viewModelScope.launch {

        csv.reader().readLines().forEach {

            val tokens = it.split(",")

            //country, diameter, and name respectively
            repo.insert(tokens[0], tokens[5], tokens[6])
        }
    }
}