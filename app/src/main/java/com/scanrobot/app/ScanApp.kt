package com.scanrobot.app

import android.app.Application
import android.util.Log
import com.scanrobot.app.data.ScanStore

class ScanApp : Application() {
    lateinit var scanStore: ScanStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        scanStore = ScanStore(this)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ScanRobot_CRASH", "Uncaught exception on ${thread.name}", throwable)
            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            val crashLog = sw.toString()
            getSharedPreferences("crash_log", MODE_PRIVATE)
                .edit()
                .putString("last_crash", crashLog)
                .putLong("crash_time", System.currentTimeMillis())
                .apply()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    companion object {
        lateinit var instance: ScanApp
            private set
    }
}
