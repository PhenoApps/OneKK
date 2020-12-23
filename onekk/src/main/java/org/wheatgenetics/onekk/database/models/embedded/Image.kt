package org.wheatgenetics.onekk.database.models.embedded

import androidx.room.ColumnInfo
import org.wheatgenetics.utils.DateUtil

data class Image(
        var uri: String?,
        var example: String?,
        var date: String? = DateUtil().getTime()
)