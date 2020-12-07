package org.wheatgenetics.onekk.interfaces

interface ContourOnTouchListener {
    fun onTouch(x: Double, y: Double, minAxis: Double, maxAxis: Double)
    fun onChoiceSwapped(id: Int, selected: Boolean)
}