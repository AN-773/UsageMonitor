package sau.odev.usagemonitor.ui

import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.groups.Group

class AppsAdapter(
    private val packageManager: PackageManager,
    private val onEditClick: (App) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    private var apps = listOf<App>()
    private var groups = mapOf<Long, Group>()

    fun updateData(newApps: List<App>, groupsList: List<Group>) {
        apps = newApps
        groups = groupsList.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        Log.d("onCreateViewHolder", "onCreateViewHolder: xxxxx")
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
        if (position == apps.size - 1) {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 150 // last item bottom margin
            holder.itemView.layoutParams = params
        } else {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 10 // other items bottom margin
            holder.itemView.layoutParams = params
        }
    }

    override fun getItemCount() = apps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        private val appPackage: TextView = itemView.findViewById(R.id.app_package)
        private val maxUsageValue: TextView = itemView.findViewById(R.id.max_usage_value)
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val editButton: Button = itemView.findViewById(R.id.edit_button)

        fun bind(app: App) {
            appName.text = app.name
            appPackage.text = app.packageName

            // Load app icon
            try {
                val icon = packageManager.getApplicationIcon(app.packageName)
                appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                appIcon.setImageResource(R.mipmap.ic_launcher)
            }

            // Format max usage
            maxUsageValue.text = if (app.maxUsagePerDayInSeconds > 0) {
                formatSeconds(app.maxUsagePerDayInSeconds)
            } else {
                "No limit"
            }

            // Display group
            val group = groups[app.groupId]
            groupName.text = if (group != null) {
                "Group: ${group.name}"
            } else {
                "Group: None"
            }

            editButton.setOnClickListener {
                onEditClick(app)
            }
        }

        private fun formatSeconds(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hours > 0 -> "$hours h $minutes m"
                minutes > 0 -> "$minutes m $secs s"
                else -> "$secs s"
            }
        }
    }
}

