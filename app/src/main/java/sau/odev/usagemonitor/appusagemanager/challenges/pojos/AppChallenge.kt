package sau.odev.usagemonitor.appusagemanager.challenges.pojos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import khronos.Dates
import sau.odev.usagemonitor.appusagemanager.apps.App

@Entity(
    tableName = "apps_challenges",
    foreignKeys = [
        ForeignKey(
            entity = App::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChallengeID::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )]
)
class AppChallenge(
    @ColumnInfo(name = "app_id") val appId: Long,
    val type: Int,
    val value: Long,
    createdDay: Long = Dates.today.time,
    createdTime: Long = System.currentTimeMillis(),
    state: Int = STATE_ACTIVE
) : Challenge(createdDay, createdTime, state) {

    //constructor(uid: Long, appId: Long, type: Int, value: Long) : this(appId, type, value, uid)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppChallenge) return false

        if (createdDay != other.createdDay) return false
        if (createdTime != other.createdTime) return false
        if (appId != other.appId) return false
        if (type != other.type) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + appId.hashCode()
        result = 31 * result + type
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "${super.toString()}\n\tAppChallenge(appId=$appId, type=$type, value=$value)"
    }

    companion object {
        const val TYPE_LIMIT = 1
        const val TYPE_FAST = 2
    }


}