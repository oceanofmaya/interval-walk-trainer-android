package com.oceanofmaya.intervalwalktrainer

import android.app.Application
import com.google.android.material.color.DynamicColors

class IntervalWalkTrainerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
