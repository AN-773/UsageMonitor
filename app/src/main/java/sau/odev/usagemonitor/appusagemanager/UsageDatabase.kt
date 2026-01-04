package sau.odev.usagemonitor.appusagemanager

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.apps.AppsDao
import sau.odev.usagemonitor.appusagemanager.challenges.ChallengesDao
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.ChallengeID
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.appusagemanager.groups.GroupsDao
import sau.odev.usagemonitor.appusagemanager.sessions.BaseSession
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSession
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSessionsDao
import sau.odev.usagemonitor.appusagemanager.sessions.SessionsDao
import sau.odev.usagemonitor.appusagemanager.notifications.NotificationData
import sau.odev.usagemonitor.appusagemanager.notifications.NotificationsDao
import sau.odev.usagemonitor.appusagemanager.usagestats.UsageStatEntity
import sau.odev.usagemonitor.appusagemanager.usagestats.UsageStatsDao

@Database(
    entities = [
        App::class,
        Group::class,
        BaseSession::class,
        ScreenSession::class,
        AppChallenge::class,
        GroupChallenge::class,
        DeviceFastChallenge::class,
        ChallengeID::class,
        NotificationData::class,
        UsageStatEntity::class],
    exportSchema = true, version = 4
)
abstract class UsageDatabase : RoomDatabase() {

    abstract fun getAppsDao(): AppsDao

    abstract fun getGroupsDao(): GroupsDao

    abstract fun getSessionDao(): SessionsDao

    abstract fun getScreenSessionDao(): ScreenSessionsDao

    abstract fun getChallengesDao(): ChallengesDao

    abstract fun getNotificationsDao(): NotificationsDao

    abstract fun getUsageStatsDao(): UsageStatsDao

    companion object {
        private var INSTANCE: UsageDatabase? = null

        @Synchronized
        fun getInstance(context: Context): UsageDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, UsageDatabase::class.java, UsageDatabase::class.java.simpleName)
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT OR ABORT INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (0,'Others',?,0)", arrayOf(R.drawable.ic_grouop_others))
                            db.execSQL("INSERT OR ABORT INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (1,'Games',?,0)", arrayOf(R.drawable.ic_group_games))
                            db.execSQL("INSERT OR ABORT INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (2,'Productivity',?,0)", arrayOf(R.drawable.ic_group_productivity))
                            db.execSQL("INSERT OR ABORT INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (3,'Social media',?,0)", arrayOf(R.drawable.ic_group_social_media))
                            db.execSQL("INSERT OR ABORT INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (4,'Entertainment',?,0)", arrayOf(R.drawable.ic_group_meida))
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            db.execSQL("INSERT OR IGNORE INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (0,'Others',?,0)", arrayOf(R.drawable.ic_grouop_others))
                            db.execSQL("INSERT OR IGNORE INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (1,'Games',?,0)", arrayOf(R.drawable.ic_group_games))
                            db.execSQL("INSERT OR IGNORE INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (2,'Productivity',?,0)", arrayOf(R.drawable.ic_group_productivity))
                            db.execSQL("INSERT OR IGNORE INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (3,'Social media',?,0)", arrayOf(R.drawable.ic_group_social_media))
                            db.execSQL("INSERT OR IGNORE INTO `groups`(`id`,`name`,`icon`,`max_usage_duration`) VALUES (4,'Entertainment',?,0)", arrayOf(R.drawable.ic_group_meida))
                        }

                    }).build()
            }
            return INSTANCE as UsageDatabase
        }
    }
}