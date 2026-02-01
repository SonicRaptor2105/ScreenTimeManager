package com.example.screentimemanager

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

data class AppTime (
    val packageName: String,
    val appName : String,
    val usage : Long
)
class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView : RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageAccessPermissions()) {
            setAdapter()
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun setAdapter() {
        val adapter = recycleAdapter(screenTime(), this)
        recyclerView.adapter = adapter
    }
    private fun hasUsageAccessPermissions(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun screenTime(): ArrayList<AppTime> {
        val apps = ArrayList<AppTime>()
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: emptyList()

        val sortedStats = usageStats.toList().sortedByDescending {it.totalTimeInForeground }

        sortedStats.forEach { usageStat ->
            val timeInMinutes = usageStat.totalTimeInForeground / 60000
            if (timeInMinutes > 0) {
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(usageStat.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    Log.d("app", usageStat.packageName)
                    apps.add(AppTime(usageStat.packageName, appName, timeInMinutes))
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d("FailedPackageName", e.toString())
                }
            }
        }
        return apps
    }
}