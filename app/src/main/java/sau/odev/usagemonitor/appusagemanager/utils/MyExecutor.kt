package sau.odev.usagemonitor.appusagemanager.utils

import android.os.Handler
import java.util.*
import java.util.concurrent.Executor

class MyExecutor private constructor() : Executor {

    private val queue = LinkedList<Pair<Runnable, Long>>() as Queue<Pair<Runnable, Long>>
    var handler: Handler? = null
    set(value) { //This is because AppsUsageManager.startObserving be called before finishing the AppsUsageManager.init
        field = value
        for (pair in queue) {
            value?.postDelayed(pair.first, pair.second)
        }
    }

    override fun execute(command: Runnable) {
        handler?.post(command) ?: queue.add(Pair(command, 0))
    }

    fun executeDelayed(command: Runnable, delay: Long) {
        handler?.postDelayed(command, delay) ?: queue.add(Pair(command, delay))
    }

    companion object {

        private val executor = MyExecutor()

        internal fun getExecutor(): MyExecutor = executor
    }

}