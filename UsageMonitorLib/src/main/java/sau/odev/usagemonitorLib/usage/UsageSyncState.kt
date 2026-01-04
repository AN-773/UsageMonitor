package sau.odev.usagemonitorLib.usage

import android.content.Context

internal class UsageSyncState(context: Context) {

  private val prefs = context.getSharedPreferences("wbk_sync", Context.MODE_PRIVATE)

  fun getLastUsageIngestMs(): Long = prefs.getLong(KEY_LAST_USAGE_INGEST_MS, 0L)

  fun setLastUsageIngestMs(value: Long) {
    prefs.edit().putLong(KEY_LAST_USAGE_INGEST_MS, value).apply()
  }

  companion object {
    private const val KEY_LAST_USAGE_INGEST_MS = "last_usage_ingest_ms"
  }
}   