package org.wheatgenetics.onekk.interfaces

interface ContourOnTouchListener {
    fun onCountEdited(cid: Int, count: Int)
    fun onTouch(cid: Int, x: Double, y: Double, cluster: Boolean, minAxis: Double, maxAxis: Double)
    fun onChoiceSwapped(id: Int, selected: Boolean)
}