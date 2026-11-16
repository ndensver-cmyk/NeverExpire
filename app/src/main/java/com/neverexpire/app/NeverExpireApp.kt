package com.neverexpire.app

import android.app.Application
import com.neverexpire.app.work.ExpiryNotifier
import com.neverexpire.app.work.ExpiryScheduler

class NeverExpireApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ExpiryNotifier.createChannel(this)
        ExpiryScheduler.schedule(this)
    }
}
