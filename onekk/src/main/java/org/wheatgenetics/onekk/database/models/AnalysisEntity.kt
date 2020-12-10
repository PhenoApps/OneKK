package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "analysis")
data class AnalysisEntity(

        @ColumnInfo(name = "aid")
        @PrimaryKey(autoGenerate = true)
        var aid: Int? = null)
