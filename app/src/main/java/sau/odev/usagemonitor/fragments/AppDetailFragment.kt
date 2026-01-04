package sau.odev.usagemonitor.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.appusagemanager.sessions.Session
import sau.odev.usagemonitor.ui.SessionHistoryAdapter
import sau.odev.usagemonitorLib.WellbeingKit
import sau.odev.usagemonitorLib.models.PackageStats
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class AppDetailFragment : Fragment() {

    private lateinit var appIcon: ImageView
    private lateinit var appName: TextView
    private lateinit var statisticsHeader: TextView
    private lateinit var totalUsageText: TextView
    private lateinit var sessionCountText: TextView
    private lateinit var avgSessionText: TextView
    private lateinit var maxUsageText: TextView
    private lateinit var sessionsHeader: TextView
    private lateinit var sessionsRecyclerView: RecyclerView
    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter

    // New combined chart components
    private lateinit var metricSpinner: Spinner
    private lateinit var periodToggleButton: Button
    private lateinit var mainUsageChart: BarChart
    private lateinit var perSessionChart: BarChart

    private lateinit var database: UsageDatabase

    private var appId: Long = -1
    private var selectedDateMillis: Long? = null
    private var currentMetric = ChartMetric.USAGE
    private var currentPeriod = ChartPeriod.DAILY

    private var mainChartJob: Job? = null
    private var perSessionChartJob: Job? = null

    private lateinit var mainUsageLoading: ProgressBar
    private lateinit var perSessionLoading: ProgressBar

    private val selectedDay: LocalDate
        get() {
            val millis = selectedDateMillis
            return if (millis != null && millis > 0L) {
                java.time.Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } else {
                LocalDate.now()
            }
        }

    enum class ChartMetric {
        USAGE, SESSIONS, NOTIFICATIONS
    }

    enum class ChartPeriod {
        DAILY, WEEKLY
    }

    companion object {
        private const val ARG_APP_ID = "app_id"
        private const val ARG_SELECTED_DATE_MILLIS = "selected_date_millis"

        fun newInstance(appId: Long, selectedDateMillis: Long? = null): AppDetailFragment {
            val fragment = AppDetailFragment()
            val args = Bundle()
            args.putLong(ARG_APP_ID, appId)
            if (selectedDateMillis != null) {
                args.putLong(ARG_SELECTED_DATE_MILLIS, selectedDateMillis)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            appId = it.getLong(ARG_APP_ID)
            if (it.containsKey(ARG_SELECTED_DATE_MILLIS)) {
                selectedDateMillis = it.getLong(ARG_SELECTED_DATE_MILLIS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = UsageDatabase.getInstance(requireContext())

        initializeViews(view)
        setupRecyclerView()
        loadAppDetails()
    }

    private fun initializeViews(view: View) {
        appIcon = view.findViewById(R.id.app_icon)
        appName = view.findViewById(R.id.app_name)
        statisticsHeader = view.findViewById(R.id.statistics_header)
        totalUsageText = view.findViewById(R.id.total_usage_text)
        sessionCountText = view.findViewById(R.id.session_count_text)
        avgSessionText = view.findViewById(R.id.avg_session_text)
        maxUsageText = view.findViewById(R.id.max_usage_text)
        sessionsHeader = view.findViewById(R.id.sessions_header)
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler_view)

        // Initialize new components
        metricSpinner = view.findViewById(R.id.metric_spinner)
        periodToggleButton = view.findViewById(R.id.period_toggle_button)
        mainUsageChart = view.findViewById(R.id.main_usage_chart)
        perSessionChart = view.findViewById(R.id.per_session_chart)

        mainUsageLoading = view.findViewById(R.id.main_usage_loading)
        perSessionLoading = view.findViewById(R.id.per_session_loading)

        setupCharts()
        setupMetricSpinner()
        setupPeriodToggle()
    }

    private fun updateDateHeaders() {
        val isToday = selectedDay == LocalDate.now()
        val dateLabel = if (isToday) {
            getString(R.string.today)
        } else {
            java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                .format(
                    Date.from(
                        selectedDay
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                    )
                )
        }

        // Keep the current feel: simple bold header + small date hint.
        statisticsHeader.text = if (isToday) {
            "${getString(R.string.statistics)} • ${getString(R.string.today)}"
        } else {
            "${getString(R.string.statistics)} • $dateLabel"
        }

        sessionsHeader.text = if (isToday) {
            "${getString(R.string.sessions)} • ${getString(R.string.today)}"
        } else {
            "${getString(R.string.sessions)} • $dateLabel"
        }
    }

    private fun setupRecyclerView() {
        sessionHistoryAdapter = SessionHistoryAdapter()
        sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        sessionsRecyclerView.adapter = sessionHistoryAdapter
    }

    private fun loadAppDetails() {
        lifecycleScope.launch {
            val app = AppsUsageManager.getInstance().getInstalledApps().getApp(appId)
            if (app == null) {
                // Handle app not found
                return@launch
            }

            withContext(Dispatchers.Main) {
                updateDateHeaders()
            }

            val appStats = WellbeingKit.getDailyPackageStats(selectedDay)
                .firstOrNull() { it.packageName == app.packageName } ?: PackageStats(
                app.packageName, 0, 0, 0
                )


            withContext(Dispatchers.Main) {

                appName.text = app.name

                // Load app icon
                try {
                    val icon =
                        requireActivity().packageManager.getApplicationIcon(app.packageName)
                    appIcon.setImageDrawable(icon)
                } catch (_: Exception) {
                    appIcon.setImageResource(R.mipmap.ic_launcher)
                }

                totalUsageText.text = formatDuration(appStats.usageTimeMs)
                sessionCountText.text = "${appStats.launchCount}"

                val avgSession = if (appStats.launchCount > 0)
                    appStats.usageTimeMs / appStats.launchCount else 0
                avgSessionText.text = formatDuration(avgSession)

                if (app.maxUsagePerDayInSeconds > 0) {
                    maxUsageText.text =
                        formatDuration(app.maxUsagePerDayInSeconds.toLong() * 1000)
                } else {
                    maxUsageText.text = "No limit set"
                }
            }
            val sessions = WellbeingKit.getDailyAppSessions(app.packageName, selectedDay)
                .map {
                    Session(
                        0,
                        appId,
                        0,
                        app.name,
                        "",
                        app.packageName,
                        it.sessionStartMs,
                        1,
                        it.durationMs
                    )
                }

            // Update session history
            withContext(Dispatchers.Main) {
                sessionHistoryAdapter.updateData(sessions)
            }

            // Load chart data
            loadMainChart()
            loadPerSessionChart()
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

    private fun setupCharts() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val color = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        // Setup Main Chart
        mainUsageChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = color
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = color
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        if (currentMetric == ChartMetric.USAGE) {
                            return formatDuration(value.toLong())
                        }
                        return if (value <= 0) "" else value.toInt().toString()
                    }
                }
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }

        // Setup Per-Session Chart
        perSessionChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = color
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = color
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun setupMetricSpinner() {
        val metrics = arrayOf("Total Usage", "Session Count", "Notification Count")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, metrics)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        metricSpinner.adapter = adapter

        metricSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentMetric = when (position) {
                    0 -> ChartMetric.USAGE
                    1 -> ChartMetric.SESSIONS
                    2 -> ChartMetric.NOTIFICATIONS
                    else -> ChartMetric.USAGE
                }
                loadMainChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPeriodToggle() {
        periodToggleButton.setOnClickListener {
            currentPeriod = if (currentPeriod == ChartPeriod.DAILY) {
                ChartPeriod.WEEKLY
            } else {
                ChartPeriod.DAILY
            }
            periodToggleButton.text =
                currentPeriod.name.lowercase().replaceFirstChar { it.uppercase() }
            loadMainChart()
        }
    }

    private fun setMainChartLoading(isLoading: Boolean) {
        mainUsageLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        mainUsageChart.isEnabled = !isLoading
    }

    private fun setPerSessionChartLoading(isLoading: Boolean) {
        perSessionLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        perSessionChart.isEnabled = !isLoading
    }

    private fun loadMainChart() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val chartValueTextColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        mainChartJob?.cancel()

        mainChartJob = viewLifecycleOwner.lifecycleScope.launch {
            setMainChartLoading(true)
            try {
                val app =
                    AppsUsageManager.getInstance().getInstalledApps().getApp(appId) ?: return@launch

                val entries = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                if (currentPeriod == ChartPeriod.DAILY) {
                    val daily = WellbeingKit.getPackageStatsDaily(
                        app.packageName,
                        selectedDay.minusDays(30),
                        selectedDay
                    ).filter {
                        it.launchCount > 0 || it.usageTimeMs > 0 || it.notificationCount > 0
                    }

                    daily.forEachIndexed { index, stats ->
                        ensureActive()

                        val startDay = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -(daily.size -  index - 1))
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endDay = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, - (daily.size - index - 1))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }

                        val value = when (currentMetric) {
                            ChartMetric.USAGE -> stats.usageTimeMs.toFloat()
                            ChartMetric.SESSIONS -> stats.launchCount.toFloat()
                            ChartMetric.NOTIFICATIONS -> withContext(Dispatchers.IO) {
                                database.getNotificationsDao()
                                    .getNotificationCountByPackageInTimeRange(
                                        app.packageName,
                                        startDay.timeInMillis,
                                        endDay.timeInMillis
                                    ).toFloat()
                            }
                        }

                        entries.add(BarEntry(index.toFloat(), value))
                        labels.add(stats.periodStart.dayOfWeek.name.substring(0, 3))
                    }
                } else {
                    val weekly = WellbeingKit.getPackageStatsWeekly(
                        app.packageName,
                        selectedDay.minusWeeks(12),
                        selectedDay
                    ).filter {
                        it.launchCount > 0 || it.usageTimeMs > 0 || it.notificationCount > 0
                    }

                    weekly.forEachIndexed { index, stats ->
                        ensureActive()

                        val startDay = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -index)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endDay = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -index)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }

                        val value = when (currentMetric) {
                            ChartMetric.USAGE -> stats.usageTimeMs.toFloat()
                            ChartMetric.SESSIONS -> stats.launchCount.toFloat()
                            ChartMetric.NOTIFICATIONS -> withContext(Dispatchers.IO) {
                                database.getNotificationsDao()
                                    .getNotificationCountByPackageInTimeRange(
                                        app.packageName,
                                        startDay.timeInMillis,
                                        endDay.timeInMillis
                                    ).toFloat()
                            }
                        }

                        entries.add(BarEntry(index.toFloat(), value))
                        labels.add("Wk ${stats.periodStart.dayOfYear / 7 + 1}")
                    }
                }

                ensureActive()

                withContext(Dispatchers.Main) {
                    if (mainChartJob !== this@launch) return@withContext

                    val dataSet = BarDataSet(entries, "").apply {
                        color = Color.parseColor("#2196F3")
                        valueTextColor = chartValueTextColor
                        valueTextSize = 10f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                if (value <= 0) return ""
                                return when (currentMetric) {
                                    ChartMetric.USAGE -> formatDuration(value.toLong())
                                    ChartMetric.SESSIONS -> String.format("%.0f", value)
                                    ChartMetric.NOTIFICATIONS -> String.format("%.0f", value)
                                }
                            }
                        }
                    }

                    mainUsageChart.data = BarData(dataSet)
                    mainUsageChart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return labels.getOrNull(value.toInt()) ?: ""
                        }
                    }
                    mainUsageChart.invalidate()
                }
            } finally {
                if (mainChartJob === this@launch) {
                    withContext(Dispatchers.Main) { setMainChartLoading(false) }
                }
            }
        }
    }

    private fun loadPerSessionChart() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val chartValueTextColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }

        perSessionChartJob?.cancel()

        perSessionChartJob = viewLifecycleOwner.lifecycleScope.launch {
            setPerSessionChartLoading(true)
            try {
                val app =
                    AppsUsageManager.getInstance().getInstalledApps().getApp(appId) ?: return@launch

                val sessions = WellbeingKit.getDailyAppSessions(app.packageName, selectedDay)
                    .map {
                        Session(
                            0,
                            appId,
                            0,
                            app.name,
                            "",
                            app.packageName,
                            it.sessionStartMs,
                            1,
                            it.durationMs
                        )
                    }

                val entries = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                sessions.forEachIndexed { index, session ->
                    ensureActive()
                    val minutes = session.usageDuration / (1000f * 60)
                    entries.add(BarEntry(index.toFloat(), minutes))
                    labels.add("#${index + 1}")
                }

                ensureActive()

                withContext(Dispatchers.Main) {
                    if (perSessionChartJob !== this@launch) return@withContext

                    if (entries.isEmpty()) {
                        perSessionChart.clear()
                        perSessionChart.invalidate()
                    } else {
                        val dataSet = BarDataSet(entries, "").apply {
                            color = Color.parseColor("#FF9800")
                            valueTextColor = chartValueTextColor
                            valueTextSize = 10f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return if (value > 0) String.format("%.0fm", value) else ""
                                }
                            }
                        }

                        perSessionChart.data = BarData(dataSet)
                        perSessionChart.xAxis.valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return labels.getOrNull(value.toInt()) ?: ""
                            }
                        }
                        perSessionChart.invalidate()
                    }
                }
            } finally {
                if (perSessionChartJob === this@launch) {
                    withContext(Dispatchers.Main) { setPerSessionChartLoading(false) }
                }
            }
        }
    }
}
