package sau.odev.usagemonitor.appusagemanager.settings

interface IChallengesSettings {

    fun setDefaultPhonePackageName(packageName: String)

    fun getDefaultPhonePackageName(default: String): String
}