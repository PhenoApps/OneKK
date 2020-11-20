package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "analysis",
        foreignKeys = [ForeignKey(entity = ExperimentEntity::class,
                        parentColumns = ["eid"], childColumns = ["eid"],
                onDelete = ForeignKey.CASCADE)])
data class AnalysisEntity(

        @ColumnInfo(name = "eid")
        var eid: Int,

        @ColumnInfo(name = "aid")
        @PrimaryKey(autoGenerate = true)
        var aid: Int? = null)
