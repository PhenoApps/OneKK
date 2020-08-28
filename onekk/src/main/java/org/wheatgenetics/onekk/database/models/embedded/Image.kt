package org.wheatgenetics.onekk.database.models.embedded

import org.wheatgenetics.utils.DateUtil

data class Image(
        var url: String?,
        var date: String? = DateUtil().getTime()
)