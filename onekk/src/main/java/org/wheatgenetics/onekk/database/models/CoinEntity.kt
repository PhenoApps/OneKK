package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wheatgenetics.onekk.database.models.embedded.Coin
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image

@Keep
@Entity(tableName = "coin")
data class CoinEntity(

        @Embedded
        var coin: Coin?,

        @ColumnInfo(name = "iid")
        @PrimaryKey
        var cid: Int)
