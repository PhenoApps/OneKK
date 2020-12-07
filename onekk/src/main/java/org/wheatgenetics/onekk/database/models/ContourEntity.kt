package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Contour
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "contour",
        foreignKeys = [ForeignKey(entity = AnalysisEntity::class,
                parentColumns = ["aid"],
                childColumns = ["aid"],
                onDelete = ForeignKey.CASCADE)])
data class ContourEntity(

        /**
         * Image column is an embedded object contains a String encoded url and a date.
         */
        @Embedded
        var contour: Contour? = null,

        @ColumnInfo(name = "selected")
        var selected: Boolean = true,

        @ColumnInfo(name = "aid")
        var aid: Int,

        @ColumnInfo(name = "cid")
        @PrimaryKey(autoGenerate = true)
        var cid: Int? = null)
