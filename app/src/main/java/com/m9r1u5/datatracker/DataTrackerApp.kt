package com.m9r1u5.datatracker

import android.app.Application
import com.m9r1u5.datatracker.data.Prefs
import com.m9r1u5.datatracker.data.UsageRepository
import com.m9r1u5.datatracker.work.Notifs
import com.m9r1u5.datatracker.work.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow

class DataTrackerApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var repo: UsageRepository
        private set

    /** 0 = follow phone, 1 = light, 2 = dark. Changing it restyles the app straight away. */
    val themeMode = MutableStateFlow(0)

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = UsageRepository(this, prefs)
        themeMode.value = prefs.themeMode
        Notifs.createChannels(this)
        Scheduler.scheduleAll(this, prefs)
    }
}
