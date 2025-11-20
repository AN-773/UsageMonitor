package sau.odev.usagemonitor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R

data class GroupUsageData(
    val groupId: Long,
    val groupName: String,
    val groupIcon: Int,
    val usageDurationSeconds: Long,
    val maxUsageSeconds: Int,
    val totalUsageSeconds: Long
)

class GroupUsageAdapter(
    private val onItemClick: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<GroupUsageAdapter.GroupUsageViewHolder>() {

    private var groupUsageList = listOf<GroupUsageData>()

    fun updateData(newData: List<GroupUsageData>) {
        groupUsageList = newData.sortedByDescending { it.usageDurationSeconds }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_usage, parent, false)
        return GroupUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupUsageViewHolder, position: Int) {
        holder.bind(groupUsageList[position])
    }

    override fun getItemCount() = groupUsageList.size

    inner class GroupUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupIcon: ImageView = itemView.findViewById(R.id.group_icon)
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val usageDuration: TextView = itemView.findViewById(R.id.usage_duration)
        private val usagePercentage: TextView = itemView.findViewById(R.id.usage_percentage)
        private val usageProgress: ProgressBar = itemView.findViewById(R.id.usage_progress)
        private val maxUsageInfo: TextView = itemView.findViewById(R.id.max_usage_info)

        fun bind(data: GroupUsageData) {
            groupName.text = data.groupName

            // Set click listener
            itemView.setOnClickListener {
                onItemClick?.invoke(data.groupId)
            }

            // Set icon
            val iconRes = getIconResource(data.groupIcon)
            groupIcon.setImageResource(iconRes)

            // Format and display usage duration
            usageDuration.text = formatDuration(data.usageDurationSeconds)

            // Calculate percentage of total usage
            val percentage = if (data.totalUsageSeconds > 0) {
                ((data.usageDurationSeconds.toFloat() / data.totalUsageSeconds.toFloat()) * 100).toInt()
            } else {
                0
            }

            // Set progress bar
            if (data.maxUsageSeconds > 0) {
                val progressPercentage = ((data.usageDurationSeconds.toFloat() / data.maxUsageSeconds.toFloat()) * 100).toInt()
                usageProgress.progress = progressPercentage.coerceIn(0, 100)
                maxUsageInfo.text = "Max: ${formatDuration(data.maxUsageSeconds.toLong())}"
                maxUsageInfo.visibility = View.VISIBLE

                usagePercentage.text = "$progressPercentage%"
                // Change color if over limit
                if (progressPercentage > 100) {
                    usageProgress.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(android.R.color.holo_red_dark)
                        )
                } else if (progressPercentage > 80) {
                    usageProgress.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(android.R.color.holo_orange_dark)
                        )
                } else {
                    usageProgress.progressTintList =
                        android.content.res.ColorStateList.valueOf(
                            itemView.context.getColor(android.R.color.holo_green_dark)
                        )
                }
            } else {
                usageProgress.progress = percentage
                maxUsageInfo.visibility = View.GONE
            }
        }

        private fun getIconResource(icon: Int): Int {
            return when (icon) {
                1 -> R.drawable.ic_group_social_media
                2 -> R.drawable.ic_group_games
                3 -> R.drawable.ic_group_meida
                4 -> R.drawable.ic_group_productivity
                else -> R.drawable.ic_grouop_others
            }
        }

        private fun formatDuration(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${secs}s"
                else -> "${secs}s"
            }
        }
    }
}

