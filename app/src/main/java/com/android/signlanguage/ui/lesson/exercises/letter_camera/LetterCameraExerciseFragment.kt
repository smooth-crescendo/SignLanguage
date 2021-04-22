package com.android.signlanguage.ui.lesson.exercises.letter_camera

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import com.android.signlanguage.HandTrackingModel
import com.android.signlanguage.SignDetectionModelLoader
import com.android.signlanguage.databinding.FragmentLetterCameraExerciseBinding
import com.android.signlanguage.ViewModelInitListener
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil

class LetterCameraExerciseFragment : Fragment(), ViewModelInitListener {
    private lateinit var _viewModel: LetterCameraExerciseViewModel
    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    private lateinit var _previewDisplayView: SurfaceView
    private var _handTrackingModel = HandTrackingModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLetterCameraExerciseBinding.inflate(inflater, container, false)

        _viewModel = ViewModelProvider(this).get(LetterCameraExerciseViewModel::class.java)
        _viewModel.signDetectionModel = SignDetectionModelLoader().load(requireActivity().assets, "model.tflite")
        viewModelInitialized?.invoke(_viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        _previewDisplayView = SurfaceView(context)
        _previewDisplayView.alpha = 1f
        binding.viewGroup.addView(_previewDisplayView, 0)
        _handTrackingModel.setupPreviewDisplayView(_previewDisplayView)
        AndroidAssetUtil.initializeNativeAssetManager(context)
        _handTrackingModel.initializeProcessor(context)
        _handTrackingModel.addCallback {
            _viewModel.handsCallback(it)
        }

        _handTrackingModel.isCameraLoaded.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.loadingCameraProgressBar.visibility = View.GONE
            }
        }

        PermissionHelper.checkAndRequestCameraPermissions(activity)
        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            binding.noCameraAccessView.visibility = View.GONE
            binding.loadingCameraProgressBar.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        _handTrackingModel.initializeConverter()
        if (PermissionHelper.cameraPermissionsGranted(activity)) {
            _handTrackingModel.startCamera(requireActivity())
        }
    }

    override fun onPause() {
        super.onPause()
        _handTrackingModel.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}