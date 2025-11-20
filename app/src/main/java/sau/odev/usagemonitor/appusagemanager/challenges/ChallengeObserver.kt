package sau.odev.usagemonitor.appusagemanager.challenges

interface ChallengeObserver {
    fun onTick(seconds: Int, remainingPercent: Int)
    fun onStateChanged(newState: Int)
}