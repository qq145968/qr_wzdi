package com.scanrobot.app

import android.app.Application
import android.util.Log
import com.scanrobot.app.data.ScanStore
import java.io.File

class ScanApp : Application() {
    lateinit var scanStore: ScanStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ScanRobot_CRASH", "Uncaught exception on ${thread.name}", throwable)

            val sw = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(sw))
            val crashLog = "Time: ${java.util.Date()}\nThread: ${thread.name}\n\n$sw"

            // Write to SharedPreferences synchronously
            getSharedPreferences("crash_log", MODE_PRIVATE)
                .edit()
                .putString("last_crash", crashLog)
                .putLong("crash_time", System.currentTimeMillis())
                .commit()

            // Also write to a file in app's external files dir (visible in file manager)
            try {
                val logFile = File(getExternalFilesDir(null), "crash_log.txt")
                logFile.writeText(crashLog)
            } catch (e: Exception) {
                Log.e("ScanRobot_CRASH", "Failed to write crash file", e)
            }

            previousHandler?.uncaughtException(thread, throwable)
        }

        scanStore = ScanStore(this)
    }

    companion object {
        lateinit var instance: ScanApp
            private set
    }
}
