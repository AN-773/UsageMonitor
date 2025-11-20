package sau.odev.usagemonitor.appusagemanager.challenges.pojos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import khronos.Dates


@Entity(
    tableName = "device_fast_challenges", foreignKeys = [ForeignKey(
        entity = ChallengeID::class,
        parentColumns = ["id"],
        childColumns = ["id"],
        onDelete = ForeignKey.CASCADE
    )]
)
class DeviceFastChallenge(
    @ColumnInfo(name = "block_until") val blockUntil: Long,
    createdDay: Long = Dates.today.time,
    createdTime: Long = System.currentTimeMillis(),
    state: Int = STATE_ACTIVE
) : Challenge(createdDay, createdTime, state) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceFastChallenge) return false

        if (createdDay != other.createdDay) return false
        if (createdTime != other.createdTime) return false
        if (blockUntil != other.blockUntil) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + blockUntil.hashCode()
        return result
    }

    override fun toString(): String {
        return "${super.toString()}\n\tDeviceFastChallenge(blockUntil=$blockUntil)"
    }

}
