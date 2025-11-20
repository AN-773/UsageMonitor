package sau.odev.usagemonitor.appusagemanager.settings

interface IDeviceUsageSettings {

    fun setDeviceMaxUsageDurationInSeconds(seconds: Int)

    fun getDeviceMaxUsageDurationInSeconds(default: Int): Int

    fun setDeviceIgnoreUsageDurationUntil(until: Long)

    fun getDeviceIgnoreUsageDurationUntil(default: Long): Long

}