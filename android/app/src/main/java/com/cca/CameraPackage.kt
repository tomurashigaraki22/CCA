package com.cca

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class CameraPackage : ReactPackage {
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> = 
        listOf(CameraPreviewManager())

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> = 
        listOf(CameraModule(reactContext))
} 