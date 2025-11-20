package sau.odev.usagemonitor.appusagemanager.challenges

import android.util.LongSparseArray
import sau.odev.usagemonitor.appusagemanager.AppConstraint
import sau.odev.usagemonitor.appusagemanager.AppConstraintType
import sau.odev.usagemonitor.appusagemanager.RunningAppSession
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.Challenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.sessions.ScreenSession
import sau.odev.usagemonitor.appusagemanager.sessions.SessionsProxy
import sau.odev.usagemonitor.appusagemanager.utils.MyExecutor
import sau.odev.usagemonitor.appusagemanager.utils.subListRandom
import khronos.Dates
import khronos.days
import khronos.endOfDay
import khronos.minus
import java.lang.ref.WeakReference


class ChallengesManager internal constructor(private val dao: ChallengesDao,
                                             private val sessionsProxy: SessionsProxy,
                                             installedApps: InstalledApps) : AppConstraint {

    private val challengesRepo: ChallengesRepo = ChallengesRepo(dao)

    internal val activeChallenges = ArrayList<Challenge>()

    private val dialerApps: ArrayList<App> by lazy {
        ArrayList(installedApps.getDialPackages().map {
            installedApps.getApp(it) as App
        })
    }

    private val challengesObservers = LongSparseArray<WeakReference<ChallengeObserver>>()

    private val mExecutor = MyExecutor.getExecutor()

    internal fun init() {
        activeChallenges.addAll(dao.getAppsChallengesByState(Challenge.STATE_ACTIVE))
        activeChallenges.addAll(dao.getGroupsChallengesByState(Challenge.STATE_ACTIVE))
        dao.getActiveDeviceFastChallenge()?.let { activeChallenges.add(it) }
        updateChallengesAndCheckSessionValidity(null)
    }

    override fun isSessionValid(appSession: RunningAppSession, screenSession: ScreenSession): Pair<Boolean, AppConstraintType> {
        return updateChallengesAndCheckSessionValidity(appSession)
    }

    private fun updateChallengesAndCheckSessionValidity(appSession: RunningAppSession?): Pair<Boolean, AppConstraintType> {
        var valid = Pair(true, AppConstraintType.VALID)
        val today = Dates.today.time
        for (challenge in activeChallenges) {
            val observer = challengesObservers.get(challenge.id)?.get()
            when (challenge) {
                is AppChallenge -> {
                    val isValid = updateAppChallenge(challenge, today, appSession, observer)
                    if (challenge.appId == appSession?.app?.id && valid.first) {
                        valid = isValid
                    }
                }

                is GroupChallenge -> {
                    val isValid = updateGroupChallenge(challenge, today, appSession, observer)
                    if (challenge.groupId == appSession?.group?.id && valid.first)
                        valid = isValid
                }

                is DeviceFastChallenge -> {
                    val isValid = updateDeviceFastChallenge(challenge, appSession, observer)
                    valid = isValid
                }

            }
        }


        val predicate = object : (Challenge) -> Boolean {
            override fun invoke(p1: Challenge): Boolean = p1.state != Challenge.STATE_ACTIVE
        }
        activeChallenges.filter(predicate).forEach {
            challengesRepo.updateChallenge(it)
        }
        activeChallenges.removeAll(predicate)
        return valid
    }

    private fun updateDeviceFastChallenge(challenge: DeviceFastChallenge, session: RunningAppSession?, observer: ChallengeObserver?): Pair<Boolean, AppConstraintType> {
        if (System.currentTimeMillis() >= challenge.blockUntil) {
            challenge.state = Challenge.STATE_COMPLETE
            observer?.onStateChanged(Challenge.STATE_COMPLETE)
            return Pair(true, AppConstraintType.VALID)
        }
        val remainingSeconds = ((challenge.blockUntil - System.currentTimeMillis()) / 1000)
        val passed = System.currentTimeMillis() - challenge.createdTime
        val duration = challenge.blockUntil - challenge.createdTime
        val remainingPercent: Int = passed.toInt() * 100 / duration.toInt()
        observer?.onTick(remainingSeconds.toInt(), remainingPercent)
        return if (session != null && session.app in dialerApps) {
            Pair(false, AppConstraintType.VALID)
        } else
            Pair(false, AppConstraintType.TYPE_DEVICE_FAST_CHALLENGE)
    }

    private fun updateAppChallenge(challenge: AppChallenge, today: Long, session: RunningAppSession?, observer: ChallengeObserver?): Pair<Boolean, AppConstraintType> {
        var valid = Pair(true, AppConstraintType.VALID)
        var appSession = session
        if (appSession?.app?.id != challenge.appId)
            appSession = null
        when (challenge.type) {
            AppChallenge.TYPE_LIMIT -> {
                val usageDuration = if (today == challenge.createdDay) {
                    sessionsProxy.getAppUsageDuration(challenge.appId, challenge.createdTime, System.currentTimeMillis()) + (appSession?.sessionUsage
                            ?: 0L)
                } else {
                    sessionsProxy.getAppUsageDuration(challenge.appId, challenge.createdTime, Dates.from(challenge.createdTime).endOfDay.time)
                }
                when {
                    (usageDuration > challenge.value || (usageDuration == challenge.value && session?.app?.id == challenge.appId)) -> {
                        //Only active app will reach this place
                        valid = Pair(false, AppConstraintType.TYPE_APP_LIMIT_USAGE_CHALLENGE)
                    }
                    today != challenge.createdDay -> {
                        challenge.state = Challenge.STATE_COMPLETE
                        dao.updateAppChallenge(challenge)
                        observer?.onStateChanged(Challenge.STATE_COMPLETE)
                    }
                    else -> {
                        val remainingSeconds = ((challenge.value - usageDuration) / 1000)
                        val remainingPercent = (usageDuration * 100) / challenge.value
                        observer?.onTick(remainingSeconds.toInt(), remainingPercent.toInt())
                    }
                }
            }
            AppChallenge.TYPE_FAST -> {
                if (System.currentTimeMillis() >= challenge.value) {
                    challenge.state = Challenge.STATE_COMPLETE
                    dao.updateAppChallenge(challenge)
                    observer?.onStateChanged(Challenge.STATE_COMPLETE)
                } else {
                    val remainingSeconds = ((challenge.value - System.currentTimeMillis()) / 1000)
                    val passed = System.currentTimeMillis() - challenge.createdTime
                    val duration = challenge.value - challenge.createdTime
                    val remainingPercent: Int = passed.toInt() * 100 / duration.toInt()
                    valid = Pair(false, AppConstraintType.TYPE_APP_FAST_CHALLENGE)
                    observer?.onTick(remainingSeconds.toInt(), remainingPercent)
                }
            }
        }
        return valid
    }

    private fun updateGroupChallenge(challenge: GroupChallenge, today: Long, session: RunningAppSession?, observer: ChallengeObserver?): Pair<Boolean, AppConstraintType> {
        var valid = Pair(true, AppConstraintType.VALID)
        var appSession = session
        if (appSession?.group?.id != challenge.groupId)
            appSession = null
        when (challenge.type) {
            GroupChallenge.TYPE_LIMIT -> {
                val usageDuration = if (today == challenge.createdDay) {
                    sessionsProxy.getGroupUsageDuration(challenge.groupId, challenge.createdTime, System.currentTimeMillis()) + (appSession?.sessionUsage
                            ?: 0L)
                } else {
                    sessionsProxy.getGroupUsageDuration(challenge.groupId, challenge.createdTime, Dates.from(challenge.createdTime).endOfDay.time)
                }
                when {
                    (usageDuration > challenge.value || (usageDuration == challenge.value && session?.group?.id == challenge.groupId)) -> {
                        //Only active app will reach this place
                        valid = Pair(false, AppConstraintType.TYPE_GROUP_LIMIT_USAGE_CHALLENGE)
                    }
                    today != challenge.createdDay -> {
                        challenge.state = Challenge.STATE_COMPLETE
                        dao.updateGroupChallenge(challenge)
                        observer?.onStateChanged(Challenge.STATE_COMPLETE)
                    }
                    else -> {
                        val remainingSeconds = ((challenge.value - usageDuration) / 1000)
                        val remainingPercent = (usageDuration * 100) / challenge.value
                        observer?.onTick(remainingSeconds.toInt(), remainingPercent.toInt())
                    }
                }
            }
            GroupChallenge.TYPE_FAST -> {
                if (System.currentTimeMillis() >= challenge.value) {
                    challenge.state = Challenge.STATE_COMPLETE
                    dao.updateGroupChallenge(challenge)
                    observer?.onStateChanged(Challenge.STATE_COMPLETE)
                } else {
                    val remainingSeconds = ((challenge.value - System.currentTimeMillis()) / 1000)
                    val passed = System.currentTimeMillis() - challenge.createdTime
                    val duration = challenge.value - challenge.createdTime
                    val remainingPercent: Int = passed.toInt() * 100 / duration.toInt()
                    valid = Pair(false, AppConstraintType.TYPE_GROUP_FAST_CHALLENGE)
                    observer?.onTick(remainingSeconds.toInt(), remainingPercent)
                }
            }
        }
        return valid
    }

    internal fun getChallenges(callback: (List<Challenge>) -> Unit) {
        mExecutor.execute {
            callback(challengesRepo.getChallenges())
        }
    }

    internal fun getChallenges(from: Long, to: Long, callback: (List<Challenge>) -> Unit) {
        mExecutor.execute {
            callback(dao.getChallenges(from, to))
        }
    }

    internal fun getChallengesByDay(day: Long, callback: (List<Challenge>) -> Unit) {
        mExecutor.execute {
            callback(challengesRepo.getChallengesByDay(day))
        }
    }

    internal fun addChallenge(challenge: Challenge, callback: (Long) -> Unit) {
        mExecutor.execute {
            if (challenge.state == Challenge.STATE_ACTIVE && !activeChallenges.contains(challenge))
                activeChallenges.add(challenge)
            callback(challengesRepo.addChallenge(challenge))
        }
    }

    internal fun removeChallenge(challenge: Challenge) {
        mExecutor.execute {
            challengesObservers.remove(challenge.id)
            activeChallenges.remove(challenge)
            challengesRepo.removeChallenge(challenge)

        }
    }

    internal fun updateChallenge(challenge: Challenge) {
        mExecutor.execute {
            challengesRepo.updateChallenge(challenge)
            if (challenge.state != Challenge.STATE_ACTIVE) {
                activeChallenges.remove(challenge)
            }
            challengesObservers.get(challenge.id)?.get()?.onStateChanged(challenge.state)
        }
    }

    override fun getPriority(): Int = 10

    fun observeChallenge(challenge: Challenge, observer: ChallengeObserver) {
        mExecutor.execute {
            challengesObservers.put(challenge.id, WeakReference(observer))
            if (challenge.state == Challenge.STATE_ACTIVE) {
                if (!activeChallenges.remove(challenge)){
                    println("No challenge was found")
                }
                activeChallenges.add(challenge)
            }
            updateChallengesAndCheckSessionValidity(null)
        }
    }

    fun removeChallengeObserver(challenge: Challenge) {
        mExecutor.execute {
            challengesObservers.remove(challenge.id)
        }
    }

    fun removeAllChallengeObserver() {
        mExecutor.execute {
            challengesObservers.clear()
        }
    }

    fun getSuggestedChallenges(callback: (List<Challenge>) -> Unit) {
        mExecutor.execute {
            @Suppress("LocalVariableName")
            val HOUR = 1 * 60 * 60 * 1000L
            val excludedApps = dialerApps.map { it.id }
            val excludedChallenges = dao.getChallenges(Dates.yesterday.time, System.currentTimeMillis())
            val sevenDays = 7
            val topUsedApps = sessionsProxy.getTopUsedApps(Dates.today.minus(sevenDays.days).time, System.currentTimeMillis(), 10)
                    .mapNotNull {
                        if(it.appId in excludedApps)
                            return@mapNotNull null
                        if (it.usageDuration > sevenDays * HOUR
                                && !activeChallenges.any { challenge -> challenge is AppChallenge && challenge.type == AppChallenge.TYPE_LIMIT && challenge.appId == it.appId }
                                && !excludedChallenges.any { challenge -> challenge is AppChallenge && challenge.type == AppChallenge.TYPE_LIMIT && challenge.appId == it.appId }) {
                           return@mapNotNull AppChallenge(appId = it.appId, type = AppChallenge.TYPE_LIMIT, value = 1 * HOUR)
                        } else {
                            return@mapNotNull  null
                        }
                    }.subListRandom(3)

            val topVisitedApps = sessionsProxy.getTopVisitedApps(Dates.today.minus((sevenDays).days).time, System.currentTimeMillis(), 10)
                    .mapNotNull {
                        if(it.appId in excludedApps)
                            return@mapNotNull null
                        if (it.visitCount > 60
                                && !activeChallenges.any { challenge -> challenge is AppChallenge && challenge.type == AppChallenge.TYPE_FAST && challenge.appId == it.appId }
                                && !excludedChallenges.any { challenge -> challenge is AppChallenge && challenge.type == AppChallenge.TYPE_FAST && challenge.appId == it.appId }) {
                            AppChallenge(appId = it.appId, type = AppChallenge.TYPE_FAST, value = Dates.now.time + (2 * HOUR))
                        } else
                            null
                    }.subListRandom(3)

            val topUsedGroups = sessionsProxy.getTopUsedGroups(Dates.today.minus((sevenDays).days).time, System.currentTimeMillis(), 10)
                    .mapNotNull {
                        if (it.usageDuration > sevenDays * 2.5 * HOUR
                                && !activeChallenges.any { challenge -> challenge is GroupChallenge && challenge.type == GroupChallenge.TYPE_LIMIT && challenge.groupId == it.groupId }
                                && !excludedChallenges.any { challenge -> challenge is GroupChallenge && challenge.type == GroupChallenge.TYPE_LIMIT && challenge.groupId == it.groupId }) {
                            GroupChallenge(groupId = it.groupId, type = GroupChallenge.TYPE_LIMIT, value = 2 * HOUR)
                        } else
                            null
                    }.subListRandom(2)

            val topVisitedGroups = sessionsProxy.getTopVisitedGroups(Dates.today.minus((7).days).time, System.currentTimeMillis(), 10)
                    .mapNotNull {
                        if (it.visitCount > sevenDays * 30
                                && !activeChallenges.any { challenge -> challenge is GroupChallenge && challenge.type == GroupChallenge.TYPE_FAST && challenge.groupId == it.groupId }
                                && !excludedChallenges.any { challenge -> challenge is GroupChallenge && challenge.type == GroupChallenge.TYPE_FAST && challenge.groupId == it.groupId }
                        ) {
                            GroupChallenge(
                                groupId = it.groupId,
                                type = GroupChallenge.TYPE_FAST,
                                value = System.currentTimeMillis() + (2 * HOUR)
                            )
                        } else
                            null
                    }.subListRandom(2)

            val deviceVisits = sessionsProxy.getScreenSessions(Dates.today.minus((sevenDays).days).time, System.currentTimeMillis())
                    .sumBy {
                        it.visitCount
                    }

            if (deviceVisits > 7 * 20 && !activeChallenges.any { challenge -> challenge is DeviceFastChallenge }) {
                callback(topUsedApps.plus(topVisitedApps).plus(topUsedGroups).plus(topVisitedGroups).plus(DeviceFastChallenge(blockUntil = System.currentTimeMillis() + (2 * HOUR))))
            } else {
                callback(topUsedApps.plus(topVisitedApps).plus(topUsedGroups).plus(topVisitedGroups))
            }
        }
    }

    internal fun getInvalidChallengeOfThisSession(rp: RunningAppSession): Challenge {
        return activeChallenges.first {
            !updateChallengesAndCheckSessionValidity(rp).first
                    && when (it) {
                is AppChallenge -> {
                    it.appId == rp.app.id
                }
                is GroupChallenge -> {
                    it.groupId == rp.group.id
                }
                else -> {
                    true
                }
            }
        }
    }

}
