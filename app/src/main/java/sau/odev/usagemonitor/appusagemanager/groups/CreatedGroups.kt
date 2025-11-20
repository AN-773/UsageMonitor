package sau.odev.usagemonitor.appusagemanager.groups

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.collection.LongSparseArray
import androidx.core.content.ContextCompat
import sau.odev.usagemonitor.appusagemanager.utils.*

class CreatedGroups internal constructor(private val groupsDao: GroupsDao) : ObserverManager.ObserverManagerUser<Group, List<Group>> {

    override val observerManager: ObserverManager<Group, List<Group>> = ObserverManager(this)

    private val mCreatedGroups = LongSparseArray<Group>()
    //TODO never delete group just mark it with deleted and the time it was deleted fr auto cleaning
    private val mExecutor = MyExecutor.getExecutor()

    internal fun init() {
        synchronized(mCreatedGroups) {
            loadCreatedGroups()
        }
    }

    private fun loadCreatedGroups() =
            groupsDao.getGroups().forEach { mCreatedGroups.put(it.id, it) }


    fun toList(observer: Observer<List<Group>>): Int {
        synchronized(mCreatedGroups) {
            val predicate: Predicate<Group> = { true}
            val id = observerManager.addObserverHolder(ObserverManager.ObserverHolder(predicate, observer))
            observer(mCreatedGroups.values())
            return id
        }
    }

    fun toList(): List<Group> {
        synchronized(mCreatedGroups) {
            return mCreatedGroups.values()
        }
    }

    fun toFilteredList(predicate: Predicate<Group>, observer: Observer<List<Group>>): Int {
        synchronized(mCreatedGroups) {
            val id = observerManager.addObserverHolder(ObserverManager.ObserverHolder(predicate, observer))
            observer(mCreatedGroups.filter(predicate))
            return id
        }
    }

    fun toFilteredList(predicate: Predicate<Group>): List<Group> {
        synchronized(mCreatedGroups) {
            return (mCreatedGroups.filter(predicate))
        }
    }

    fun getGroup(groupId: Long): Group? {
        synchronized(mCreatedGroups) {
            return mCreatedGroups[groupId]
        }
    }

    fun addGroup(group: Group, callback: (Long) -> Unit) {
        mExecutor.execute {
            synchronized(mCreatedGroups) {
                val id = groupsDao.addGroup(group)
                group.id = id
                mCreatedGroups.put(id, group)
                callback(id)
            }
        }
    }

    fun updateGroup(group: Group) {
        mExecutor.execute {
            synchronized(mCreatedGroups) {
                groupsDao.updateGroup(group)
                mCreatedGroups[group.id]?.copyFrom(group)
            }
        }
    }

    fun removeGroup(group: Group) {
        mExecutor.execute {
            synchronized(mCreatedGroups) {
                groupsDao.deleteGroup(group)
                mCreatedGroups.remove(group.id)
            }
        }
    }

    fun getGroupIcon(group: Group, context: Context): Drawable {
        return ContextCompat.getDrawable(context, group.icon)!!
    }

    override fun notifyObserver(observerHolder: ObserverManager.ObserverHolder<Group, List<Group>>) {
        synchronized(mCreatedGroups) {
            observerHolder.observer(mCreatedGroups.filter(observerHolder.predicate))
        }
    }

}