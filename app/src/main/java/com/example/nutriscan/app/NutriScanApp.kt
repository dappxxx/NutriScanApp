package com.nutriscan.app

import android.app.Application

class NutriScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NutriScanApp
            private set
    }
}