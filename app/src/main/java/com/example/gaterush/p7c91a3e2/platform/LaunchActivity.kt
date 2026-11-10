package com.example.gaterush.p7c91a3e2.platform

import android.content.pm.ApplicationInfo
import android.os.Bundle
import com.pico.spatial.ui.platform.stub.SpatialLaunchActivity

class LaunchActivity : SpatialLaunchActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        diagnosticsStart = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
            intent.getBooleanExtra("gateRushDiagnosticsStart",false)
        super.onCreate(savedInstanceState)
    }
    companion object { @Volatile var diagnosticsStart=false }
}
