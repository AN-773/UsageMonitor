package sau.odev.usagemonitor.appusagemanager.sessions

import sau.odev.usagemonitor.appusagemanager.DeviceObserver

class CurrentScreenSessionsTracker : DeviceObserver {

    private var launchTime = 0L

    @Synchronized
    override fun onDeviceUnlocked(screenSession: ScreenSession) {
        launchTime = screenSession.launchTime
    }

    @Synchronized
    override fun onDeviceClosed(screenSession: ScreenSession) {
        launchTime = 0L
    }

    @Synchronized
    fun getCurrentSessionDuration(): Long =
            if (launchTime == 0L)
                0L
            else
                System.currentTimeMillis() - launchTime

}