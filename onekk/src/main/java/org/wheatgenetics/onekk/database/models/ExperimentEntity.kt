package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wheatgenetics.onekk.database.models.embedded.Experiment

@Keep
@Entity(tableName = "experiments")
data class ExperimentEntity(

        @Embedded
        var experiment: Experiment?,

        @ColumnInfo(name = "eid")
        @PrimaryKey
        var eid: Int)
