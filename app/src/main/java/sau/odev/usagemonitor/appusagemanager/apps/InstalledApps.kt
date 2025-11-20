package sau.odev.usagemonitor.appusagemanager.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.Log
import sau.odev.usagemonitor.R
import sau.odev.usagemonitor.appusagemanager.IGNORED_PACKAGES
import sau.odev.usagemonitor.appusagemanager.utils.*
import sau.odev.usagemonitor.appusagemanager.utils.Observer
import java.io.*
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class InstalledApps internal constructor(
    private val context: Context,
    private val mAppsDao: AppsDao
) : ObserverManager.ObserverManagerUser<App, List<App>> {


    override val observerManager: ObserverManager<App, List<App>> = ObserverManager(this)

    private val mPackageManager = context.packageManager
    private val mLauncherPackages: ArrayList<String> by lazy {
        getLauncherPackageNames()
    }
    private val mInstalledApps = HashMap<String, App>(50)
    private val mExecutor = MyExecutor.getExecutor()

    internal fun init() {
        synchronized(mInstalledApps) {
            loadInstalledAppsToDb()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    return
                if (Intent.ACTION_PACKAGE_REMOVED == intent.action) {
                    val packageName = intent.dataString?.replace("package:", "") ?: return
                    removeApp(packageName)
                } else if (Intent.ACTION_PACKAGE_ADDED == intent.action) {
                    val packageName = intent.dataString?.replace("package:", "") ?: return
                    mExecutor.execute {
                        addApp(packageName, true)
                    }
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addDataScheme("package")
        context.registerReceiver(receiver, filter)
    }

    private fun loadInstalledAppsToDb() {
        loadInstalledAppsFromDb()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val appsPackages = mPackageManager.queryIntentActivities(intent, 0)?.mapNotNull {
            it?.activityInfo?.packageName
        } ?: return
        appsPackages.forEach {
            Log.d("INSTALLED APPS", "loadInstalledAppsToDb: $it")
            if (mInstalledApps[it] == null) {
                addApp(it, false)
            }
        }
        getAppsCategory()
        mInstalledApps.keys.forEach {
            if (!appsPackages.contains(it)) {
                removeApp(it)
            }
        }
    }

    private fun loadInstalledAppsFromDb() =
        mAppsDao.getAllApps().forEach {
            mInstalledApps[it.packageName] = it
        }


    private fun addApp(packageName: String, checkAppCategory: Boolean): App? {
        try {
            val packageInfo: PackageInfo = mPackageManager.getPackageInfo(packageName, 0)
            val applicationInfo: ApplicationInfo = packageInfo.applicationInfo ?: return null
            val name: String = mPackageManager.getApplicationLabel(applicationInfo)?.toString()
                ?: return null
            var isSystem = false
            var monitor = true
            if (checkAppCategory) {
                try {
                    mPackageManager.resolveActivity(
                        mPackageManager.getLaunchIntentForPackage(packageName)!!,
                        PackageManager.GET_RESOLVED_FILTER
                    )?.filter
                } catch (e: Exception) {
                    isSystem = true
                    monitor = false

                }
            }
            val app = App(
                name = name, system = isSystem,
                packageName = packageName, category = ""
            )
            mInstalledApps[packageName] = app
            if (!monitor || app.packageName in IGNORED_PACKAGES || app.packageName in mLauncherPackages)
                app.ignored = true
            if (app.packageName in getDialPackages()) {
                app.maxUsagePerDayInSeconds = 0
                app.strictMode = false
            }
            saveAppIcon(packageName)
            val id = mAppsDao.addApp(app)
            app.id = id
            observerManager.onItemInserted(app)
            return app
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun getApp(id: Long): App? {
        synchronized(mInstalledApps) {
            for (app in mInstalledApps.values) {
                if (app.id == id)
                    return app
            }
        }
        return null
    }

    fun getApp(packageName: String): App? {
        synchronized(mInstalledApps) {
            var app = mInstalledApps[packageName]
            if (app == null && Looper.getMainLooper().thread.id != Thread.currentThread().id)
                app = addApp(packageName, true)
            return app
        }
    }

    fun toList(getDeleted: Boolean, observer: Observer<List<App>>): Int {
        synchronized(mInstalledApps) {
            val predicate: Predicate<App> = { getDeleted || it.installed }
            val id = observerManager.addObserverHolder(ObserverManager.ObserverHolder(predicate, observer))
            observer(mInstalledApps.values.filter(predicate).filter {
                it.packageName !in IGNORED_PACKAGES
            })
            return id
        }
    }

    fun toList(getDeleted: Boolean): List<App> {
        synchronized(mInstalledApps) {
            val predicate: Predicate<App> = { getDeleted || it.installed }
            return mInstalledApps.values.filter(predicate).filter {
                it.packageName !in IGNORED_PACKAGES
            }
        }
    }

    fun toList(predicate: Predicate<App>, observer: Observer<List<App>>): Int {
        synchronized(mInstalledApps) {
            val id = observerManager.addObserverHolder(ObserverManager.ObserverHolder(predicate, observer))
            observer(mInstalledApps.values.filter(predicate).filter {
                it.packageName !in IGNORED_PACKAGES
            })
            return id
        }
    }

    fun toList(predicate: Predicate<App>): List<App> {
        synchronized(mInstalledApps) {
            return mInstalledApps.values.filter(predicate).filter {
                it.packageName !in IGNORED_PACKAGES
            }
        }
    }

    fun getAppsOfGroup(groupId: Long) = synchronized(mInstalledApps) {
        mInstalledApps.values.filter { it.groupId == groupId }
    }

    fun updateApp(app: App) {
        mExecutor.execute {
            println("Installed apps thread: ${Thread.currentThread().id}")
            synchronized(mInstalledApps) {
                mAppsDao.updateApp(app)
                mInstalledApps[app.packageName]?.copyFrom(app)
            }
        }
    }

    fun size(): Int = mInstalledApps.size

    private fun getAppsCategory() {
        if (true)
            return
        for (app in toList(false)) {
           /* if (app.category != "" && app.category != "error" && app.category != "Nothing")
                continue*/
            //    print("ad")
            try {
                val appInfo = mPackageManager.getApplicationInfo(app.packageName, 0)
                val s = getAppCategory(appInfo)
                println("${app.name}:${s}")
               /* app.category = s
                mAppsDao.updateApp(app)*/
            }catch (e: Exception){
                app.category = "error"
            }
        }
        /* val ex = Executors.newFixedThreadPool(5)
         val apps = mInstalledApps.values.toList()
         val size = apps.size / 5
         ex.execute {
             val s = System.currentTimeMillis()
             val so1 = Socket()
             so1.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
             so1.soTimeout = 20000
             val reader1 = BufferedReader(InputStreamReader(so1.getInputStream()))
             val writer1 = BufferedWriter(OutputStreamWriter(so1.getOutputStream()))
             for (app in apps.subList(size * 0, size * 1)) {
                 writer1.write(app.packageName)
                 writer1.newLine()
                 writer1.flush()
                 val result = reader1.readLine()
                 app.category = result
             }
             so1.close()
             val e = System.currentTimeMillis()
             println("Finish adding in ${(e - s) / 1000} for $size")
         }
         ex.execute {
             val s = System.currentTimeMillis()
             val so2 = Socket()
             so2.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
             so2.soTimeout = 20000
             val reader2 = BufferedReader(InputStreamReader(so2.getInputStream()))
             val writer2 = BufferedWriter(OutputStreamWriter(so2.getOutputStream()))
             for (app in apps.subList(size * 1, size * 2)) {
                 writer2.write(app.packageName)
                 writer2.newLine()
                 writer2.flush()
                 val result = reader2.readLine()
                 app.category = result
             }
             so2.close()
             val e = System.currentTimeMillis()
             println("Finish adding in ${(e - s) / 1000} for $size")
         }
         ex.execute {
             val s = System.currentTimeMillis()
             val so = Socket()
             so.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
             so.soTimeout = 20000
             val reader = BufferedReader(InputStreamReader(so.getInputStream()))
             val writer = BufferedWriter(OutputStreamWriter(so.getOutputStream()))
             for (app in apps.subList(size * 2, size * 3)) {
                 writer.write(app.packageName)
                 writer.newLine()
                 writer.flush()
                 val result = reader.readLine()
                 app.category = result
             }
             so.close()
             val e = System.currentTimeMillis()
             println("Finish adding in ${(e - s) / 1000} for $size")
         }
         ex.execute {
             val s = System.currentTimeMillis()
             val so = Socket()
             so.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
             so.soTimeout = 20000
             val reader = BufferedReader(InputStreamReader(so.getInputStream()))
             val writer = BufferedWriter(OutputStreamWriter(so.getOutputStream()))
             for (app in apps.subList(size * 3, size * 4)) {
                 writer.write(app.packageName)
                 writer.newLine()
                 writer.flush()
                 val result = reader.readLine()
                 app.category = result
             }
             so.close()
             val e = System.currentTimeMillis()
             println("Finish adding in ${(e - s) / 1000} for $size")
         }
         ex.execute {
             val s = System.currentTimeMillis()
             val so = Socket()
             so.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
             so.soTimeout = 20000
             val reader = BufferedReader(InputStreamReader(so.getInputStream()))
             val writer = BufferedWriter(OutputStreamWriter(so.getOutputStream()))
             for (app in apps.subList(size * 4, apps.size)) {
                 writer.write(app.packageName)
                 writer.newLine()
                 writer.flush()
                 val result = reader.readLine()
                 app.category = result
             }
             so.close()
             val e = System.currentTimeMillis()
             println("Finish adding in ${(e - s) / 1000} for ${apps.size - size * 4}")
         }*/
    }

    private fun getAppCategory(applicationInfo: ApplicationInfo): String {
        val url = URL("http://timey.aboody.me/api/categorizer/${applicationInfo.packageName}")
        val reader = BufferedReader(InputStreamReader(url.openConnection().getInputStream()))
        println("${applicationInfo.packageName}: ${reader.readLine()}")
        reader.close()
        return ""
       /*  try {
            val playStoreLink = "https://play.google.com/store/apps/details?id="
            val url = URL(playStoreLink + applicationInfo.packageName)
            val connection = url.openConnection()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            var line: String? = reader.readLine()
            var category = ""
            while (line != null) {
                if (line.contains("applicationCategory")) {
                    val one = line.split("\"applicationCategory\"".toRegex()).dropLastWhile { TextUtils.isEmpty(it) }
                        .toTypedArray()
                    val two = one[1].split("\"".toRegex()).dropLastWhile { TextUtils.isEmpty(it) }.toTypedArray()
                    val fullCategory = two[1]
                    category = if (fullCategory.contains("_"))
                        fullCategory.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    else
                        fullCategory

                }
                line = reader.readLine()
            }
            reader.close()
            return if (TextUtils.isEmpty(category)) ""
            else
                category.toUpperCase(Locale.ROOT)

        } catch (e: Exception) {
            if (e is FileNotFoundException) {
                return "OTHERS"
            }
            e.printStackTrace()
            return "error"
        }*/
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             return when (applicationInfo.category) {
                 ApplicationInfo.CATEGORY_GAME -> "GAME"
                 ApplicationInfo.CATEGORY_AUDIO,
                 ApplicationInfo.CATEGORY_IMAGE,
                 ApplicationInfo.CATEGORY_VIDEO,
                 ApplicationInfo.CATEGORY_NEWS -> "MEDIA"
                 ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
                 ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
                 ApplicationInfo.CATEGORY_MAPS,
                 ApplicationInfo.CATEGORY_UNDEFINED -> "OTHERS"
                 else -> "OTHERS"
             }
         }*/
        /*try {
               val s = Socket()
               s.connect(InetSocketAddress("192.168.1.19", 44343), 3000)
               s.soTimeout = 20000
               val reader = BufferedReader(InputStreamReader(s.getInputStream()))
               val writer = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
               writer.write(applicationInfo.packageName)
               writer.newLine()
               writer.flush()
               val result = reader.readLine()
               s.close()
               result
           } catch (e: Exception) {
               e.printStackTrace()
               "error"
           }*/
    }

    private fun saveAppIcon(packageName: String) {
        try {
            val parent = File(context.filesDir, "icons")
            if (!parent.exists())
                parent.mkdirs()
            val file = File(parent, "$packageName.png")
            if (file.exists()) {
                return
            }
            val output = FileOutputStream(file)
            val bitmap = mPackageManager.getApplicationIcon(packageName).toBitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAppIcon(packageName: String): Drawable {
        return try {
            mPackageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            mInstalledApps[packageName]?.installed = false
            val file = File(File(context.filesDir, "icons"), "$packageName.png")
            if (file.exists()) {
                BitmapDrawable(context.resources, BitmapFactory.decodeFile(file.absolutePath))
            } else
                BitmapDrawable(
                    context.resources,
                    BitmapFactory.decodeResource(context.resources, R.drawable.appusagemanager_default_app_icon)
                )
        }
    }

    private fun getLauncherPackageNames(): ArrayList<String> {
        //https://stackoverflow.com/a/25056270/6763219
        val list = ArrayList<String>()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val result = mPackageManager.queryIntentActivities(intent, 0)
        for (info in result) {
            info ?: continue
            val activityInfo = info.activityInfo
            activityInfo ?: continue
            list.add(activityInfo.packageName)
        }
        return list
    }

    fun removeApp(packageName: String) {
        synchronized(mInstalledApps) {
            val app = mInstalledApps[packageName]
            app?.let {
                val appCopy = it.copy()
                it.installed = false
                observerManager.onItemUpdated(appCopy)
                updateApp(it)
            }
        }
    }


    fun getDialPackages(): ArrayList<String> {
        // Declare action which target application listen to initiate phone call
        val intent = Intent()
        intent.action = Intent.ACTION_DIAL
        // Query for all those applications
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        // Read package name of all those applications
        val packageNames = ArrayList<String>()
        for (resolveInfo in resolveInfos) {
            resolveInfo ?: continue
            val activityInfo = resolveInfo.activityInfo
            activityInfo ?: continue
            val applicationInfo = activityInfo.applicationInfo
            applicationInfo ?: continue
            packageNames.add(applicationInfo.packageName)
        }
        return packageNames
    }

    override fun notifyObserver(observerHolder: ObserverManager.ObserverHolder<App, List<App>>) {
        synchronized(mInstalledApps) {
            observerHolder.observer(mInstalledApps.values.filter(observerHolder.predicate))
        }
    }
}
