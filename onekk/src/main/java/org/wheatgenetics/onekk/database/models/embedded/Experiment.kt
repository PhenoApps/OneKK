package org.wheatgenetics.onekk.database.models.embedded

import org.wheatgenetics.utils.DateUtil

data class Experiment(
        var name: String?,
        var date: String? = DateUtil().getTime()
)