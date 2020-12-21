package org.wheatgenetics.onekk.interfaces

import org.wheatgenetics.onekk.database.models.AnalysisEntity

interface OnClickAnalysis {
    fun onClick(aid: Int)
    fun onClickCount(aid: Int)
    fun onSelectionSwapped(position: Int, model: AnalysisEntity, selected: Boolean)
}