package sau.odev.usagemonitor.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R

data class AppUsageData(
    val appId: Long,
    val appName: String,
    val packageName: String,
    val category: String,
    val usageDurationSeconds: Long,
    val maxUsageSeconds: Int,
    val sessionCount: Int
)

class AppUsageAdapter(
    private val packageManager: PackageManager,
    private val onItemClick: ((Long) -> Unit)? = null
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var appUsageList = listOf<AppUsageData>()

    fun updateData(newData: List<AppUsageData>) {
        appUsageList = newData.sortedByDescending { it.usageDurationSeconds }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return AppUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(appUsageList[position])
    }

    override fun getItemCount() = appUsageList.size

    inner class AppUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        private val appCategory: TextView = itemView.findViewById(R.id.app_category)
        private val sessionCount: TextView = itemView.findViewById(R.id.session_count)
        private val usageDuration: TextView = itemView.findViewById(R.id.usage_duration)
        private val maxUsageIndicator: TextView = itemView.findViewById(R.id.max_usage_indicator)

        fun bind(data: AppUsageData) {
            appName.text = data.appName
            appCategory.text = data.category
            sessionCount.text = "${data.sessionCount} sessions"
            usageDuration.text = formatDuration(data.usageDurationSeconds)

            // Set click listener
            itemView.setOnClickListener {
                onItemClick?.invoke(data.appId)
            }

            // Load app icon
            try {
                val icon = packageManager.getApplicationIcon(data.packageName)
                appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIcon.setImageResource(R.mipmap.ic_launcher)
            }

            // Show max usage if set
            if (data.maxUsageSeconds > 0) {
                maxUsageIndicator.text = "of ${formatDuration(data.maxUsageSeconds.toLong())}"
                maxUsageIndicator.visibility = View.VISIBLE

                // Change color if over limit
                if (data.usageDurationSeconds > data.maxUsageSeconds) {
                    usageDuration.setTextColor(
                        itemView.context.getColor(android.R.color.holo_red_dark)
                    )
                } else if (data.usageDurationSeconds > data.maxUsageSeconds * 0.8) {
                    usageDuration.setTextColor(
                        itemView.context.getColor(android.R.color.holo_orange_dark)
                    )
                } else {
                    usageDuration.setTextColor(
                        itemView.context.getColor(android.R.color.holo_blue_dark)
                    )
                }
            } else {
                maxUsageIndicator.visibility = View.GONE
                usageDuration.setTextColor(
                    itemView.context.getColor(android.R.color.holo_blue_dark)
                )
            }
        }

        private fun formatDuration(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "${secs}s"
            }
        }
    }
}

