package sau.odev.usagemonitor.appusagemanager.groups

import androidx.room.*

@Dao
interface GroupsDao {
    @Insert
    fun addGroup(group: Group): Long

    @Query("SELECT * FROM groups")
    fun getGroups(): List<Group>

    @Update
    fun updateGroup(group: Group)

    @Delete
    fun deleteGroup(group: Group)

}