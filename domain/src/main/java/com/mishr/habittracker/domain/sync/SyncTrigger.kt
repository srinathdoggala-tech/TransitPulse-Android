package com.mishr.habittracker.domain.sync

interface SyncTrigger {
    fun scheduleSync()
    fun schedulePeriodicSync()
}
