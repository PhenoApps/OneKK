package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

/**
 * This table tracks the images saved during reference detection, kernel clustering, and counting algorithms.
 */

@Keep
@Entity(tableName = "images")
data class ImageEntity(

        /**
         * Image column is an embedded object contains a String encoded url and a date.
         */
        @Embedded
        var image: Image?,

        @ColumnInfo(name = "iid")
        @PrimaryKey
        var iid: Int)
