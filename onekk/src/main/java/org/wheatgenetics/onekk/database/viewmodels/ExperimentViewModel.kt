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

    fun contours(aid: Int) = liveData {

        val result = repo.selectAllContours(aid)

        emit(result)

    }

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
}