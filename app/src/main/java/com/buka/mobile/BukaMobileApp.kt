package com.buka.mobile

import android.app.Application
import com.airmusic.player.multicast.MobileServiceContext
import com.buka.mobile.data.DiagnosticsRepository

class BukaMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileServiceContext.initialize(this)
        DiagnosticsRepository(this).install()
    }
}
