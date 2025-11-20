package sau.odev.usagemonitor.appusagemanager.settings

import android.content.Context
import androidx.annotation.StringDef
import java.util.*

class Settings(context: Context): IDeviceUsageSettings, IChallengesSettings {
    private val mPreferences = context.getSharedPreferences("AppsUsageManager_Settings", Context.MODE_PRIVATE)

    private val mSettingsCache = Collections.synchronizedMap(mPreferences.all)

    override fun setDefaultPhonePackageName(packageName: String) {
        putString(KEY_DEFALUT_DIAL_PACKAGE_NAME, packageName)
    }

    override fun getDefaultPhonePackageName(default: String): String = getString(KEY_DEFALUT_DIAL_PACKAGE_NAME, default)

    override fun setDeviceMaxUsageDurationInSeconds(seconds: Int) {
        putInt(KEY_DAY_MAX_USAGE_DURATION_IN_SECONDS, seconds)
    }

    override fun getDeviceMaxUsageDurationInSeconds(default: Int): Int = getInt(KEY_DAY_MAX_USAGE_DURATION_IN_SECONDS, default)


    override fun setDeviceIgnoreUsageDurationUntil(until: Long) {
        putLong(KEY_IGNORE_MAX_USAGE_DURATION_UNTIL, until)
    }

    override fun getDeviceIgnoreUsageDurationUntil(default: Long): Long = getLong(KEY_IGNORE_MAX_USAGE_DURATION_UNTIL, default)


    private fun putBoolean(@Keys key: String, value: Boolean) {
        mPreferences.edit().putBoolean(key, value).apply()
        mSettingsCache[key] = value
    }

    private fun getBoolean(@Keys key: String, defaultValue: Boolean): Boolean =
            if (!mSettingsCache.containsKey(key)) defaultValue else mSettingsCache[key] as Boolean

    private fun putString(@Keys key: String, value: String) {
        mPreferences.edit().putString(key, value).apply()
        mSettingsCache[key] = value
    }

    private fun getString(@Keys key: String, defaultValue: String): String = mSettingsCache[key] as String?
            ?: defaultValue

    private fun putInt(@Keys key: String, value: Int) {
        mPreferences.edit().putInt(key, value).apply()
        mSettingsCache[key] = value
    }

    private fun getInt(@Keys key: String, defaultValue: Int): Int = mSettingsCache[key] as Int? ?: defaultValue

    private fun putLong(@Keys key: String, value: Long) {
        mPreferences.edit().putLong(key, value).apply()
        mSettingsCache[key] = value
    }

    private fun getLong(@Keys key: String, defaultValue: Long): Long = mSettingsCache[key] as Long? ?: defaultValue


    fun hasKey(@Keys key: String): Boolean {
        return mSettingsCache.containsKey(key)
    }

    fun getSettingSize(): Int {
        return mSettingsCache.size
    }

    override fun toString(): String {
        return mSettingsCache.toString()
    }

    @StringDef(value = [KEY_DAY_MAX_USAGE_DURATION_IN_SECONDS, KEY_IGNORE_MAX_USAGE_DURATION_UNTIL])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Keys

    companion object {
        private const val KEY_DAY_MAX_USAGE_DURATION_IN_SECONDS = "dmud"
        private const val KEY_IGNORE_MAX_USAGE_DURATION_UNTIL = "imudu"
        private const val KEY_DEFALUT_DIAL_PACKAGE_NAME = "ddpn"
    }


}