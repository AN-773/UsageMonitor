package sau.odev.usagemonitor.appusagemanager.challenges

import androidx.room.*
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.*


@Dao
abstract class ChallengesDao {

    @Insert
    abstract fun addChallengeID(challengeID: ChallengeID)

    @Insert
    abstract fun addAppChallenge(challenge: AppChallenge): Long

    @Insert
    abstract fun addGroupChallenge(challenge: GroupChallenge): Long

    @Insert
    abstract fun addDeviceFastChallenge(challenge: DeviceFastChallenge): Long

    @Query("SELECT * FROM apps_challenges AS ac ")
    abstract fun getAppsChallenges(): List<AppChallenge>

    @Query("SELECT * FROM apps_challenges AS ac WHERE created_day = :createdDay")
    abstract fun getAppsChallengesByDay(createdDay: Long): List<AppChallenge>

    @Query("SELECT * FROM apps_challenges as ac WHERE created_time >= :from AND created_time < :to")
    abstract fun getAppsChallenges(from: Long, to: Long): List<AppChallenge>

    @Query("SELECT * FROM apps_challenges AS ac WHERE state = :state")
    abstract fun getAppsChallengesByState(state: Int): List<AppChallenge>

    @Query("SELECT * FROM groups_challenges AS gc")
    abstract fun getGroupsChallenges(): List<GroupChallenge>

    @Query("SELECT * FROM groups_challenges AS gc WHERE created_day = :createdDay")
    abstract fun getGroupsChallengesByDay(createdDay: Long): List<GroupChallenge>

    @Query("SELECT * FROM groups_challenges as gc WHERE created_time >= :from AND created_time < :to")
    abstract fun getGroupsChallenges(from: Long, to: Long): List<GroupChallenge>

    @Query("SELECT * FROM groups_challenges AS gc WHERE state = :state")
    abstract fun getGroupsChallengesByState(state: Int): List<GroupChallenge>

    @Query("SELECT * FROM device_fast_challenges AS dc")
    abstract fun getDeviceFastChallenges(): List<DeviceFastChallenge>

    @Query("SELECT * FROM device_fast_challenges AS dc WHERE created_day = :createdDay")
    abstract fun getDeviceFastChallengesByDay(createdDay: Long): List<DeviceFastChallenge>

    @Query("SELECT * FROM device_fast_challenges as dc WHERE created_time >= :from AND created_time < :to")
    abstract fun getDeviceFastChallenges(from: Long, to: Long): List<DeviceFastChallenge>

    @Query("SELECT * FROM device_fast_challenges AS dc WHERE state = :state")
    abstract fun getDeviceFastChallengesByState(state: Int): List<DeviceFastChallenge>

    @Query("SELECT * FROM device_fast_challenges AS dc WHERE state = ${Challenge.STATE_ACTIVE} LIMIT 1")
    abstract fun getActiveDeviceFastChallenge(): DeviceFastChallenge?

    @Update
    abstract fun updateAppChallenge(challenge: AppChallenge)

    @Update
    abstract fun updateGroupChallenge(challenge: GroupChallenge)

    @Update
    abstract fun updateDeviceFastChallenge(challenge: DeviceFastChallenge)

    @Delete
    abstract fun removeAppChallenge(challenge: AppChallenge)

    @Delete
    abstract fun removeGroupChallenge(challenge: GroupChallenge)

    @Delete
    abstract fun removeDeviceFastChallenge(challenge: DeviceFastChallenge)


    @Transaction
    open fun getChallenges(from: Long, to: Long): List<Challenge> {
        val list = ArrayList<Challenge>(getAppsChallenges(from, to))
        list.addAll(getGroupsChallenges(from, to))
        list.addAll(getDeviceFastChallenges(from, to))
        list.sortByDescending { it.createdTime }
        return list
    }


    @Transaction
    open fun getAllChallenges(): List<Challenge> {
        val list = ArrayList<Challenge>(getAppsChallenges())
        list.addAll(getGroupsChallenges())
        list.addAll(getDeviceFastChallenges())
        list.sortByDescending { it.createdTime }
        return list
    }

    @Transaction
    open fun getChallengesByDay(day: Long): List<Challenge> {
        val list = ArrayList<Challenge>(getAppsChallengesByDay(day))
        list.addAll(getGroupsChallengesByDay(day))
        list.addAll(getDeviceFastChallengesByDay(day))
        list.sortByDescending { it.createdTime }
        return list
    }

}