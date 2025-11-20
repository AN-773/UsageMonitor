package sau.odev.usagemonitor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.sessions.Session
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter : RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder>() {

    private var sessions = listOf<Session>()

    fun updateData(newData: List<Session>) {
        sessions = newData.sortedByDescending { it.launchTime }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_history, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount() = sessions.size

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sessionTime: TextView = itemView.findViewById(R.id.session_time)
        private val sessionDuration: TextView = itemView.findViewById(R.id.session_duration)

        fun bind(session: Session) {
            val startTime = Date(session.launchTime)
            val endTime = Date(session.launchTime + session.usageDuration)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            sessionTime.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            sessionDuration.text = formatDuration(session.usageDuration)
        }

        private fun formatDuration(ms: Long): String {
            val seconds = ms / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hours > 0 -> "${hours}h ${minutes}m ${secs}s"
                minutes > 0 -> "${minutes}m ${secs}s"
                else -> "${secs}s"
            }
        }
    }
}

