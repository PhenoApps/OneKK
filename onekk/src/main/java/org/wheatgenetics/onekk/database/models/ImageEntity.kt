package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.*
import org.wheatgenetics.onekk.database.models.embedded.Image

/**
 * This table tracks the images saved during reference detection, kernel clustering, and counting algorithms.
 */

@Keep
@Entity(tableName = "image",
        foreignKeys = [ForeignKey(entity = AnalysisEntity::class,
                parentColumns = ["aid"], childColumns = ["aid"], onDelete = ForeignKey.CASCADE)])
data class ImageEntity(

        /**
         * Image column is an embedded object contains a String encoded url and a date.
         */
        @Embedded
        var image: Image?,

        @ColumnInfo(name = "aid")
        var aid: Int,

        @ColumnInfo(name = "iid")
        @PrimaryKey(autoGenerate = true)
        var iid: Int? = null)
