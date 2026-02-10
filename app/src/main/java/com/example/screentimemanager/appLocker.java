package com.example.screentimemanager;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private static final String TAG = "AppLocker";
    private Handler handler = new Handler();
    private Runnable monitorRunnable;
    private String previousApp = "";
    private Set<String> launcherPackages;
    private boolean useAccessibilityService = false;
    private ForegroundAppReceiver foregroundAppReceiver;
    private boolean hasActivityManagerAccess = true;

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
        
        // Test if ActivityManager works, if not use AccessibilityService
        testActivityManagerAccess();
        
        // Register receiver for accessibility service broadcasts if needed
        if (useAccessibilityService) {
            foregroundAppReceiver = new ForegroundAppReceiver();
            IntentFilter filter = new IntentFilter("com.example.screentimemanager.FOREGROUND_APP_CHANGED");
            registerReceiver(foregroundAppReceiver, filter, Context.RECEIVER_EXPORTED);
            Log.d(TAG, "Using AccessibilityService for foreground app detection");
        }
    }
    
    private void testActivityManagerAccess() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            if (runningProcesses == null || runningProcesses.isEmpty()) {
                Log.w(TAG, "ActivityManager returned empty process list, will use AccessibilityService");
                useAccessibilityService = true;
                hasActivityManagerAccess = false;
            } else {
                Log.d(TAG, "ActivityManager is available and working");
                hasActivityManagerAccess = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "ActivityManager not accessible, switching to AccessibilityService", e);
            useAccessibilityService = true;
            hasActivityManagerAccess = false;
        }
    }
    
    private class ForegroundAppReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && 
                intent.getAction().equals("com.example.screentimemanager.FOREGROUND_APP_CHANGED")) {
                String appPackage = intent.getStringExtra("package_name");
                if (appPackage != null && !appPackage.isEmpty()) {
                    handleForegroundAppChange(appPackage);
                }
            }
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
        // Only use polling if ActivityManager works
        if (!useAccessibilityService) {
            monitorRunnable = new Runnable() {
                @Override
                public void run() {
                    checkForegroundApp();
                    handler.postDelayed(this, 500);
                }
            };
            handler.post(monitorRunnable);
            Log.d(TAG, "Started monitoring with ActivityManager polling");
        } else {
            Log.d(TAG, "Monitoring delegated to AccessibilityService (broadcast receiver active)");
        }
    }

    private void checkForegroundApp() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            
            if (runningProcesses != null && !runningProcesses.isEmpty()) {
                // Get the process with highest importance (foreground app)
                ActivityManager.RunningAppProcessInfo foregroundProcess = runningProcesses.get(0);
                int highestImportance = foregroundProcess.importance;
                String foregroundApp = foregroundProcess.processName;
                
                // Extract package name from process name (everything before the colon if exists)
                if (foregroundApp.contains(":")) {
                    foregroundApp = foregroundApp.substring(0, foregroundApp.indexOf(":"));
                }
                
                for (ActivityManager.RunningAppProcessInfo process : runningProcesses) {
                    if (process.importance < highestImportance) {
                        highestImportance = process.importance;
                        foregroundApp = process.processName;
                        if (foregroundApp.contains(":")) {
                            foregroundApp = foregroundApp.substring(0, foregroundApp.indexOf(":"));
                        }
                    }
                }
                
                handleForegroundAppChange(foregroundApp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking foreground app with ActivityManager", e);
            // Switch to accessibility service on error
            if (!useAccessibilityService) {
                useAccessibilityService = true;
                Log.w(TAG, "Switching to AccessibilityService due to ActivityManager error");
                // Stop polling
                if (monitorRunnable != null) {
                    handler.removeCallbacks(monitorRunnable);
                }
                // Register accessibility service receiver
                if (foregroundAppReceiver == null) {
                    foregroundAppReceiver = new ForegroundAppReceiver();
                    IntentFilter filter = new IntentFilter("com.example.screentimemanager.FOREGROUND_APP_CHANGED");
                    registerReceiver(foregroundAppReceiver, filter, Context.RECEIVER_EXPORTED);
                }
            }
        }
    }
    
    private void handleForegroundAppChange(String foregroundApp) {
        if (!foregroundApp.isEmpty() && !foregroundApp.equals(previousApp)) {
            if (!previousApp.isEmpty()) {
                boolean isPreviousAppLauncher = launcherPackages.contains(previousApp);
                boolean isForegroundAppLauncher = launcherPackages.contains(foregroundApp);

                if (isPreviousAppLauncher && !isForegroundAppLauncher) {
                    Log.d(TAG, "App opened: " + foregroundApp + ", usage exceeded: " + getTodayAppUsage(foregroundApp));
                } else if (!isPreviousAppLauncher && isForegroundAppLauncher) {
                    Log.d(TAG, "App closed: " + previousApp);
                }
            }
            previousApp = foregroundApp;
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
        if (monitorRunnable != null) {
            handler.removeCallbacks(monitorRunnable);
        }
        if (foregroundAppReceiver != null) {
            try {
                unregisterReceiver(foregroundAppReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered", e);
            }
        }
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
