package sau.odev.usagemonitor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.Challenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.Group
import java.text.SimpleDateFormat
import java.util.*

sealed class ChallengeItem {
    data class AppChallengeItem(val challenge: AppChallenge, val app: App?) : ChallengeItem()
    data class GroupChallengeItem(val challenge: GroupChallenge, val group: Group?) : ChallengeItem()
    data class DeviceFastItem(val challenge: DeviceFastChallenge) : ChallengeItem()
}

class ChallengesAdapter : RecyclerView.Adapter<ChallengesAdapter.ChallengeViewHolder>() {

    private var challenges = listOf<ChallengeItem>()

    fun updateData(
        appChallenges: List<AppChallenge>,
        groupChallenges: List<GroupChallenge>,
        deviceFastChallenges: List<DeviceFastChallenge>,
        apps: List<App>,
        groups: List<Group>
    ) {
        val appMap = apps.associateBy { it.id }
        val groupMap = groups.associateBy { it.id }

        val items = mutableListOf<ChallengeItem>()
        items.addAll(appChallenges.map { ChallengeItem.AppChallengeItem(it, appMap[it.appId]) })
        items.addAll(groupChallenges.map { ChallengeItem.GroupChallengeItem(it, groupMap[it.groupId]) })
        items.addAll(deviceFastChallenges.map { ChallengeItem.DeviceFastItem(it) })

        challenges = items.sortedByDescending {
            when (it) {
                is ChallengeItem.AppChallengeItem -> it.challenge.createdTime
                is ChallengeItem.GroupChallengeItem -> it.challenge.createdTime
                is ChallengeItem.DeviceFastItem -> it.challenge.createdTime
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(challenges[position])
    }

    override fun getItemCount() = challenges.size

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val challengeTitle: TextView = itemView.findViewById(R.id.challenge_title)
        private val challengeType: TextView = itemView.findViewById(R.id.challenge_type)
        private val challengeValue: TextView = itemView.findViewById(R.id.challenge_value)
        private val challengeCreated: TextView = itemView.findViewById(R.id.challenge_created)
        private val challengeState: TextView = itemView.findViewById(R.id.challenge_state)

        fun bind(item: ChallengeItem) {
            when (item) {
                is ChallengeItem.AppChallengeItem -> bindAppChallenge(item)
                is ChallengeItem.GroupChallengeItem -> bindGroupChallenge(item)
                is ChallengeItem.DeviceFastItem -> bindDeviceFast(item)
            }
        }

        private fun bindAppChallenge(item: ChallengeItem.AppChallengeItem) {
            val challenge = item.challenge
            val app = item.app

            challengeTitle.text = app?.name ?: "Unknown App"

            val typeStr = when (challenge.type) {
                AppChallenge.TYPE_LIMIT -> "Limit"
                AppChallenge.TYPE_FAST -> "Fast"
                else -> "Unknown"
            }
            challengeType.text = "App Challenge - $typeStr"

            challengeValue.text = if (challenge.type == AppChallenge.TYPE_LIMIT) {
                "Max: ${formatSeconds(challenge.value.toInt())}"
            } else {
                "No usage"
            }

            challengeCreated.text = "Created: ${formatDate(challenge.createdTime)}"
            updateStateText(challenge)
        }

        private fun bindGroupChallenge(item: ChallengeItem.GroupChallengeItem) {
            val challenge = item.challenge
            val group = item.group

            challengeTitle.text = group?.name ?: "Unknown Group"

            val typeStr = when (challenge.type) {
                GroupChallenge.TYPE_LIMIT -> "Limit"
                GroupChallenge.TYPE_FAST -> "Fast"
                else -> "Unknown"
            }
            challengeType.text = "Group Challenge - $typeStr"

            challengeValue.text = if (challenge.type == GroupChallenge.TYPE_LIMIT) {
                "Max: ${formatSeconds(challenge.value.toInt())}"
            } else {
                "No usage"
            }

            challengeCreated.text = "Created: ${formatDate(challenge.createdTime)}"
            updateStateText(challenge)
        }

        private fun bindDeviceFast(item: ChallengeItem.DeviceFastItem) {
            val challenge = item.challenge

            challengeTitle.text = "Device Fast"
            challengeType.text = "Device Challenge"
            challengeValue.text = "Block until: ${formatDate(challenge.blockUntil)}"
            challengeCreated.text = "Created: ${formatDate(challenge.createdTime)}"
            updateStateText(challenge)
        }

        private fun updateStateText(challenge: Challenge) {
            val (stateText, color) = when (challenge.state) {
                Challenge.STATE_ACTIVE -> Pair("Active", android.R.color.holo_green_dark)
                Challenge.STATE_FAILED -> Pair("Failed", android.R.color.holo_red_dark)
                Challenge.STATE_COMPLETE -> Pair("Complete", android.R.color.holo_blue_dark)
                else -> Pair("Unknown", android.R.color.darker_gray)
            }
            challengeState.text = stateText
            challengeState.setTextColor(ContextCompat.getColor(itemView.context, color))
        }

        private fun formatSeconds(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

            return when {
                hours > 0 -> "$hours hours $minutes min"
                minutes > 0 -> "$minutes minutes"
                else -> "$seconds seconds"
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}

