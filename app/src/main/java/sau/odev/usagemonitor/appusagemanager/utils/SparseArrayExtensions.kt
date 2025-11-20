package sau.odev.usagemonitor.appusagemanager.utils

import android.util.SparseArray
import androidx.collection.LongSparseArray

fun <T> SparseArray<T>.forEach(action: (T) -> Unit) {
    for (i in 0 until size()) {
        action(valueAt(i))
    }
}

fun <T> SparseArray<T>.values(): List<T> {
    val list = ArrayList<T>()
    for (i in 0 until size()) {
        list.add(valueAt(i))
    }
    return list
}

fun <T> SparseArray<T>.filter(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (i in 0 until size()) {
        val item = valueAt(i)
        if (predicate(item))
            list.add(item)
    }
    return list
}

fun <T> LongSparseArray<T>.forEach(action: (T) -> Unit) {
    for (i in 0 until size()) {
        action(valueAt(i))
    }
}

fun <T> LongSparseArray<T>.values(): List<T> {
    val list = ArrayList<T>()
    for (i in 0 until size()) {
        list.add(valueAt(i))
    }
    return list
}

fun <T> LongSparseArray<T>.filter(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (i in 0 until size()) {
        val item = valueAt(i)
        if (predicate(item))
            list.add(item)
    }
    return list
}
