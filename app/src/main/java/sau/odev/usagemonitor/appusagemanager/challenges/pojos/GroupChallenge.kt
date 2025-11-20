package sau.odev.usagemonitor.appusagemanager.challenges.pojos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import khronos.Dates
import sau.odev.usagemonitor.appusagemanager.groups.Group

@Entity(
    tableName = "groups_challenges",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        ), ForeignKey(
            entity = ChallengeID::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )]
)

class GroupChallenge(
    @ColumnInfo(name = "group_id") val groupId: Long,
    val type: Int,
    val value: Long,
    createdDay: Long = Dates.today.time,
    createdTime: Long = System.currentTimeMillis(),
    state: Int = STATE_ACTIVE
) : Challenge(createdDay, createdTime, state) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupChallenge) return false

        if (createdDay != other.createdDay) return false
        if (createdTime != other.createdTime) return false
        if (groupId != other.groupId) return false
        if (type != other.type) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + type
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        return "${super.toString()}\n\tGroupChallenge(groupId=$groupId, type=$type, value=$value)"
    }

    companion object {
        const val TYPE_LIMIT = 1
        const val TYPE_FAST = 2
    }

}