package sau.odev.usagemonitor.appusagemanager.utils

import android.os.SystemClock
import kotlin.random.Random

fun <T> List<T>.subListRandom(max: Int): List<T> {
    if (size == 0)
        return emptyList()
    val list = ArrayList<T>()
    val rand = Random(SystemClock.elapsedRealtime())
    val bound = kotlin.math.min(max, size)
    val used = ArrayList<Int>(bound)
    for (i in 0 until bound) {
        var index = rand.nextInt(size)
        while (used.contains(index)) {
            index = rand.nextInt(bound)
        }
        used.add(index)
        list.add(get(index))
    }
    return list
}
