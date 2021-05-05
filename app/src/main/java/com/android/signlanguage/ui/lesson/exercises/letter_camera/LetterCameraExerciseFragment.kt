package com.android.signlanguage.ui.lesson.exercises.letter_camera

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import com.android.signlanguage.model.ml.HandTrackingModel
import com.android.signlanguage.model.ml.SignDetectionModelLoader
import com.android.signlanguage.databinding.FragmentLetterCameraExerciseBinding
import com.android.signlanguage.ViewModelInitListener
import com.android.signlanguage.ui.lesson.Exercise
import com.android.signlanguage.ui.lesson.ExerciseRules
import com.google.mediapipe.components.PermissionHelper
import com.google.mediapipe.framework.AndroidAssetUtil

class LetterCameraExerciseFragment : Fragment(), ViewModelInitListener, Exercise {

    companion object : ExerciseRules {
        private const val TAG = "LetterCameraExerciseFragment"

        override val unlockedSignsRequired: Int = 1

        private const val SIGN_BUNDLE = "sign"

        fun newInstance(sign: Char): LetterCameraExerciseFragment {
            val args = Bundle()
            args.putChar(SIGN_BUNDLE, sign)
            val fragment = LetterCameraExerciseFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var _viewModel: LetterCameraExerciseViewModel
    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    private lateinit var _previewDisplayView: SurfaceView
    private var _handTrackingModel = HandTrackingModel()

    override val sign: Char
        get() = requireArguments().getChar(SIGN_BUNDLE)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLetterCameraExerciseBinding.inflate(inflater, container, false)

        val factory = LetterCameraExerciseViewModelFactory(sign)
        _viewModel = ViewModelProvider(this, factory).get(LetterCameraExerciseViewModel::class.java)
        _viewModel.signDetectionModel =
            SignDetectionModelLoader().load(requireActivity().assets, "model.tflite")
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

        _viewModel.isCameraAccessible.observe(viewLifecycleOwner) {
            if (it)
                _viewModel.isLoading.value = true
        }

        _handTrackingModel.isCameraLoaded.observe(viewLifecycleOwner) {
            if (_viewModel.isCameraAccessible.value == true)
                _viewModel.isLoading.value = !it
        }

        if (PermissionHelper.cameraPermissionsGranted(activity))
            _viewModel.isCameraAccessible.value = true
        else
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 1)

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
        _viewModel.signDetectionModel.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionHelper.cameraPermissionsGranted(activity))
            _viewModel.isCameraAccessible.value = true
    }
}