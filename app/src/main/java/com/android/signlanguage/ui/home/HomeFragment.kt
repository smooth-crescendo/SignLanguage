package com.android.signlanguage.ui.home

import android.content.res.AssetManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.R
import com.google.mediapipe.components.CameraHelper.CameraFacing
import com.google.mediapipe.components.CameraXPreviewHelper
import com.google.mediapipe.components.ExternalTextureConverter
import com.google.mediapipe.components.FrameProcessor
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.AndroidAssetUtil
import com.google.mediapipe.framework.Packet
import com.google.mediapipe.framework.PacketGetter
import com.google.mediapipe.glutil.EglManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


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


    private lateinit var previewFrameTexture: SurfaceTexture
    private lateinit var previewDisplayView: SurfaceView
    private lateinit var eglManager: EglManager
    private lateinit var processor: FrameProcessor
    private lateinit var converter: ExternalTextureConverter
    private lateinit var cameraHelper: CameraXPreviewHelper

    private lateinit var viewGroup: ViewGroup
    private lateinit var signButton: Button

    private lateinit var interpreter: Interpreter

    companion object {
        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }

        public val letterA = ArrayList<LandmarkProto.NormalizedLandmarkList>()
        public val letterB = ArrayList<LandmarkProto.NormalizedLandmarkList>()
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PermissionHelper.checkAndRequestCameraPermissions(activity)

        val m = loadModelFile(requireActivity().assets, "model.tflite")

        val options = Interpreter.Options()
        interpreter = Interpreter(m, options)

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

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewGroup = root.findViewById(R.id.preview_display_layout)
        signButton = root.findViewById(R.id.sign_button)

        previewDisplayView = SurfaceView(context)
        setupPreviewDisplayView()

        val packetCreator = processor.packetCreator
        val inputSidePackets: MutableMap<String, Packet> = HashMap()
        inputSidePackets[INPUT_NUM_HANDS_SIDE_PACKET_NAME] = packetCreator.createInt32(NUM_HANDS)
        processor.setInputSidePackets(inputSidePackets)

        processor.addPacketCallback(
            OUTPUT_LANDMARKS_STREAM_NAME
        ) { packet: Packet ->
            handsCallback(packet)
        }

        return root
    }

    private fun handsCallback(packet: Packet) {
        val multiHandLandmarks =
            PacketGetter.getProtoVector(
                packet,
                LandmarkProto.NormalizedLandmarkList.parser()
            )
        val handLandmarks = multiHandLandmarks[0]
        if (signButton.isPressed)
            letterA.add(handLandmarks)

        val inp = Array(1) { Array(21) { FloatArray(3) } }

        for (i in 0..20) {
            val lm = handLandmarks.getLandmark(i)
            inp[0][i][0] = lm.x
            inp[0][i][1] = lm.y
            inp[0][i][2] = lm.z
        }

        val output = Array(1) { FloatArray(5) }
        interpreter.run(inp, output)

        val s = StringBuilder()
        var maxIndex = 0
        for (j in 0..4) {
            s.append(output[0][j])
            s.append("  ")
            if (output[0][j] > output[0][maxIndex])
                maxIndex = j
        }
        signButton.text = ('A' + maxIndex).toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter.close()
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
        cameraHelper.setOnCameraStartedListener { surfaceTexture ->
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