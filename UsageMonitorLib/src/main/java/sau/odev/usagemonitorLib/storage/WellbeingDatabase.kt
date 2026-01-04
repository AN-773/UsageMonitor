package sau.odev.usagemonitorLib.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import sau.odev.usagemonitorLib.storage.dao.NotificationEventsDao
import sau.odev.usagemonitorLib.storage.dao.UsageEventsDao
import sau.odev.usagemonitorLib.storage.entities.NotificationEventEntity
import sau.odev.usagemonitorLib.storage.entities.UsageEventEntity

@Database(
  entities = [
    UsageEventEntity::class,
    NotificationEventEntity::class
  ],
  version = 1,
  exportSchema = true
)
internal abstract class WellbeingDatabase : RoomDatabase() {

  abstract fun usageEventsDao(): UsageEventsDao
  abstract fun notificationEventsDao(): NotificationEventsDao

  companion object {
    @Volatile private var INSTANCE: WellbeingDatabase? = null

    fun get(context: Context): WellbeingDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          WellbeingDatabase::class.java,
          "wellbeingkit.db"
        )
          .fallbackToDestructiveMigration() // v1 only; later we add real migrations
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}