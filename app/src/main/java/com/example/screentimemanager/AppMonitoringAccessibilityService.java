package com.example.screentimemanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class AppMonitoringAccessibilityService extends AccessibilityService {
    private static final String TAG = "AppMonitoringA11y";
    private String currentForegroundApp = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                String appPackage = packageName.toString();
                if (!appPackage.equals(currentForegroundApp)) {
                    currentForegroundApp = appPackage;
                    Log.d(TAG, "Foreground app changed to: " + appPackage);
                    
                    // Broadcast the current foreground app to appLocker service
                    Intent intent = new Intent("com.example.screentimemanager.FOREGROUND_APP_CHANGED");
                    intent.setPackage(getPackageName());
                    intent.putExtra("package_name", appPackage);
                    sendBroadcast(intent);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
        
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);
    }

    public static String getCurrentForegroundApp() {
        // Method to query current foreground app from accessibility service
        // This is used by the monitoring service if accessibility service is active
        return "";
    }
}
