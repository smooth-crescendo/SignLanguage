package com.android.signlanguage.ui.home

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.R
import com.google.mediapipe.components.CameraHelper.CameraFacing
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.*
import com.google.mediapipe.glutil.EglManager


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private val BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb"
    private val INPUT_VIDEO_STREAM_NAME = "input_video"
    private val OUTPUT_VIDEO_STREAM_NAME = "output_video"
    private val OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks"
    private val INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands"
    private val NUM_HANDS = 1
    private val CAMERA_FACING = CameraFacing.FRONT
    private val FLIP_FRAMES_VERTICALLY = true

    init
    {
        System.loadLibrary("mediapipe_jni")
        System.loadLibrary("opencv_java3")
    }

    private lateinit var previewFrameTexture: SurfaceTexture
    private lateinit var previewDisplayView: SurfaceView
    private lateinit var eglManager: EglManager
    private lateinit var processor: FrameProcessor
    private lateinit var converter: ExternalTextureConverter
    private lateinit var cameraHelper: CameraXPreviewHelper

    private lateinit var viewGroup: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewGroup = root.findViewById(R.id.preview_display_layout)

        previewDisplayView = SurfaceView(context)
        setupPreviewDisplayView()

        AndroidAssetUtil.initializeNativeAssetManager(context)
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

        PermissionHelper.checkAndRequestCameraPermissions(activity)
        val packetCreator = processor.packetCreator
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets[INPUT_NUM_HANDS_SIDE_PACKET_NAME] = packetCreator.createInt32(NUM_HANDS)
        processor.setInputSidePackets(inputSidePackets)

//        if (Log.isLoggable(TAG, Log.VERBOSE)) {
//            processor.addPacketCallback(
//                OUTPUT_LANDMARKS_STREAM_NAME
//            ) { packet: Packet ->
//                Log.v(TAG, "Received multi-hand landmarks packet.")
//                val multiHandLandmarks =
//                    PacketGetter.getProtoVector(
//                        packet,
//                        NormalizedLandmarkList.parser()
//                    )
//                Log.v(
//                    TAG,
//                    "[TS:"
//                            + packet.timestamp
//                            + "] "
//                            + getMultiHandLandmarksDebugString(multiHandLandmarks)
//                )
//            }
//        }


        return root
    }

    override fun onResume() {
        super.onResume()
        converter = ExternalTextureConverter(
            eglManager.context, 2
        )
        converter.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter.setConsumer(processor)
        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        converter.close()
        previewDisplayView.visibility = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun cameraTargetResolution(): Size? {
        return null
    }

    private fun startCamera() {
        cameraHelper = CameraXPreviewHelper()
        cameraHelper.setOnCameraStartedListener{ surfaceTexture ->
            previewFrameTexture = surfaceTexture!!
            previewDisplayView.visibility = View.VISIBLE
        }
        val cameraFacing = CameraFacing.FRONT
        cameraHelper.startCamera(
            activity, cameraFacing, null, cameraTargetResolution()
        )
    }

    private fun computeViewSize(width: Int, height: Int): Size {
        return Size(width, height)
    }

    private fun onPreviewDisplaySurfaceChanged(
        width: Int, height: Int
    ) {
        val viewSize = computeViewSize(width, height)
        val displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize)
        val isCameraRotated = cameraHelper.isCameraRotated
        converter.setSurfaceTextureAndAttachToGLContext(
            previewFrameTexture,
            if (isCameraRotated) displaySize.height else displaySize.width,
            if (isCameraRotated) displaySize.width else displaySize.height
        )
    }

    private fun setupPreviewDisplayView() {
        previewDisplayView.visibility = View.GONE
        viewGroup.addView(previewDisplayView)
        previewDisplayView
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        processor.videoSurfaceOutput.setSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        onPreviewDisplaySurfaceChanged(width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        processor.videoSurfaceOutput.setSurface(null)
                    }
                })
    }
}