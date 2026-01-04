package sau.odev.usagemonitorLib.storage

internal object UsageEventTypes {
  // Mirrors UsageEvents.Event constants, but we store raw ints from the system.
  // Example values: UsageEvents.Event.ACTIVITY_RESUMED, ACTIVITY_PAUSED, MOVE_TO_FOREGROUND, etc.
}

internal object NotificationEventTypes {
  const val POSTED = 1
  const val REMOVED = 2
}