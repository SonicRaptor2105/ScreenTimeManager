package com.example.screentimemanager

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.screentimemanager.ui.theme.ScreenTimeManagerTheme

class MainActivity : ComponentActivity() {
    data class AppTime (
        val app : String,
        val usage : Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(text = "abc", modifier = Modifier.padding(innerPadding))
                }
            }
        }

        if (hasPermissions()) {

        }
        else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

    }
    private fun hasPermissions(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun screenTime(): ArrayList<AppTime> {
        val apps = ArrayList<AppTime>()

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 86400000

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val sortedStats = stats.sortedByDescending { it.totalTimeInForeground }

        sortedStats.forEach { usageStat ->
            val timeInMinutes = usageStat.totalTimeInForeground / 60000
            if (timeInMinutes > 0) {
                apps.add(AppTime(usageStat.packageName, timeInMinutes))
            }
        }
        return apps
    }
}