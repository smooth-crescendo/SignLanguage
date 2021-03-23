package com.android.signlanguage.ui.home

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.HandTrackingModel
import com.android.signlanguage.R
import com.android.signlanguage.SignModelLoader
import com.google.mediapipe.components.*
import com.google.mediapipe.framework.AndroidAssetUtil
import org.tensorflow.lite.Interpreter


class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var viewGroup: ViewGroup
    private lateinit var previewDisplayView: SurfaceView
    private lateinit var signButton: Button
    private lateinit var signText: TextView
    private lateinit var noCameraAccessView: TextView
    private lateinit var loadingCameraProgressBar: ProgressBar
    private lateinit var letterImage: ImageView

    private var handTrackingModel = HandTrackingModel()

    private lateinit var signModel: Interpreter

    companion object {
        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG + this.hashCode(), "onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG + this.hashCode(), "onCreateView")
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewGroup = root.findViewById(R.id.preview_display_layout)
        signButton = root.findViewById(R.id.sign_button)
        signText = root.findViewById(R.id.sign_text)
        noCameraAccessView = root.findViewById(R.id.no_camera_access_view)
        loadingCameraProgressBar = root.findViewById(R.id.loading_camera_progress_bar)
        letterImage = root.findViewById(R.id.letter_image)

        signModel = SignModelLoader().load(requireActivity().assets, "model.tflite")

        previewDisplayView = SurfaceView(context)
        viewGroup.addView(previewDisplayView)
        handTrackingModel.setupPreviewDisplayView(previewDisplayView)

        AndroidAssetUtil.initializeNativeAssetManager(context)

        handTrackingModel.initializeProcessor(context)

        handTrackingModel.addCallback {
            handsCallback(it)
        }

        handTrackingModel.isCameraLoaded.observe(viewLifecycleOwner) {
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

        return root
    }

    private fun handsCallback(input: Array<Array<FloatArray>>) {
        val output = Array(1) { FloatArray(5) }
        signModel.run(input, output)

        var predictedSignIndex = 0
        for (j in 0..4) {
            if (output[0][j] > output[0][predictedSignIndex])
                predictedSignIndex = j
        }
        signText.text = ('A' + predictedSignIndex).toString()
    }

    override fun onResume() {
        Log.d(TAG + this.hashCode(), "onResume")
        super.onResume()

        handTrackingModel.initializeConverter()

        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            handTrackingModel.startCamera(requireActivity())
        }
    }

    override fun onPause() {
        Log.d(TAG + this.hashCode(), "onPause")
        super.onPause()
        handTrackingModel.close()
    }

    override fun onDestroyView() {
        Log.d(TAG + this.hashCode(), "onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG + this.hashCode(), "onDestroy")
        super.onDestroy()
        signModel.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}