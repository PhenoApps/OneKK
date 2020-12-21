package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "analysis")
data class AnalysisEntity(

        @ColumnInfo(name = "selected")
        var selected: Boolean = false,

        @ColumnInfo(name = "name")
        var name: String? = null,

        @ColumnInfo(name = "date")
        var date: String? = null,

        @ColumnInfo(name = "weight")
        var weight: Double? = null,

        @ColumnInfo(name = "count")
        var count: Int? = null,

        @ColumnInfo(name = "uri")
        var uri: String? = null,

        @ColumnInfo(name = "collector")
        var collector: String? = null,

        @ColumnInfo(name = "minAxisAvg")
        var minAxisAvg: Double? = null,

        @ColumnInfo(name = "maxAxisAvg")
        var maxAxisAvg: Double? = null,

        @ColumnInfo(name = "minAxisVar")
        var minAxisVar: Double? = null,

        @ColumnInfo(name = "maxAxisVar")
        var maxAxisVar: Double? = null,

        @ColumnInfo(name = "minAxisCv")
        var minAxisCv: Double? = null,

        @ColumnInfo(name = "maxAxisCv")
        var maxAxisCv: Double? = null,

        @ColumnInfo(name = "aid")
        @PrimaryKey(autoGenerate = true)
        var aid: Int? = null)
