package com.cca

import android.view.SurfaceView
import android.view.SurfaceHolder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import android.hardware.camera2.*
import android.view.Surface
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.HandlerThread
import android.graphics.ImageFormat
import android.util.Size
import kotlin.math.abs

class CameraPreviewManager : SimpleViewManager<SurfaceView>() {
    private var cameraDevice: CameraDevice? = null
    private var cameraId: String? = null
    private lateinit var reactContext: ThemedReactContext
    private var captureSession: CameraCaptureSession? = null
    private var currentSurface: Surface? = null
    
    private val backgroundThread = HandlerThread("CameraBackground").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    override fun getName(): String = "CameraPreview"

    override fun createViewInstance(context: ThemedReactContext): SurfaceView {
        reactContext = context
        return SurfaceView(context).apply {
            setZOrderMediaOverlay(true)
            holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.d("CameraPreview", "Surface Created")
                    holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)
                    currentSurface = holder.surface
                    initializeCamera(holder)
                }
                
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.d("CameraPreview", "Surface Changed: $width x $height")
                    currentSurface = holder.surface
                    if (cameraDevice == null) {
                        initializeCamera(holder)
                    } else {
                        restartPreview(holder)
                    }
                }
                
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.d("CameraPreview", "Surface Destroyed")
                    currentSurface = null
                    releaseCamera()
                }
            })
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val targetRatio = width.toFloat() / height
        return choices.minByOrNull { size ->
            abs((size.width.toFloat() / size.height) - targetRatio) + 
            abs(size.width - width) * 0.1f  // Give less weight to size difference
        } ?: choices[0]
    }

    @ReactProp(name = "cameraId")
    fun setCameraId(view: SurfaceView, newCameraId: String) {
        Log.d("CameraPreview", "Setting camera ID: $newCameraId")
        if (cameraId == newCameraId) return
        releaseCamera()
        cameraId = newCameraId
        view.holder?.let { holder ->
            if (holder.surface.isValid) {
                initializeCamera(holder)
            }
        }
    }

    private fun initializeCamera(holder: SurfaceHolder) {
        try {
            val cameraManager = reactContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cameraId ?: return
            
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw IllegalStateException("Cannot get available preview/video sizes")

            val displaySize = holder.surfaceFrame
            val previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceHolder::class.java),
                displaySize.width(),
                displaySize.height()
            )

            holder.setFixedSize(previewSize.width, previewSize.height)
            
            Log.d("CameraPreview", "Initializing camera: $id with size: ${previewSize.width}x${previewSize.height}")
            
            cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("CameraPreview", "Camera opened: ${camera.id}")
                    cameraDevice = camera
                    createCameraPreviewSession(holder.surface)
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("CameraPreview", "Camera disconnected: ${camera.id}")
                    releaseCamera()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("CameraPreview", "Camera error: ${camera.id}, error: $error")
                    releaseCamera()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error initializing camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession(surface: Surface) {
        try {
            val camera = cameraDevice ?: return
            Log.d("CameraPreview", "Creating preview session for camera: ${camera.id}")

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d("CameraPreview", "Session configured")
                        if (cameraDevice == null) return

                        captureSession = session
                        try {
                            val previewRequest = camera.createCaptureRequest(
                                CameraDevice.TEMPLATE_PREVIEW
                            ).apply {
                                addTarget(surface)
                                // Essential camera preview settings
                                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                // Additional settings for better preview
                                set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                                set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)
                                set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
                            }.build()

                            session.setRepeatingRequest(previewRequest, object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
                                    Log.d("CameraPreview", "Preview capture started")
                                }
                            }, backgroundHandler)
                            Log.d("CameraPreview", "Preview started")
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Error starting preview: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("CameraPreview", "Failed to configure camera session")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error creating preview session: ${e.message}")
        }
    }

    private fun restartPreview(holder: SurfaceHolder) {
        Log.d("CameraPreview", "Restarting preview")
        captureSession?.close()
        captureSession = null
        initializeCamera(holder)
    }

    private fun releaseCamera() {
        Log.d("CameraPreview", "Releasing camera")
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error releasing camera: ${e.message}")
        }
    }

    override fun onDropViewInstance(view: SurfaceView) {
        super.onDropViewInstance(view)
        releaseCamera()
        backgroundThread.quitSafely()
    }
} 