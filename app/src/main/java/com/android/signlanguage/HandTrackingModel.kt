package com.android.signlanguage

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.components.CameraHelper
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager

class HandTrackingModel {

    private val TAG = "HandTrackingModel"

    private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
    private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
    private val INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands"
    private val NUM_HANDS = 1
    private val CAMERA_FACING = CameraHelper.CameraFacing.FRONT
    private val FLIP_FRAMES_VERTICALLY = true

    private lateinit var previewFrameTexture: SurfaceTexture
    private lateinit var processor: FrameProcessor
    private lateinit var eglManager: EglManager
    private lateinit var converter: ExternalTextureConverter
    private lateinit var cameraHelper: CameraXPreviewHelper
    private lateinit var previewDisplayView: SurfaceView

    private var handTrackingCallback: ((Array<Array<FloatArray>>) -> Unit)? = null

    private var _isCameraLoaded = MutableLiveData(false)
    val isCameraLoaded: LiveData<Boolean> = _isCameraLoaded

    fun initializeConverter() {
        converter = ExternalTextureConverter(
            eglManager.context, 2
        )
        converter.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter.setConsumer(processor)
    }

    fun setupPreviewDisplayView(pdv: SurfaceView) {
        previewDisplayView = pdv
        previewDisplayView.visibility = View.GONE
        previewDisplayView.alpha = 0f
        previewDisplayView
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Log.d(TAG, "surface created")

                        processor.videoSurfaceOutput.setSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        Log.d(TAG, "surface changed")
                        val viewSize = Size(width, height)
                        val displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize)
                        val isCameraRotated = cameraHelper.isCameraRotated
                        converter.setSurfaceTextureAndAttachToGLContext(
                            previewFrameTexture,
                            if (isCameraRotated) displaySize.height else displaySize.width,
                            if (isCameraRotated) displaySize.width else displaySize.height
                        )
                        try {
                            previewFrameTexture.updateTexImage()
                        } catch (exception: Exception) {}
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Log.d(TAG, "surface destroyed")
                        processor.videoSurfaceOutput.setSurface(null)
                    }
                })
    }

    fun startCamera(activity: Activity) {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper.setOnCameraStartedListener { surfaceTexture ->
            Log.d(TAG, "camera started")
            previewFrameTexture = surfaceTexture!!
            previewDisplayView.visibility = View.VISIBLE
        }
        cameraHelper.startCamera(
            activity, CAMERA_FACING, null, null
        )
    }

    fun initializeProcessor(context: Context?) {
        eglManager = EglManager(null)
        processor = FrameProcessor(
            context,
            eglManager.nativeContext,
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME
        )
        processor
            .videoSurfaceOutput
            .setFlipY(FLIP_FRAMES_VERTICALLY)

        processor.setOnWillAddFrameListener {
            if (_isCameraLoaded.value == false)
                _isCameraLoaded.postValue(true)
        }

        val packetCreator = processor.packetCreator
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets[INPUT_NUM_HANDS_SIDE_PACKET_NAME] = packetCreator.createInt32(NUM_HANDS)
        processor.setInputSidePackets(inputSidePackets)

        processor.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet: Packet ->
            handsCallback(packet)
        }
    }

    fun close() {
        converter.close()
        previewDisplayView.visibility = View.GONE
    }

    private fun handsCallback(packet: Packet) {
        val multiHandLandmarks =
            PacketGetter.getProtoVector(
                packet,
                LandmarkProto.NormalizedLandmarkList.parser()
            )
        val handLandmarks = multiHandLandmarks[0]

        handTrackingCallback?.invoke(processHandLandmarks(handLandmarks))
    }

    private fun processHandLandmarks(handLandmarks: LandmarkProto.NormalizedLandmarkList): Array<Array<FloatArray>> {
        val input = Array(1) { Array(21) { FloatArray(3) } }

        for (i in 0..20) {
            val lm = handLandmarks.getLandmark(i)
            input[0][i][0] = lm.x
            input[0][i][1] = lm.y
            input[0][i][2] = lm.z
        }

        alignAxisLandmarks(input, 0)
        alignAxisLandmarks(input, 1)
        alignAxisLandmarks(input, 2)

        return input
    }

    private fun alignAxisLandmarks(src: Array<Array<FloatArray>>, ax: Int) {
        var min = 100f
        for (i in 0..20) {
            val v = src[0][i][ax]
            if (v < min) {
                min = v
            }
        }
        for (i in 0..20) {
            src[0][i][ax] -= min
        }
    }

    /**
     * @param handsLandmarks Array(1 - hands) { Array(21 - landmarks) { FloatArray(3 - axes) } }
     */
    fun addCallback(callback: (Array<Array<FloatArray>>) -> Unit) {
        handTrackingCallback = callback
    }
}