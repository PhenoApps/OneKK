package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "analysis")
data class AnalysisEntity(

        @ColumnInfo(name = "date")
        var date: String? = null,

        @ColumnInfo(name = "weight")
        var weight: Double? = null,

        @ColumnInfo(name = "count")
        var count: Int? = null,

        @ColumnInfo(name = "aid")
        @PrimaryKey(autoGenerate = true)
        var aid: Int? = null)
