package com.android.signlanguage.ui.home

import android.content.res.AssetManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.R
import com.google.mediapipe.components.*
import com.google.mediapipe.components.CameraHelper.CameraFacing
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
    private lateinit var signText: TextView
    private lateinit var noCameraAccessView: TextView
    private lateinit var loadingCameraProgressBar: ProgressBar
    private lateinit var letterImage: ImageView

    private var isCameraLoaded = MutableLiveData(false)

    private lateinit var interpreter: Interpreter

    companion object {
        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("HomeFragment" + this.hashCode(), "onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment" + this.hashCode(), "onCreateView")
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewGroup = root.findViewById(R.id.preview_display_layout)
        signButton = root.findViewById(R.id.sign_button)
        signText = root.findViewById(R.id.sign_text)
        noCameraAccessView = root.findViewById(R.id.no_camera_access_view)
        loadingCameraProgressBar = root.findViewById(R.id.loading_camera_progress_bar)
        letterImage = root.findViewById(R.id.letter_image)

        val m = loadModelFile(requireActivity().assets, "model.tflite")

        val options = Interpreter.Options()
        interpreter = Interpreter(m, options)

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

        processor.setOnWillAddFrameListener {
            if (isCameraLoaded.value == false)
                isCameraLoaded.postValue(true)
        }

        isCameraLoaded.observe(viewLifecycleOwner) {
            if (it == true) {
                loadingCameraProgressBar.visibility = View.GONE
                letterImage.visibility = View.VISIBLE
            }
        }

        PermissionHelper.checkAndRequestCameraPermissions(activity)
        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            noCameraAccessView.visibility = View.GONE
            loadingCameraProgressBar.visibility = View.VISIBLE

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

        return root
    }

    private fun handsCallback(packet: Packet) {
        val multiHandLandmarks =
            PacketGetter.getProtoVector(
                packet,
                LandmarkProto.NormalizedLandmarkList.parser()
            )
        val handLandmarks = multiHandLandmarks[0]

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

        val output = Array(1) { FloatArray(5) }
        interpreter.run(input, output)

        val signTextStringBuilder = StringBuilder()
        var predictedSignIndex = 0
        for (j in 0..4) {
            signTextStringBuilder.append(output[0][j])
            signTextStringBuilder.append("  ")
            if (output[0][j] > output[0][predictedSignIndex])
                predictedSignIndex = j
        }
        signText.text = ('A' + predictedSignIndex).toString()
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

    override fun onResume() {
        Log.d("HomeFragment" + this.hashCode(), "onResume")
        super.onResume()

        converter = ExternalTextureConverter(
            eglManager.context, 2
        )
        converter.setFlipY(FLIP_FRAMES_VERTICALLY)
        converter.setConsumer(processor)

        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            cameraHelper = CameraXPreviewHelper()
            cameraHelper.setOnCameraStartedListener { surfaceTexture ->
                Log.d("HomeFragment", "camera started")
                previewFrameTexture = surfaceTexture!!
                previewDisplayView.visibility = View.VISIBLE
            }
            cameraHelper.startCamera(
                activity, CAMERA_FACING, null, null
            )
        }
    }

    override fun onPause() {
        Log.d("HomeFragment" + this.hashCode(), "onPause")
        super.onPause()
        converter.close()
        previewDisplayView.visibility = View.GONE
    }

    override fun onDestroyView() {
        Log.d("HomeFragment" + this.hashCode(), "onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d("HomeFragment" + this.hashCode(), "onDestroy")
        super.onDestroy()
        interpreter.close()
    }

    private fun setupPreviewDisplayView() {
        previewDisplayView.visibility = View.GONE
        previewDisplayView.alpha = 0f
        viewGroup.addView(previewDisplayView)
        previewDisplayView
            .holder
            .addCallback(
                object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Log.d("HomeFragment", "surface created")

                        processor.videoSurfaceOutput.setSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                        Log.d("HomeFragment", "surface changed")
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
                        } catch (exception: Exception) {
                            Log.d("HomeFragment", exception.message.orEmpty())
                        }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Log.d("HomeFragment", "surface destroyed")
                        processor.videoSurfaceOutput.setSurface(null)
                    }
                })
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}