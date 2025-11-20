package sau.odev.usagemonitor.appusagemanager.alerts

import android.os.SystemClock
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
import sau.odev.usagemonitor.appusagemanager.challenges.CreatedChallenges
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.CreatedGroups
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.appusagemanager.sDelayScan
import sau.odev.usagemonitor.appusagemanager.settings.IChallengesSettings
import sau.odev.usagemonitor.appusagemanager.settings.IDeviceUsageSettings

private const val WAIT_TIME = 0

class AlertsManagerWrapper internal constructor() : IAlertsManager {

    internal var alertManager: IAlertsManager? = null

    private var lastShownTime = 0L

    override fun showAppUsageAlert(app: App, appLaunchTime: Long, installedApps: InstalledApps) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showAppUsageAlert(app, appLaunchTime, installedApps)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showGroupUsageAlert(app: App, appLaunchTime: Long, group: Group, createdGroups: CreatedGroups, installedApps: InstalledApps) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showGroupUsageAlert(app, appLaunchTime, group, createdGroups, installedApps)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showDeviceUsageAlert(settings: IDeviceUsageSettings) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showDeviceUsageAlert(settings)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showAppLimitChallengeAlert(appChallenge: AppChallenge, app: App, installedApps: InstalledApps, createdChallenges: CreatedChallenges) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showAppLimitChallengeAlert(appChallenge, app, installedApps, createdChallenges)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showAppFastChallengeAlert(appChallenge: AppChallenge, app: App, installedApps: InstalledApps, createdChallenges: CreatedChallenges) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showAppFastChallengeAlert(appChallenge, app, installedApps, createdChallenges)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showGroupLimitChallengeAlert(groupChallenge: GroupChallenge, app: App, group: Group, installedApps: InstalledApps, createdGroups: CreatedGroups, createdChallenges: CreatedChallenges) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showGroupLimitChallengeAlert(groupChallenge, app, group, installedApps, createdGroups, createdChallenges)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showGroupFastChallengeAlert(groupChallenge: GroupChallenge, app: App, group: Group, installedApps: InstalledApps, createdGroups: CreatedGroups, createdChallenges: CreatedChallenges) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showGroupFastChallengeAlert(groupChallenge, app, group, installedApps, createdGroups, createdChallenges)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun showDeviceFastChallenge(deviceFastChallenge: DeviceFastChallenge, createdChallenges: CreatedChallenges, settings: IChallengesSettings, defaultDialer: String) {
        if (SystemClock.elapsedRealtime() - lastShownTime < WAIT_TIME)
            return
        try {
            alertManager?.showDeviceFastChallenge(deviceFastChallenge, createdChallenges, settings, defaultDialer)
            sDelayScan = 30L
        } catch (e: Exception) {
            hideAlert()
        }
    }

    override fun isShown(): Boolean = alertManager?.isShown() == true

    override fun hideAlert() {
        alertManager?.hideAlert()
        lastShownTime = SystemClock.elapsedRealtime()
        sDelayScan = 1000L
    }


}
