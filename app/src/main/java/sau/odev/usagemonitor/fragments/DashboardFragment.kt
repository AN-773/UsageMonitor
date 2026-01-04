package sau.odev.usagemonitor.fragments

import android.app.DatePickerDialog
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.ui.AppUsageAdapter
import sau.odev.usagemonitor.ui.AppUsageData
import sau.odev.usagemonitor.ui.GroupUsageAdapter
import sau.odev.usagemonitorLib.WellbeingKit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.Executors
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.CancellationException

class DashboardFragment : Fragment() {

    private lateinit var totalScreenTimeText: TextView
    private lateinit var screenSessionsCountText: TextView
    private lateinit var unlocksCountText: TextView
    private lateinit var notificationsCountText: TextView

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var datePickerButton: Button
    private lateinit var usageTabs: TabLayout
    private lateinit var usageViewPager: ViewPager2
    private lateinit var usagePieChart: PieChart
    private lateinit var loadingOverlay: View

    private lateinit var groupUsageAdapter: GroupUsageAdapter
    private lateinit var appUsageAdapter: AppUsageAdapter

    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getLong(STATE_SELECTED_DATE_MILLIS)?.let { millis ->
            if (millis > 0L) {
                selectedDate = Calendar.getInstance().apply { timeInMillis = millis }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_SELECTED_DATE_MILLIS, selectedDate.timeInMillis)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        initializeViews(view)
        setupAdapters()
        setupListeners()

        loadDashboardData()
    }

    private fun initializeViews(view: View) {
        totalScreenTimeText = view.findViewById(R.id.total_screen_time)
        screenSessionsCountText = view.findViewById(R.id.screen_sessions_count)
        unlocksCountText = view.findViewById(R.id.unlocks_count)
        notificationsCountText = view.findViewById(R.id.notifications_count)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        datePickerButton = view.findViewById(R.id.date_picker_button)
        usageTabs = view.findViewById(R.id.usage_tabs)
        usageViewPager = view.findViewById(R.id.usage_viewpager)
        usagePieChart = view.findViewById(R.id.usage_pie_chart)
        loadingOverlay = view.findViewById(R.id.loading_overlay)

        // Match app theme feel
        swipeRefresh.setColorSchemeResources(R.color.purple_500)

        setupPieChart()
    }

    private fun setupPieChart() {
        usagePieChart.apply {
            setNoDataText("")
            description.isEnabled = false

            // Enable entry labels (app names beside sections)
            setDrawEntryLabels(true)

            // Set label color based on theme (white for night mode, black for light mode)
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val labelColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                Color.WHITE
            } else {
                Color.BLACK
            }
            setEntryLabelColor(labelColor)
            setEntryLabelTextSize(11f)

            // Disable legend since we're using labels
            legend.isEnabled = false

            // Configure hole for center text
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 85f
            transparentCircleRadius = 80f
            setDrawCenterText(false) // We use overlay TextView instead

            setUsePercentValues(false)
            isRotationEnabled = false

            setExtraOffsets(20f, 5f, 20f, 0f)

            // Enable touch for click events
            setTouchEnabled(true)
            setClickable(true)

            // Set click listener
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e is PieEntry) {
                        val appName = e.label
                        val appId = appIdMap[appName]

                        // Navigate to app detail if it's not "Other"
                        if (appId != null) {
                            navigateToAppDetail(appId)
                        }
                    }
                }

                override fun onNothingSelected() {
                    // Do nothing
                }
            })
        }
    }

    private fun setupAdapters() {
        groupUsageAdapter = GroupUsageAdapter { groupId ->
            navigateToGroupDetail(groupId)
        }
        appUsageAdapter = AppUsageAdapter(requireActivity().packageManager) { appId ->
            navigateToAppDetail(appId)
        }

        usageViewPager.adapter = UsagePagerAdapter(this)

        TabLayoutMediator(usageTabs, usageViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.apps)
                else -> ""
            }
        }.attach()
    }

    class UsagePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AppUsageSubFragment.newInstance()
                1 -> GroupUsageSubFragment.newInstance()
                else -> AppUsageSubFragment.newInstance()
            }
        }
    }

    class GroupUsageSubFragment : Fragment() {
        companion object {
            fun newInstance() = GroupUsageSubFragment()
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.fragment_usage_list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            (parentFragment as? DashboardFragment)?.let { dashboardFragment ->
                recyclerView.adapter = dashboardFragment.groupUsageAdapter
            }
        }
    }

    class AppUsageSubFragment : Fragment() {
        companion object {
            fun newInstance() = AppUsageSubFragment()
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.fragment_usage_list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            (parentFragment as? DashboardFragment)?.let { dashboardFragment ->
                recyclerView.adapter = dashboardFragment.appUsageAdapter
            }
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
        datePickerButton.setOnClickListener { showDatePicker() }
        updateDateButtonText()
    }

    private fun showDatePicker() {
        val today = Calendar.getInstance()

        // Fetch minDate asynchronously (DB query) before showing the picker.
        lifecycleScope.launch {
            val minDateMillis = try {
                withContext(Dispatchers.IO) {
                    val oldest = WellbeingKit.getOldestUsageTimestampMsOrNull()
                    oldest?.let { toStartOfDayMillis(it) }
                }
            } catch (_: CancellationException) {
                null
            } catch (_: Exception) {
                null
            }

            if (!isAdded) return@launch

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        // Normalize time component
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // Guard against future dates.
                    if (picked.after(today)) return@DatePickerDialog

                    // Guard against dates earlier than our oldest stored day.
                    if (minDateMillis != null && picked.timeInMillis < minDateMillis) {
                        picked.timeInMillis = minDateMillis
                    }

                    selectedDate = picked
                    updateDateButtonText()
                    loadDashboardData()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )

            // Prevent selecting dates in the future.
            datePickerDialog.datePicker.maxDate = today.timeInMillis

            // Prevent selecting dates older than the stored dataset (when known).
            minDateMillis?.let { datePickerDialog.datePicker.minDate = it }

            datePickerDialog.show()
        }
    }

    private fun toStartOfDayMillis(epochMillis: Long): Long {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return localDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun updateDateButtonText() {
        val today = Calendar.getInstance()
        datePickerButton.text = if (isSameDay(selectedDate, today)) {
            getString(R.string.today)
        } else {
            // Locale-aware date formatting
            DateFormat.getMediumDateFormat(requireContext()).format(selectedDate.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun selectedLocalDate(): LocalDate {
        return selectedDate
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun selectedDayRangeMillis(): Pair<Long, Long> {
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

        return startOfDay to endOfDay
    }

    private fun loadDashboardData() {
        // If a full-screen load is already active, avoid double spinners.
        if (loadingOverlay.visibility == View.VISIBLE) {
            swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                loadingOverlay.visibility = View.VISIBLE
            }

            val selectedDay = selectedLocalDate()

            val (_, appUsageDataList) = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                while (!WellbeingKit.hasUsageStatsAccess(this@DashboardFragment.context!!)) {
                    // Wait until access is granted
                    Thread.sleep(500)
                }
                // Only incremental sync for "today".
                if (isSameDay(selectedDate, Calendar.getInstance())) {
                    WellbeingKit.syncUsageIncremental(now)
                }

                val apps = database.getAppsDao().getAllApps()
                val ignoredApps = apps.filter { it.ignored }.map { it.packageName }.toSet()

                val appsDailyUsage = WellbeingKit.getDailyPackageStats(selectedDay).filter {
                    it.packageName !in ignoredApps
                }

                val appUsageDataList = apps.mapNotNull { app ->
                    if (!app.installed) return@mapNotNull null
                    val session = appsDailyUsage.find { it.packageName == app.packageName }

                    AppUsageData(
                        appId = app.id,
                        appName = app.name,
                        packageName = app.packageName,
                        category = app.category,
                        usageDurationSeconds = (session?.usageTimeMs ?: 0) / 1000,
                        maxUsageSeconds = app.maxUsagePerDayInSeconds,
                        sessionCount = session?.launchCount ?: 0
                    )
                }.sortedByDescending { it.usageDurationSeconds }

                Pair(Unit, appUsageDataList)
            }

            withContext(Dispatchers.Main) {
                appUsageAdapter.updateData(appUsageDataList)
                updatePieChart(appUsageDataList)
                loadingOverlay.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        executor.execute {
            try {
                val (startOfDay, endOfDay) = selectedDayRangeMillis()

                val screenSessions =
                    database.getScreenSessionDao().getScreenSessions(startOfDay, endOfDay)
                val totalScreenTime = if (screenSessions.isNotEmpty())  screenSessions.sumOf { it.usageDuration } else -1L
                val sessionCount = screenSessions.size

                // Calculate total notifications count for the day
                val totalNotificationsCount = try {
                    val notificationsDao = database.getNotificationsDao()
                    val allNotifications =
                        notificationsDao.getNotificationsInTimeRange(startOfDay, endOfDay)
                    allNotifications.size
                } catch (_: Exception) {
                    0 // If notification tracking not available yet
                }

                activity?.runOnUiThread {
                    totalScreenTimeText.text = if (totalScreenTime != -1L) formatDuration(totalScreenTime) else "No data"

                    screenSessionsCountText.text = if (isSameDay(selectedDate, Calendar.getInstance())) {
                        getString(R.string.today)
                    } else {
                        DateFormat.getMediumDateFormat(requireContext()).format(selectedDate.time)
                    }

                    // Update unlocks count (screen sessions count)
                    unlocksCountText.text = if (totalScreenTime != -1L)  sessionCount.toString() else "No data"

                    // Update notifications count
                    notificationsCountText.text = if (totalNotificationsCount > 0) totalNotificationsCount.toString() else "No data"

                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    totalScreenTimeText.text = "Error loading data"
                    swipeRefresh.isRefreshing = false
                }
            } finally {
                activity?.runOnUiThread {
                    loadingOverlay.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
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

    private var appIdMap = mutableMapOf<String, Long>()

    private fun updatePieChart(appUsageDataList: List<AppUsageData>) {
        if (appUsageDataList.isEmpty()) {
            usagePieChart.clear()
            usagePieChart.invalidate()
            return
        }

        // Clear previous mapping
        appIdMap.clear()

        // Take top 5 apps, group the rest as "Other"
        val topApps = appUsageDataList.take(5)
        val otherAppsUsage = appUsageDataList.drop(5).sumOf { it.usageDurationSeconds }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Define colors similar to the image (WhatsApp=Blue, Sync Pro=Red, Clock=Yellow, Phone=Green, Other=Purple)
        val appColors = listOf(
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#FFC107"), // Yellow/Amber
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#9C27B0")  // Purple
        )

        topApps.forEachIndexed { index, app ->
            val minutes = app.usageDurationSeconds / 60f
            if (minutes > 0) {
                entries.add(PieEntry(minutes, app.appName))
                colors.add(appColors[index % appColors.size])
                // Store app ID for click handling
                appIdMap[app.appName] = app.appId
            }
        }

        if (otherAppsUsage > 0) {
            val minutes = otherAppsUsage / 60f
            entries.add(PieEntry(minutes, "Other"))
            colors.add(Color.parseColor("#9E9E9E")) // Gray
        }

        if (entries.isEmpty()) {
            usagePieChart.clear()
            usagePieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            valueTextSize = 0f // Hide values, we show them in labels

            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            valueTextColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                Color.WHITE
            } else {
                Color.BLACK
            }

            selectionShift = 10f // Shift when selected
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE

            valueLinePart1OffsetPercentage = 10f
            valueLinePart1Length = 0.1f
            valueLinePart2Length = 0.1f
            valueLineColor = Color.TRANSPARENT
            isUsingSliceColorAsValueLineColor = false
        }

        val data = PieData(dataSet)

        usagePieChart.data = data
        usagePieChart.invalidate()
    }

    private fun navigateToAppDetail(appId: Long) {
        val fragment = AppDetailFragment.newInstance(appId, selectedDate.timeInMillis)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToGroupDetail(groupId: Long) {
        val fragment = GroupDetailFragment.newInstance(groupId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val STATE_SELECTED_DATE_MILLIS = "state_selected_date_millis"

        fun newInstance() = DashboardFragment()
    }
}
