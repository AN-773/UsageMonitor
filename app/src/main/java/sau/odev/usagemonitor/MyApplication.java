package sau.odev.usagemonitor;

import android.app.Application;

import sau.odev.usagemonitor.appusagemanager.AppsUsageManager;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppsUsageManager.init(this, getApplicationContext().getPackageName());
        //AppsUsageManager.getInstance().setAlertManager(new AlertsManager(getApplicationContext()));
    }
}
