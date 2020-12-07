package org.wheatgenetics.onekk.database.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import java.io.InputStream

class ExperimentViewModel(
        private val repo: OnekkRepository): ViewModel() {

    fun insert(contour: ContourEntity) = viewModelScope.launch {
        repo.insert(contour)
    }

    fun insert(exp: ExperimentEntity) = viewModelScope.launch {
        repo.insert(exp)
    }

    fun insert(img: ImageEntity) = viewModelScope.launch {
        repo.insert(img)
    }

    fun updateCoinValue(country: String, name: String, value: Double) = viewModelScope.launch {
        repo.updateCoinValue(country, name, value)
    }

    fun clearAll() = viewModelScope.launch {

    }
    fun deleteAll() = viewModelScope.launch {
        repo.dropExperiment()
    }

    fun deleteContour(aid: Int, cid: Int) = viewModelScope.launch {
        repo.deleteContour(aid, cid)
    }

    fun deleteExperiment(id: Int) = viewModelScope.launch {
        repo.deleteExperiment(id)
    }

    val experiments = repo.selectAllExperiment()

    fun countries() = liveData {

        val result = repo.selectAllCountries()

        emit(result)

    }

    fun coins(country: String) = liveData {

        val result = repo.selectAllCoins(country)

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

    fun analysis(exp: ExperimentEntity) = liveData<List<Bitmap>> {

//        val result = repo.selectAllAnalysis(exp)
//
//        val bmps = result.mapNotNull {
//            BitmapFactory.decodeFile(it.image?.url!!.toUri().path)
//        }
//
//        emit(bmps)

    }

    fun insert(analysis: AnalysisEntity): LiveData<Int> = liveData {

        val data = repo.insert(analysis).toInt()

        emit(data)

    }

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