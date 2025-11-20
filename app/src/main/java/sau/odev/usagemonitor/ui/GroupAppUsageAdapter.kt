package sau.odev.usagemonitor.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R

class GroupAppUsageAdapter(
    private val packageManager: PackageManager
) : RecyclerView.Adapter<GroupAppUsageAdapter.GroupAppViewHolder>() {

    private var appUsageList = listOf<AppUsageData>()

    fun updateData(newData: List<AppUsageData>) {
        appUsageList = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_app_usage, parent, false)
        return GroupAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupAppViewHolder, position: Int) {
        holder.bind(appUsageList[position])
    }

    override fun getItemCount() = appUsageList.size

    inner class GroupAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        private val sessionCount: TextView = itemView.findViewById(R.id.session_count)
        private val usageDuration: TextView = itemView.findViewById(R.id.usage_duration)

        fun bind(data: AppUsageData) {
            appName.text = data.appName
            sessionCount.text = "${data.sessionCount} sessions"
            usageDuration.text = formatDuration(data.usageDurationSeconds)

            // Load app icon
            try {
                val icon = packageManager.getApplicationIcon(data.packageName)
                appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIcon.setImageResource(R.mipmap.ic_launcher)
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

