package sau.odev.usagemonitor

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.ui.AppUsageAdapter
import sau.odev.usagemonitor.ui.AppUsageData
import sau.odev.usagemonitor.ui.GroupUsageAdapter
import sau.odev.usagemonitor.ui.GroupUsageData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class DashboardActivity : AppCompatActivity() {

    private lateinit var totalScreenTimeText: TextView
    private lateinit var screenSessionsCountText: TextView
    private lateinit var datePickerButton: Button
    private lateinit var usageTabs: TabLayout
    private lateinit var usageViewPager: ViewPager2

    private lateinit var groupUsageAdapter: GroupUsageAdapter
    private lateinit var appUsageAdapter: AppUsageAdapter

    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        supportActionBar?.title = "Dashboard"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        database = UsageDatabase.getInstance(applicationContext)

        initializeViews()
        setupAdapters()
        setupListeners()

        loadDashboardData()
    }

    private fun initializeViews() {
        totalScreenTimeText = findViewById(R.id.total_screen_time)
        screenSessionsCountText = findViewById(R.id.screen_sessions_count)
        datePickerButton = findViewById(R.id.date_picker_button)
        usageTabs = findViewById(R.id.usage_tabs)
        usageViewPager = findViewById(R.id.usage_viewpager)
    }

    private fun setupAdapters() {
        groupUsageAdapter = GroupUsageAdapter()
        appUsageAdapter = AppUsageAdapter(packageManager)

        // Setup ViewPager with adapter
        usageViewPager.adapter = UsagePagerAdapter(this)

        // Link TabLayout with ViewPager
        TabLayoutMediator(usageTabs, usageViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.usage_by_app)
                1 -> getString(R.string.usage_by_group)
                else -> ""
            }
        }.attach()
    }

    // ViewPager Adapter
    inner class UsagePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AppUsageFragment.newInstance()
                1 -> GroupUsageFragment.newInstance()
                else -> AppUsageFragment.newInstance()
            }
        }
    }

    // Helper method to get adapter for a specific position
    fun getAdapterForPosition(position: Int): RecyclerView.Adapter<*> {
        return when (position) {
            0 -> appUsageAdapter
            1 -> groupUsageAdapter
            else -> appUsageAdapter
        }
    }

    // Group Usage Fragment
    class GroupUsageFragment : Fragment() {

        companion object {
            fun newInstance() = GroupUsageFragment()
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return inflater.inflate(R.layout.fragment_usage_list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Set adapter from parent activity
            (activity as? DashboardActivity)?.let { dashboardActivity ->
                recyclerView.adapter = dashboardActivity.groupUsageAdapter
            }
        }
    }

    // App Usage Fragment
    class AppUsageFragment : Fragment() {

        companion object {
            fun newInstance() = AppUsageFragment()
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            return inflater.inflate(R.layout.fragment_usage_list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Set adapter from parent activity
            (activity as? DashboardActivity)?.let { dashboardActivity ->
                recyclerView.adapter = dashboardActivity.appUsageAdapter
            }
        }
    }

    private fun setupListeners() {
        datePickerButton.setOnClickListener {
            showDatePicker()
        }

        updateDateButtonText()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateButtonText()
                loadDashboardData()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateButtonText() {
        val today = Calendar.getInstance()
        if (isSameDay(selectedDate, today)) {
            datePickerButton.text = getString(R.string.today)
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            datePickerButton.text = sdf.format(selectedDate.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun loadDashboardData() {
        executor.execute {
            try {
                // Get start and end of selected day
                val startOfDay = Calendar.getInstance().apply {
                    timeInMillis = selectedDate.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = selectedDate.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // Get screen sessions
                val screenSessions = database.getScreenSessionDao().getScreenSessions(startOfDay, endOfDay)
                val totalScreenTime = screenSessions.sumOf { it.usageDuration }
                val sessionCount = screenSessions.size

                // Get apps and groups
                val apps = database.getAppsDao().getAllApps()
                val groups = database.getGroupsDao().getGroups()
                val appsMap = apps.associateBy { it.id }
                val groupsMap = groups.associateBy { it.id }

                // Get app sessions
                val appSessions = database.getSessionDao().getAppsUsageOrderByUsageTime(startOfDay, endOfDay)

                // Calculate app usage data
                val appUsageDataList = appSessions.mapNotNull { session ->
                    val app = appsMap[session.appId]
                    app?.let {
                        // Count sessions for this app
                        val sessions = database.getSessionDao().getAppSessions(app.id, startOfDay, endOfDay)
                        AppUsageData(
                            appId = app.id,
                            appName = app.name,
                            packageName = app.packageName,
                            category = app.category,
                            usageDurationSeconds = session.usageDuration / 1000,
                            maxUsageSeconds = app.maxUsagePerDayInSeconds,
                            sessionCount = sessions.size
                        )
                    }
                }

                // Get group sessions
                val groupSessions = database.getSessionDao().getGroupsUsageOrderByUsageTime(startOfDay, endOfDay)

                // Calculate group usage data
                val groupUsageDataList = groupSessions.mapNotNull { session ->
                    val group = groupsMap[session.groupId]
                    group?.let {
                        GroupUsageData(
                            groupId = group.id,
                            groupName = group.name,
                            groupIcon = group.icon,
                            usageDurationSeconds = session.usageDuration / 1000,
                            maxUsageSeconds = group.maxUsagePerDayInSeconds,
                            totalUsageSeconds = totalScreenTime
                        )
                    }
                }

                runOnUiThread {
                    // Update screen time
                    totalScreenTimeText.text = formatDuration(totalScreenTime)
                    screenSessionsCountText.text = "$sessionCount sessions"

                    // Debug: Log the data sizes
                    android.util.Log.d("DashboardActivity", "Group usage items: ${groupUsageDataList.size}")
                    android.util.Log.d("DashboardActivity", "App usage items: ${appUsageDataList.size}")

                    // Update adapters with data - fragments will automatically display the data
                    appUsageAdapter.updateData(appUsageDataList)
                    groupUsageAdapter.updateData(groupUsageDataList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    totalScreenTimeText.text = "Error loading data"
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            seconds > 0 -> "${seconds}s"
            else -> "0m"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

