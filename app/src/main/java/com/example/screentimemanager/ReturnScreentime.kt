package com.example.screentimemanager

fun screentime(): Int {
    return android.app.usage.UsageStatsManager.INTERVAL_DAILY
}