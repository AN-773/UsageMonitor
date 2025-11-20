package sau.odev.usagemonitor.appusagemanager.utils

import android.util.SparseArray


class ObserverManager<I, O>(private val user: ObserverManagerUser<I, O>) {

    private val mObservers = SparseArray<ObserverHolder<I, O>>()

    private var id = 0

    @Synchronized
    fun addObserverHolder(observerHolder: ObserverHolder<I, O>): Int {
        mObservers.put(++id, observerHolder)
        return id
    }

    @Synchronized
    fun removeObserverHolder(id: Int) {
        mObservers.remove(id)
    }

    @Synchronized
    fun onItemInserted(item: I) {
        mObservers.forEach {
            if (it.predicate(item)) {
                user.notifyObserver(it)
            }
        }
    }

    @Synchronized
    fun onItemRemoved(item: I) {
        mObservers.forEach {
            if (it.predicate(item)) {
                user.notifyObserver(it)
            }
        }
    }

    @Synchronized
    fun onItemUpdated(oldItem: I) {
        mObservers.forEach {
            if (it.predicate(oldItem)) {
                user.notifyObserver(it)
            }
        }

    }

    interface ObserverManagerUser<I, O> {
        val observerManager: ObserverManager<I, O>

        fun notifyObserver(observerHolder: ObserverHolder<I, O>)

        fun removeObserverHolder(id: Int) {
            observerManager.removeObserverHolder(id)
        }
    }

    data class ObserverHolder<I, O>(val predicate: Predicate<I>, val observer: Observer<O>)

}
typealias Observer<O> = (O) -> Unit
typealias Predicate<I> =(I) -> Boolean
