package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OfflineRegionEntity::class, OfflineTileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FictionHikeDatabase : RoomDatabase() {
    abstract fun offlineMapDao(): OfflineMapDao

    companion object {
        @Volatile
        private var INSTANCE: FictionHikeDatabase? = null

        fun getInstance(context: Context): FictionHikeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FictionHikeDatabase::class.java,
                    "fiction_hike_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
