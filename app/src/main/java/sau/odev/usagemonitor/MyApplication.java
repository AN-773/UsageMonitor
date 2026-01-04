package sau.odev.usagemonitor;

import android.app.Application;

import sau.odev.usagemonitor.appusagemanager.AppsUsageManager;
import sau.odev.usagemonitorLib.WellbeingKit;


public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        WellbeingKit.INSTANCE.init(this);
        AppsUsageManager.init(this, getApplicationContext().getPackageName());
        //AppsUsageManager.getInstance().setAlertManager(new AlertsManager(getApplicationContext()));
    }
}
