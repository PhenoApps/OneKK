package org.wheatgenetics.onekk.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.Room
import androidx.room.RoomDatabase
import org.wheatgenetics.onekk.DB_NAME
import org.wheatgenetics.onekk.database.dao.CoinDao
import org.wheatgenetics.onekk.database.dao.OnekkDao
import org.wheatgenetics.onekk.database.models.*
import java.io.File


@Database(entities = [AnalysisEntity::class, ImageEntity::class,
    CoinEntity::class, ContourEntity::class],
        views = [], version = 1)
abstract class OnekkDatabase : RoomDatabase() {

    private fun ifExists(path: String): Boolean {

        val db = File(path)

        if (db.exists()) {

            return true

        }

        db.parent?.let { parent ->

            val dir = File(parent)

            if (!dir.exists()) {

                dir.mkdirs()

            }

        }

        return false
    }

    /**
     * The database mStringust enable foreign keys using pragma to cascade deletes.
     */
    override fun init(configuration: DatabaseConfiguration) {

        val path = configuration.context.getDatabasePath(DB_NAME).path

        if (ifExists(path)) {

            SQLiteDatabase.openDatabase(configuration.context
                    .getDatabasePath(DB_NAME).path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE)?.let { db ->

                db.rawQuery("PRAGMA foreign_keys=ON;", null).close()

                db.close()
            }

        }

        super.init(configuration)
    }

    abstract fun dao(): OnekkDao
    abstract fun coinDao(): CoinDao

    companion object {

        //singleton pattern
        @Volatile private var instance: OnekkDatabase? = null

        fun getInstance(ctx: Context): OnekkDatabase {

            return instance ?: synchronized(this) {

                instance ?: buildDatabase(ctx).also { instance = it }
            }
        }

        private fun buildDatabase(ctx: Context): OnekkDatabase {

            return Room.databaseBuilder(ctx, OnekkDatabase::class.java, DB_NAME)
                    .build()

        }
    }
}