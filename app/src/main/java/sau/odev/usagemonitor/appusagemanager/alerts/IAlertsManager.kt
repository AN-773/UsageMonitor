package sau.odev.usagemonitor.appusagemanager.alerts

import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
import sau.odev.usagemonitor.appusagemanager.challenges.CreatedChallenges
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.CreatedGroups
import sau.odev.usagemonitor.appusagemanager.groups.Group
import sau.odev.usagemonitor.appusagemanager.settings.IChallengesSettings
import sau.odev.usagemonitor.appusagemanager.settings.IDeviceUsageSettings

interface IAlertsManager {
    fun showAppUsageAlert(app: App, appLaunchTime: Long, installedApps: InstalledApps)
    fun showGroupUsageAlert(app: App, appLaunchTime: Long, group: Group, createdGroups: CreatedGroups, installedApps: InstalledApps)
    fun showDeviceUsageAlert(settings: IDeviceUsageSettings)
    fun showAppLimitChallengeAlert(appChallenge: AppChallenge, app: App, installedApps: InstalledApps, createdChallenges: CreatedChallenges)
    fun showAppFastChallengeAlert(appChallenge: AppChallenge, app: App, installedApps: InstalledApps, createdChallenges: CreatedChallenges)
    fun showGroupLimitChallengeAlert(groupChallenge: GroupChallenge, app: App, group: Group, installedApps: InstalledApps, createdGroups: CreatedGroups, createdChallenges: CreatedChallenges)
    fun showGroupFastChallengeAlert(groupChallenge: GroupChallenge, app: App, group: Group, installedApps: InstalledApps, createdGroups: CreatedGroups, createdChallenges: CreatedChallenges)
    fun showDeviceFastChallenge(deviceFastChallenge: DeviceFastChallenge, createdChallenges: CreatedChallenges, settings: IChallengesSettings, defaultDialer: String)
    fun isShown(): Boolean

    fun hideAlert()
}