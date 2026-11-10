package com.example.gaterush.p7c91a3e2.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.gaterush.p7c91a3e2.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
