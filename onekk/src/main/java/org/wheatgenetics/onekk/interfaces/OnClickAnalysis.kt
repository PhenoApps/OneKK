package org.wheatgenetics.onekk.interfaces

interface OnClickAnalysis {
    fun onClick(aid: Int)
    fun onClickCount(aid: Int)
    fun onSelectionSwapped(aid: Int, selected: Boolean)
}