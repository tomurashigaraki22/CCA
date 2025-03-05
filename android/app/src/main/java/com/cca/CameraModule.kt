package com.cca

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val context = reactContext
    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    override fun getName(): String = "CameraModule"

    @ReactMethod
    fun getBackCameras(promise: Promise) {
        try {
            val backCameras = WritableNativeArray()
            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameras.pushString(cameraId)
                }
            }
            promise.resolve(backCameras)
        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", e.message)
        }
    }

    @ReactMethod
    fun takePicturesSimultaneously(promise: Promise) {
        try {
            val picturesDir = File(context.getExternalFilesDir(null), "Pictures").apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val results = WritableNativeArray()

            cameraManager.cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    try {
                        val imageReader = ImageReader.newInstance(1920, 1080, android.graphics.ImageFormat.JPEG, 1)
                        
                        imageReader.setOnImageAvailableListener({ reader ->
                            try {
                                reader.acquireLatestImage()?.use { image ->
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    
                                    val file = File(picturesDir, "CAM${cameraId}_${timestamp}.jpg")
                                    FileOutputStream(file).use { it.write(bytes) }
                                    results.pushString(file.absolutePath)
                                }
                            } catch (e: Exception) {
                                Log.e("CameraModule", "Error saving image: ${e.message}")
                            }
                        }, backgroundHandler)

                        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                try {
                                    val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                        addTarget(imageReader.surface)
                                    }

                                    camera.createCaptureSession(
                                        listOf(imageReader.surface),
                                        object : CameraCaptureSession.StateCallback() {
                                            override fun onConfigured(session: CameraCaptureSession) {
                                                session.capture(captureBuilder.build(), null, backgroundHandler)
                                            }
                                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                                        },
                                        backgroundHandler
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraModule", "Error in capture: ${e.message}")
                                }
                            }
                            override fun onDisconnected(camera: CameraDevice) {}
                            override fun onError(camera: CameraDevice, error: Int) {}
                        }, backgroundHandler)

                    } catch (e: Exception) {
                        Log.e("CameraModule", "Error with camera $cameraId: ${e.message}")
                    }
                }
            }

            // Give some time for captures to complete
            backgroundHandler.postDelayed({
                promise.resolve(results)
            }, 3000)

        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", e.message)
        }
    }
} 