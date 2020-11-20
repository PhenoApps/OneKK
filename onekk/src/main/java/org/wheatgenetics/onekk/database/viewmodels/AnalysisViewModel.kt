package org.wheatgenetics.onekk.database.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wheatgenetics.imageprocess.DetectRectangles
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity

class AnalysisViewModel(
        private val repo: OnekkRepository): ViewModel() {

//    fun analysis(exp: ExperimentEntity) = liveData<List<Bitmap>> {
//
//        val result = repo.selectAllAnalysis(exp)
//
//        emit(result.mapNotNull {
//            BitmapFactory.decodeFile(it.image?.url)
//        })
//
//    }

    fun insert(analysis: AnalysisEntity) = viewModelScope.launch {
        repo.insert(analysis)
    }

    fun dropAll() = viewModelScope.launch {
        repo.dropAnalysis()
    }
}