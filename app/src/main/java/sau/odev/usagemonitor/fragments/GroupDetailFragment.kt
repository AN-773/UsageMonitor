package sau.odev.usagemonitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.ui.GroupAppUsageAdapter
import sau.odev.usagemonitor.ui.AppUsageData
import java.util.*
import java.util.concurrent.Executors

class GroupDetailFragment : Fragment() {

    private lateinit var groupIcon: ImageView
    private lateinit var groupName: TextView
    private lateinit var totalUsageText: TextView
    private lateinit var appCountText: TextView
    private lateinit var maxUsageText: TextView
    private lateinit var usageStatusText: TextView
    private lateinit var usageProgress: ProgressBar
    private lateinit var appsRecyclerView: RecyclerView
    private lateinit var groupAppUsageAdapter: GroupAppUsageAdapter

    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    private var groupId: Long = -1

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: Long): GroupDetailFragment {
            val fragment = GroupDetailFragment()
            val args = Bundle()
            args.putLong(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getLong(ARG_GROUP_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        initializeViews(view)
        setupRecyclerView()
        loadGroupDetails()
    }

    private fun initializeViews(view: View) {
        groupIcon = view.findViewById(R.id.group_icon)
        groupName = view.findViewById(R.id.group_name)
        totalUsageText = view.findViewById(R.id.total_usage_text)
        appCountText = view.findViewById(R.id.app_count_text)
        maxUsageText = view.findViewById(R.id.max_usage_text)
        usageStatusText = view.findViewById(R.id.usage_status_text)
        usageProgress = view.findViewById(R.id.usage_progress)
        appsRecyclerView = view.findViewById(R.id.apps_recycler_view)
    }

    private fun setupRecyclerView() {
        groupAppUsageAdapter = GroupAppUsageAdapter(requireActivity().packageManager)
        appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        appsRecyclerView.adapter = groupAppUsageAdapter
    }

    private fun loadGroupDetails() {
        executor.execute {
            try {
                val group = AppsUsageManager.getInstance().getCreatedGroups().getGroup(groupId)
                if (group == null) {
                    activity?.runOnUiThread {
                        // Handle group not found
                    }
                    return@execute
                }

                // Get today's data
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // Get all apps in this group
                val appsInGroup = database.getAppsDao().getAllApps().filter { it.groupId == groupId }
                val appsMap = appsInGroup.associateBy { it.id }

                // Get usage for each app in the group
                val appUsageList = mutableListOf<AppUsageData>()
                var totalGroupUsage = 0L

                appsInGroup.forEach { app ->
                    val sessions = database.getSessionDao().getAppSessions(app.id, startOfDay, endOfDay)
                    val appUsage = sessions.sumOf { it.usageDuration }
                    totalGroupUsage += appUsage

                    if (appUsage > 0) {
                        appUsageList.add(
                            AppUsageData(
                                appId = app.id,
                                appName = app.name,
                                packageName = app.packageName,
                                category = app.category,
                                usageDurationSeconds = appUsage / 1000,
                                maxUsageSeconds = app.maxUsagePerDayInSeconds,
                                sessionCount = sessions.size
                            )
                        )
                    }
                }

                activity?.runOnUiThread {
                    groupName.text = group.name

                    // Set group icon
                    val iconRes = getIconResource(group.icon)
                    groupIcon.setImageResource(iconRes)

                    totalUsageText.text = formatDuration(totalGroupUsage)
                    appCountText.text = "${appsInGroup.size} apps"

                    if (group.maxUsagePerDayInSeconds > 0) {
                        maxUsageText.text = formatDuration(group.maxUsagePerDayInSeconds.toLong() * 1000)

                        val usageSeconds = totalGroupUsage / 1000
                        val percentage = (usageSeconds.toFloat() / group.maxUsagePerDayInSeconds * 100).toInt()

                        usageProgress.progress = percentage.coerceIn(0, 100)

                        when {
                            percentage > 100 -> {
                                usageStatusText.text = "Over limit by ${formatDuration(totalGroupUsage - group.maxUsagePerDayInSeconds * 1000L)}"
                                usageStatusText.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                                usageProgress.progressTintList = android.content.res.ColorStateList.valueOf(
                                    requireContext().getColor(android.R.color.holo_red_dark)
                                )
                            }
                            percentage > 80 -> {
                                usageStatusText.text = "$percentage% of daily limit"
                                usageStatusText.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                                usageProgress.progressTintList = android.content.res.ColorStateList.valueOf(
                                    requireContext().getColor(android.R.color.holo_orange_dark)
                                )
                            }
                            else -> {
                                usageStatusText.text = "$percentage% of daily limit"
                                usageStatusText.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                                usageProgress.progressTintList = android.content.res.ColorStateList.valueOf(
                                    requireContext().getColor(android.R.color.holo_green_dark)
                                )
                            }
                        }
                    } else {
                        maxUsageText.text = "No limit set"
                        usageStatusText.text = "No restrictions"
                        usageProgress.progress = 0
                    }

                    // Update apps list
                    groupAppUsageAdapter.updateData(appUsageList.sortedByDescending { it.usageDurationSeconds })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
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

