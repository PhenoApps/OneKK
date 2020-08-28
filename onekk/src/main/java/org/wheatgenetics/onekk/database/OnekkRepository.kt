package org.wheatgenetics.onekk.database

import org.wheatgenetics.onekk.database.dao.CoinDao
import org.wheatgenetics.onekk.database.dao.OnekkDao

class OnekkRepository
    private constructor(
            private val dao: OnekkDao, private val coinDao: CoinDao) {

    companion object {

        @Volatile private var instance: OnekkRepository? = null

        fun getInstance(onekk: OnekkDao, coinDao: CoinDao) =
                instance ?: synchronized(this) {
                    instance ?: OnekkRepository(onekk, coinDao)
                        .also { instance = it }
                }
    }
}