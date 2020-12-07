package org.wheatgenetics.onekk.database.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "coin", primaryKeys = ["country", "name"])
data class CoinEntity(

        @ColumnInfo(name = "country")
        var country: String,

        @ColumnInfo(name = "diameter")
        var diameter: String,

        @ColumnInfo(name = "name")
        var name: String)
