package sau.odev.usagemonitor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.groups.Group

class GroupsAdapter(
    private val onEditClick: (Group) -> Unit,
    private val onDeleteClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    private var groups = listOf<Group>()

    fun updateData(newGroups: List<Group>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
        if (position == groups.size - 1) {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 150 // last item bottom margin
            holder.itemView.layoutParams = params
        } else {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            params.bottomMargin = 10 // other items bottom margin
            holder.itemView.layoutParams = params
        }
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupIcon: ImageView = itemView.findViewById(R.id.group_icon)
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val groupMaxUsage: TextView = itemView.findViewById(R.id.group_max_usage)
        private val editButton: Button = itemView.findViewById(R.id.edit_button)
        private val deleteButton: Button = itemView.findViewById(R.id.delete_button)

        fun bind(group: Group) {
            groupName.text = group.name

            // Set icon based on group icon value
            val iconRes = getIconResource(group.icon)
            groupIcon.setImageResource(iconRes)

            // Format max usage
            groupMaxUsage.text = if (group.maxUsagePerDayInSeconds > 0) {
                "Max usage: ${formatSeconds(group.maxUsagePerDayInSeconds)}/day"
            } else {
                "Max usage: No limit"
            }

            editButton.setOnClickListener {
                onEditClick(group)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(group)
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

        private fun formatSeconds(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60

            return when {
                hours > 0 -> "$hours hours $minutes min"
                minutes > 0 -> "$minutes minutes"
                else -> "$seconds seconds"
            }
        }
    }
}

