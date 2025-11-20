package sau.odev.usagemonitor

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import khronos.Dates
import khronos.beginningOfDay
import sau.odev.usagemonitor.appusagemanager.AppsUsageManager
import sau.odev.usagemonitor.appusagemanager.UsageDatabase
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.service.UsageMonitorService
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.ChallengeID
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.appusagemanager.notifications.NotificationTracker
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSession
import sau.odev.usagemonitor.fragments.AppsFragment
import sau.odev.usagemonitor.fragments.ChallengesFragment
import sau.odev.usagemonitor.fragments.DashboardFragment
import sau.odev.usagemonitor.fragments.GroupsFragment
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(),
    AppsFragment.AppEditListener,
    GroupsFragment.GroupEditListener,
    ChallengesFragment.ChallengeCreateListener {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var database: UsageDatabase
    private val executor = Executors.newSingleThreadExecutor()
    private var usageStatsPermissionObserver: ((android.content.Intent?) -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = UsageDatabase.getInstance(applicationContext)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        setupNavigation()

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment.newInstance())
        }

        // Start the foreground service with persistent notification
        UsageMonitorService.startService(this)
    }

    override fun onStart() {
        super.onStart()
        usageStatsPermissionObserver = { intent: android.content.Intent? ->
            startActivity(intent)
        }
        if (!NotificationTracker.hasPermission(this)) {
            val intent = AppsUsageManager.getInstance().getNotificationManager().getNotificationListenerPermissionIntent()
            startActivity(intent)
        }
        AppsUsageManager.getInstance()
            .addUsageStatsPermissionObserver(usageStatsPermissionObserver!!)
    }

    override fun onStop() {
        usageStatsPermissionObserver?.let {
            AppsUsageManager.getInstance().removeUsageStatsPermissionObserver(it)
            usageStatsPermissionObserver = null
        }
        super.onStop()
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment.newInstance())
                    true
                }
                R.id.nav_apps -> {
                    loadFragment(AppsFragment.newInstance())
                    true
                }
                R.id.nav_groups -> {
                    loadFragment(GroupsFragment.newInstance())
                    true
                }
//                R.id.nav_challenges -> {
//                    loadFragment(ChallengesFragment.newInstance())
//                    true
//                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun reloadCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (currentFragment) {
            is AppsFragment -> currentFragment.loadData()
            is GroupsFragment -> currentFragment.loadData()
            is ChallengesFragment -> currentFragment.loadData()
        }
    }

    override fun showEditAppDialog(app: App) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_app, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val appNameInput = dialogView.findViewById<EditText>(R.id.app_name)
        val categoryInput = dialogView.findViewById<EditText>(R.id.app_category)
        val maxUsageInput = dialogView.findViewById<EditText>(R.id.max_usage)
        val groupSpinner = dialogView.findViewById<Spinner>(R.id.group_spinner)
        val ignoredSwitch =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.ignored_switch)
        val floatingTimerSwitch =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.floating_timer_switch)
        val strictModeSwitch =
            dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.strict_mode_switch)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        // Load groups for spinner
        executor.execute {
            val groups = database.getGroupsDao().getGroups()
            runOnUiThread {
                val groupNames = mutableListOf("No Group")
                groupNames.addAll(groups.map { it.name })
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                groupSpinner.adapter = adapter

                // Set current selection
                val selectedIndex = groups.indexOfFirst { it.id == app.groupId }
                groupSpinner.setSelection(if (selectedIndex >= 0) selectedIndex + 1 else 0)

                // Set current values
                appNameInput.setText(app.name)
                categoryInput.setText(app.category)
                maxUsageInput.setText(app.maxUsagePerDayInSeconds.toString())
                ignoredSwitch.isChecked = app.ignored
                floatingTimerSwitch.isChecked = app.floatingTimer
                strictModeSwitch.isChecked = app.strictMode

                saveButton.setOnClickListener {
                    val updatedApp = app.copy(
                        category = categoryInput.text.toString(),
                        maxUsagePerDayInSeconds = maxUsageInput.text.toString().toIntOrNull() ?: 0,
                        groupId = if (groupSpinner.selectedItemPosition > 0) {
                            groups[groupSpinner.selectedItemPosition - 1].id
                        } else {
                            0L
                        },
                        ignored = ignoredSwitch.isChecked,
                        floatingTimer = floatingTimerSwitch.isChecked,
                        strictMode = strictModeSwitch.isChecked
                    )

                    executor.execute {
                        database.getAppsDao().updateApp(updatedApp)
                        runOnUiThread {
                            reloadCurrentFragment()
                            dialog.dismiss()
                            Toast.makeText(this, "App updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    override fun showEditGroupDialog(group: Group?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_group, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val groupNameInput = dialogView.findViewById<EditText>(R.id.group_name)
        val maxUsageInput = dialogView.findViewById<EditText>(R.id.max_usage)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        titleView.text = if (group == null) "Create Group" else "Edit Group"


        if (group != null) {
            groupNameInput.setText(group.name)
            maxUsageInput.setText(group.maxUsagePerDayInSeconds.toString())
        }

        saveButton.setOnClickListener {
            val name = groupNameInput.text.toString()
            val maxUsage = maxUsageInput.text.toString().toIntOrNull() ?: 0

            if (name.isBlank()) {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            executor.execute {
                if (group == null) {
                    val newGroup = Group(name, maxUsage,)
                    database.getGroupsDao().addGroup(newGroup)
                } else {
                    group.name = name
                    group.maxUsagePerDayInSeconds = maxUsage
                    database.getGroupsDao().updateGroup(group)
                }

                runOnUiThread {
                    reloadCurrentFragment()
                    dialog.dismiss()
                    Toast.makeText(this, "Group saved", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun deleteGroup(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete ${group.name}?")
            .setPositiveButton("Delete") { _, _ ->
                executor.execute {
                    database.getGroupsDao().deleteGroup(group)
                    runOnUiThread {
                        reloadCurrentFragment()
                        Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun showCreateChallengeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_challenge, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val categoryGroup = dialogView.findViewById<RadioGroup>(R.id.challenge_category_group)
        val selectLabel = dialogView.findViewById<TextView>(R.id.select_label)
        val targetSpinner = dialogView.findViewById<Spinner>(R.id.target_spinner)
        val typeLabel = dialogView.findViewById<TextView>(R.id.type_label)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.challenge_type_group)
        val valueInputLayout =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.value_input_layout)
        val valueInput = dialogView.findViewById<EditText>(R.id.challenge_value)
        val blockUntilLayout =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.block_until_layout)
        val blockUntilInput = dialogView.findViewById<EditText>(R.id.block_until)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        executor.execute {
            val apps = database.getAppsDao().getAllApps()
            val groups = database.getGroupsDao().getGroups()

            runOnUiThread {
                // Handle category selection
                categoryGroup.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        R.id.radio_app_challenge -> {
                            selectLabel.text = "Select App"
                            typeLabel.visibility = View.VISIBLE
                            typeGroup.visibility = View.VISIBLE
                            targetSpinner.visibility = View.VISIBLE
                            valueInputLayout.visibility = View.VISIBLE
                            blockUntilLayout.visibility = View.GONE

                            val adapter = ArrayAdapter(
                                this,
                                android.R.layout.simple_spinner_item,
                                apps.map { it.name })
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            targetSpinner.adapter = adapter
                        }

                        R.id.radio_group_challenge -> {
                            selectLabel.text = "Select Group"
                            typeLabel.visibility = View.VISIBLE
                            typeGroup.visibility = View.VISIBLE
                            targetSpinner.visibility = View.VISIBLE
                            valueInputLayout.visibility = View.VISIBLE
                            blockUntilLayout.visibility = View.GONE

                            val adapter = ArrayAdapter(
                                this,
                                android.R.layout.simple_spinner_item,
                                groups.map { it.name })
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            targetSpinner.adapter = adapter
                        }

                        R.id.radio_device_fast -> {
                            selectLabel.visibility = View.GONE
                            targetSpinner.visibility = View.GONE
                            typeLabel.visibility = View.GONE
                            typeGroup.visibility = View.GONE
                            valueInputLayout.visibility = View.GONE
                            blockUntilLayout.visibility = View.VISIBLE
                        }
                    }
                }

                // Initialize with app challenge
                val adapter =
                    ArrayAdapter(this, android.R.layout.simple_spinner_item, apps.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                targetSpinner.adapter = adapter

                saveButton.setOnClickListener {
                    val selectedCategoryId = categoryGroup.checkedRadioButtonId

                    executor.execute {
                        try {
                            val challengeId = System.currentTimeMillis()
                            database.getChallengesDao().addChallengeID(ChallengeID(challengeId))

                            when (selectedCategoryId) {
                                R.id.radio_app_challenge -> {
                                    val app = apps[targetSpinner.selectedItemPosition]
                                    val type =
                                        if (typeGroup.checkedRadioButtonId == R.id.radio_limit) {
                                            AppChallenge.TYPE_LIMIT
                                        } else {
                                            AppChallenge.TYPE_FAST
                                        }
                                    val value = valueInput.text.toString().toLongOrNull() ?: 0L

                                    val challenge = AppChallenge(app.id, type, value)
                                    challenge.id = challengeId
                                    database.getChallengesDao().addAppChallenge(challenge)
                                }

                                R.id.radio_group_challenge -> {
                                    val group = groups[targetSpinner.selectedItemPosition]
                                    val type =
                                        if (typeGroup.checkedRadioButtonId == R.id.radio_limit) {
                                            GroupChallenge.TYPE_LIMIT
                                        } else {
                                            GroupChallenge.TYPE_FAST
                                        }
                                    val value = valueInput.text.toString().toLongOrNull() ?: 0L

                                    val challenge = GroupChallenge(group.id, type, value)
                                    challenge.id = challengeId
                                    database.getChallengesDao().addGroupChallenge(challenge)
                                }

                                R.id.radio_device_fast -> {
                                    val blockUntil =
                                        blockUntilInput.text.toString().toLongOrNull() ?: 0L
                                    val challenge = DeviceFastChallenge(blockUntil)
                                    challenge.id = challengeId
                                    database.getChallengesDao().addDeviceFastChallenge(challenge)
                                }
                            }

                            runOnUiThread {
                                reloadCurrentFragment()
                                dialog.dismiss()
                                Toast.makeText(this, "Challenge created", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(
                                    this,
                                    "Error creating challenge: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
}
