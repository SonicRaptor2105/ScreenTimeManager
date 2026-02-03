package com.example.screentimemanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class appLocker extends Service {
    private static final String CHANNEL_ID = "MonitorChannel";
    private Handler handler = new Handler();
    private Runnable monitorRunnable;
    private String previousApp = "";
    private Set<String> launcherPackages;

    @Override
    public void onCreate() {
        super.onCreate();
        launcherPackages = new HashSet<>();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            launcherPackages.add(resolveInfo.activityInfo.packageName);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App Locker Active")
                .setContentText("Monitoring foreground applications...")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        startMonitoring();
        return START_STICKY;
    }

    private void startMonitoring() {
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                checkForegroundApp();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(monitorRunnable);
    }

    private void checkForegroundApp() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 2 * 60 * 1000, time);
        if (stats != null && !stats.isEmpty()) {
            long lastTimeUsed = 0;
            String foregroundApp = "";

            for (UsageStats usageStats : stats) {
                long lastTime;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    lastTime = usageStats.getLastTimeVisible();
                } else {
                    lastTime = usageStats.getLastTimeUsed();
                }

                if (lastTime > lastTimeUsed) {
                    lastTimeUsed = lastTime;
                    foregroundApp = usageStats.getPackageName();
                }
            }

            if (!foregroundApp.isEmpty() && !foregroundApp.equals(previousApp)) {
                if (!previousApp.isEmpty()) {
                    boolean isPreviousAppLauncher = launcherPackages.contains(previousApp);
                    boolean isForegroundAppLauncher = launcherPackages.contains(foregroundApp);

                    if (isPreviousAppLauncher && !isForegroundAppLauncher) {
                        Log.d("AppMonitor", "App opened: " + foregroundApp + ", usage exceeded: " + getTodayAppUsage(foregroundApp));
                    } else if (!isPreviousAppLauncher && isForegroundAppLauncher) {
                        Log.d("AppMonitor", "App closed: " + previousApp);
                    }
                }
                previousApp = foregroundApp;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Monitor Service Channel", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(monitorRunnable);
        super.onDestroy();
    }

    public boolean getTodayAppUsage(String targetPackage) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        if (stats != null) {
            for (UsageStats usageStats : stats) {
                if (usageStats.getPackageName().equals(targetPackage)) {
                    if (usageStats.getTotalTimeInForeground() >= (1000 * 60)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
