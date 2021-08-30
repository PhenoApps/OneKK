package org.wheatgenetics.onekk.database.models.embedded

import org.wheatgenetics.utils.DateUtil

data class Contour(
        var x: Double,
        var y: Double,
        var count: Int,
        var area: Double?,
        var minAxis: Double?,
        var maxAxis: Double?,
        var date: String? = DateUtil().getTime()
)