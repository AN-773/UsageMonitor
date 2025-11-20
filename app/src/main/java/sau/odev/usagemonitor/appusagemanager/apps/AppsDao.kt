package sau.odev.usagemonitor.appusagemanager.apps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppsDao {

    @Query("SELECT * FROM apps")
    fun getAllApps(): List<App>

    @Insert
    fun addApp(app: App): Long

    @Update
    fun updateApp(app: App)
}