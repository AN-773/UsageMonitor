package sau.odev.usagemonitor.appusagemanager.challenges

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.groups.CreatedGroups
import sau.odev.usagemonitor.appusagemanager.apps.InstalledApps
//import sau.odev.usagemonitor.appusagemanager.R
import sau.odev.usagemonitor.appusagemanager.apps.App
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.Challenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge
import sau.odev.usagemonitor.appusagemanager.groups.Group

class CreatedChallenges(private val context: Context,
                        private val challengesManager: ChallengesManager,
                        private val installedApps: InstalledApps,
                        private val createdGroups: CreatedGroups) {


    fun addChallenge(challenge: Challenge, callback: (Long) -> Unit) {
        challengesManager.addChallenge(challenge, callback)
    }

    fun toList(callback: (List<Challenge>) -> Unit) {
        challengesManager.getChallenges(callback)
    }

    fun toListByDay(day: Long, callback: (List<Challenge>) -> Unit) {
        challengesManager.getChallengesByDay(day, callback)
    }

    fun toList(from: Long, to: Long, callback: (List<Challenge>) -> Unit) {
        challengesManager.getChallenges(from, to, callback)
    }

    fun removeChallenge(challenge: Challenge) {
        challengesManager.removeChallenge(challenge)
    }

    fun updateChallenge(challenge: Challenge) {
        challengesManager.updateChallenge(challenge)
    }

    fun getChallengeName(challenge: Challenge): String = when (challenge) {
        is AppChallenge -> {
            val app = installedApps.getApp(challenge.appId)
            app?.name ?: ""
        }
        is GroupChallenge -> {
            val group = createdGroups.getGroup(challenge.groupId)
            group?.name ?: ""
        }
        else -> {
            context.getString(R.string.appusagemanager_phone)
        }
    }

    fun getChallengeIcon(challenge: Challenge, context: Context): Drawable? = when (challenge) {
        is AppChallenge -> {
            val app = installedApps.getApp(challenge.appId)
            if (app == null)
                null
            else
                installedApps.getAppIcon(app.packageName)

        }
        is GroupChallenge -> {
            val group = createdGroups.getGroup(challenge.groupId)
            if (group == null) {
                null
            } else {
                createdGroups.getGroupIcon(group, context)
            }
        }
        else -> {
            ContextCompat.getDrawable(context, R.drawable.appusagemanager_ic_no_phone)
        }
    }

    fun getSuggestedChallenges(callback: (List<Challenge>) -> Unit) {
        challengesManager.getSuggestedChallenges(callback)
    }


    fun observeChallenge(challenge: Challenge, observer: ChallengeObserver) {
        challengesManager.observeChallenge(challenge, observer)
    }

    fun removeChallengeObserver(challenge: Challenge) {
        challengesManager.removeChallengeObserver(challenge)
    }

    fun removeAllChallengeObserver() {
        challengesManager.removeAllChallengeObserver()
    }

    fun groupHasActiveChallenge(group: Group, type: Int): Boolean {
        val activeChallenges = challengesManager.activeChallenges.toMutableList()
        for (challenge in activeChallenges) {
            if (challenge is GroupChallenge && challenge.groupId == group.id && challenge.type == type)
                return true
        }
        return false
    }

    fun appHasActiveChallenge(app: App, type: Int): Boolean {
        val activeChallenges = challengesManager.activeChallenges.toMutableList()
        for (challenge in activeChallenges) {
            if (challenge is AppChallenge && challenge.appId == app.id && challenge.type == type)
                return true
        }
        return false
    }
}