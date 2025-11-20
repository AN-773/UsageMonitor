package sau.odev.usagemonitor.appusagemanager.challenges.pojos

import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.concurrent.atomic.AtomicLong


open class Challenge(@ColumnInfo(name = "created_day") val createdDay: Long,
                     @ColumnInfo(name = "created_time") val createdTime: Long,
                     @get:Synchronized @set:Synchronized
                     var state: Int = STATE_ACTIVE,
                     @PrimaryKey()  var id: Long = 0L
) {


    override fun toString(): String {
        return "Challenge(createdDay=$createdDay, createdTime=$createdTime, state=$state)"
    }

    companion object {
        val lastUID = AtomicLong(1)
        const val STATE_ACTIVE = 0
        const val STATE_FAILED = 1
        const val STATE_COMPLETE = 2
    }
}