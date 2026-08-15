package com.scanrobot.app

import android.app.Application
import com.scanrobot.app.data.ScanStore

class ScanApp : Application() {
    lateinit var scanStore: ScanStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        scanStore = ScanStore(this)
    }

    companion object {
        lateinit var instance: ScanApp
            private set
    }
}
