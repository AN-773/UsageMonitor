package sau.odev.usagemonitor.appusagemanager.challenges.pojos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges_ids")
data class ChallengeID(@PrimaryKey(autoGenerate = true) internal val id: Long = 0L)