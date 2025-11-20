package sau.odev.usagemonitor.appusagemanager.challenges

import sau.odev.usagemonitor.appusagemanager.challenges.pojos.AppChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.Challenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.DeviceFastChallenge
import sau.odev.usagemonitor.appusagemanager.challenges.pojos.GroupChallenge

class ChallengesRepo(private val dao: ChallengesDao) {

    internal fun addChallenge(challenge: Challenge): Long {
        var uid = -1L
        when (challenge) {
            is AppChallenge -> {
                uid = dao.addAppChallenge(challenge)
                challenge.id = uid
            }
            is GroupChallenge -> {
                uid = dao.addGroupChallenge(challenge)
                challenge.id = uid
            }
            is DeviceFastChallenge -> {
                uid = dao.addDeviceFastChallenge(challenge)
                challenge.id = uid
            }
        }
        return uid
    }

    internal fun removeChallenge(challenge: Challenge) {
        when (challenge) {
            is AppChallenge -> {
                dao.removeAppChallenge(challenge)
            }
            is GroupChallenge -> {
                dao.removeGroupChallenge(challenge)
            }
            is DeviceFastChallenge -> {
                dao.removeDeviceFastChallenge(challenge)
            }
        }
    }


    fun getChallenges(): List<Challenge> = dao.getAllChallenges()

    fun getChallenges(from: Long, to: Long) = dao.getChallenges(from, to)

    fun getChallengesByDay(day: Long): List<Challenge> = dao.getChallengesByDay(day)

    fun updateChallenge(challenge: Challenge) {
        when (challenge) {
            is AppChallenge -> {
                dao.updateAppChallenge(challenge)
            }
            is GroupChallenge -> {
                dao.updateGroupChallenge(challenge)
            }
            is DeviceFastChallenge -> {
                dao.updateDeviceFastChallenge(challenge)
            }
        }
    }

}