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
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.appusagemanager.sessions.Session
import sau.odev.usagemonitor.ui.SessionHistoryAdapter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class AppDetailFragment : Fragment() {

    private lateinit var appIcon: ImageView
    private lateinit var appName: TextView
    private lateinit var totalUsageText: TextView
    private lateinit var sessionCountText: TextView
    private lateinit var avgSessionText: TextView
    private lateinit var maxUsageText: TextView
    private lateinit var sessionsRecyclerView: RecyclerView
    private lateinit var sessionHistoryAdapter: SessionHistoryAdapter

    // New combined chart components
    private lateinit var metricSpinner: Spinner
    private lateinit var periodToggleButton: Button
    private lateinit var mainUsageChart: BarChart
    private lateinit var perSessionChart: BarChart

    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()

    private var appId: Long = -1
    private var currentMetric = ChartMetric.USAGE
    private var currentPeriod = ChartPeriod.DAILY

    enum class ChartMetric {
        USAGE, SESSIONS, NOTIFICATIONS
    }

    enum class ChartPeriod {
        DAILY, WEEKLY
    }

    companion object {
        private const val ARG_APP_ID = "app_id"

        fun newInstance(appId: Long): AppDetailFragment {
            val fragment = AppDetailFragment()
            val args = Bundle()
            args.putLong(ARG_APP_ID, appId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            appId = it.getLong(ARG_APP_ID)
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
        totalUsageText = view.findViewById(R.id.total_usage_text)
        sessionCountText = view.findViewById(R.id.session_count_text)
        avgSessionText = view.findViewById(R.id.avg_session_text)
        maxUsageText = view.findViewById(R.id.max_usage_text)
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler_view)

        // Initialize new components
        metricSpinner = view.findViewById(R.id.metric_spinner)
        periodToggleButton = view.findViewById(R.id.period_toggle_button)
        mainUsageChart = view.findViewById(R.id.main_usage_chart)
        perSessionChart = view.findViewById(R.id.per_session_chart)

        setupCharts()
        setupMetricSpinner()
        setupPeriodToggle()
    }

    private fun setupRecyclerView() {
        sessionHistoryAdapter = SessionHistoryAdapter()
        sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        sessionsRecyclerView.adapter = sessionHistoryAdapter
    }

    private fun loadAppDetails() {
        executor.execute {
            try {
                val app = AppsUsageManager.getInstance().getInstalledApps().getApp(appId)
                if (app == null) {
                    activity?.runOnUiThread {
                        // Handle app not found
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

                val sessions =
                    database.getSessionDao().getAppSessions(appId, startOfDay, endOfDay).map {
                        Session(
                            it.id,
                            it.appId,
                            it.groupId,
                            app.name,
                            "",
                            app.packageName,
                            it.launchTime,
                            it.visitCount,
                            it.usageDuration
                        )
                    }
                val totalUsage = sessions.sumOf { it.usageDuration }
                val sessionCount = sessions.size
                val avgSession = if (sessionCount > 0) totalUsage / sessionCount else 0

                activity?.runOnUiThread {
                    appName.text = app.name

                    // Load app icon
                    try {
                        val icon =
                            requireActivity().packageManager.getApplicationIcon(app.packageName)
                        appIcon.setImageDrawable(icon)
                    } catch (e: Exception) {
                        appIcon.setImageResource(R.mipmap.ic_launcher)
                    }

                    totalUsageText.text = formatDuration(totalUsage)
                    sessionCountText.text = "$sessionCount"
                    avgSessionText.text = formatDuration(avgSession)

                    if (app.maxUsagePerDayInSeconds > 0) {
                        maxUsageText.text =
                            formatDuration(app.maxUsagePerDayInSeconds.toLong() * 1000)
                    } else {
                        maxUsageText.text = "No limit set"
                    }

                    // Update session history
                    sessionHistoryAdapter.updateData(sessions)

                    // Load chart data
                    loadMainChart()
                    loadPerSessionChart()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun loadMainChart() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val _color = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        executor.execute {
            try {
                val entries = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                if (currentPeriod == ChartPeriod.DAILY) {
                    // Get last 7 days
                    val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    for (i in 6 downTo 0) {
                        val calendar = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -i)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val startOfDay = calendar.timeInMillis
                        val endOfDay = calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val value = when (currentMetric) {
                            ChartMetric.USAGE -> {
                                val usage = database.getSessionDao()
                                    .getAppUsageDuration(appId, startOfDay, endOfDay)
                                usage / (1000f * 60 * 60) // Convert to hours
                            }

                            ChartMetric.SESSIONS -> {
                                database.getSessionDao()
                                    .getAppSessionCount(appId, startOfDay, endOfDay).toFloat()
                            }

                            ChartMetric.NOTIFICATIONS -> {
                                val app =
                                    AppsUsageManager.getInstance().getInstalledApps().getApp(appId)
                                if (app != null) {
                                    database.getNotificationsDao()
                                        .getNotificationCountByPackageInTimeRange(
                                            app.packageName, startOfDay, endOfDay
                                        ).toFloat()
                                } else 0f
                            }
                        }

                        entries.add(BarEntry((6 - i).toFloat(), value))
                        labels.add(dateFormat.format(calendar.time))
                    }
                } else {
                    // Get last 4 weeks
                    for (i in 3 downTo 0) {
                        val calendar = Calendar.getInstance().apply {
                            add(Calendar.WEEK_OF_YEAR, -i)
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val startOfWeek = calendar.timeInMillis
                        val endOfWeek = calendar.apply {
                            add(Calendar.DAY_OF_YEAR, 6)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val value = when (currentMetric) {
                            ChartMetric.USAGE -> {
                                val usage = database.getSessionDao()
                                    .getAppUsageDuration(appId, startOfWeek, endOfWeek)
                                usage / (1000f * 60 * 60) // Convert to hours
                            }

                            ChartMetric.SESSIONS -> {
                                database.getSessionDao()
                                    .getAppSessionCount(appId, startOfWeek, endOfWeek).toFloat()
                            }

                            ChartMetric.NOTIFICATIONS -> {
                                val app =
                                    AppsUsageManager.getInstance().getInstalledApps().getApp(appId)
                                if (app != null) {
                                    database.getNotificationsDao()
                                        .getNotificationCountByPackageInTimeRange(
                                            app.packageName, startOfWeek, endOfWeek
                                        ).toFloat()
                                } else 0f
                            }
                        }

                        entries.add(BarEntry((3 - i).toFloat(), value))

                        val weekLabel = SimpleDateFormat("MMM dd", Locale.getDefault())
                        labels.add(weekLabel.format(calendar.time))
                    }
                }



                activity?.runOnUiThread {
                    val dataSet = BarDataSet(entries, "").apply {
                        color = Color.parseColor("#2196F3")
                        valueTextColor = _color
                        valueTextSize = 10f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                if (value <= 0) return ""
                                return when (currentMetric) {
                                    ChartMetric.USAGE -> String.format("%.1fh", value)
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadPerSessionChart() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val _color = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            Color.WHITE
        } else {
            Color.BLACK
        }
        executor.execute {
            try {
                val entries = mutableListOf<BarEntry>()
                val labels = mutableListOf<String>()

                // Get today's start and end
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

                val sessions = database.getSessionDao().getAppSessions(appId, startOfDay, endOfDay)

                sessions.forEachIndexed { index, session ->
                    val minutes = session.usageDuration / (1000f * 60)
                    entries.add(BarEntry(index.toFloat(), minutes))
                    labels.add("#${index + 1}")
                }

                activity?.runOnUiThread {
                    if (entries.isEmpty()) {
                        // Show empty chart
                        perSessionChart.clear()
                        perSessionChart.invalidate()
                    } else {
                        val dataSet = BarDataSet(entries, "").apply {
                            color = Color.parseColor("#FF9800")
                            valueTextColor = color
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

