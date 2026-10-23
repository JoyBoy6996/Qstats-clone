package dev.pulsekit.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FpsSessionEntity::class, FpsSampleEntity::class], version = 1, exportSchema = false)
abstract class PulseDatabase : RoomDatabase() {

    abstract fun fps(): FpsDao

    companion object {
        @Volatile private var instance: PulseDatabase? = null

        fun get(context: Context): PulseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, PulseDatabase::class.java, "pulsekit.db"
                ).build().also { instance = it }
            }
    }
}
