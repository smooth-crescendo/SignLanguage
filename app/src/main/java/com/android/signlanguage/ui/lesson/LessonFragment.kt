package com.android.signlanguage.ui.lesson

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.R
import com.android.signlanguage.ViewModelInitListener
import com.android.signlanguage.databinding.FragmentLessonBinding

class LessonFragment : Fragment() {

    companion object {
        private const val TAG = "LessonFragment"

        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }
    }

    private lateinit var _viewModel: LessonViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: ${hashCode()}")

        val binding = FragmentLessonBinding.inflate(inflater, container, false)

        val factory = LessonViewModelFactory { onCurrentScreenChanged(it) }
        _viewModel = ViewModelProvider(this, factory).get(LessonViewModel::class.java)
        _viewModel.currentScreenChanged = {
            onCurrentScreenChanged(it)
        }

        _viewModel.finished.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigateUp()
            }
        }

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }

    private fun onCurrentScreenChanged(screen: Fragment) {
        showScreen(screen)
    }

    private fun showScreen(screenToShow: Fragment) {
        Log.d(TAG, "showScreen: ${hashCode()}")

        if (screenToShow !is ViewModelInitListener)
            return

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            .replace(R.id.exercise_fragment_container, screenToShow)
            .commit()

        screenToShow.viewModelInitialized = { vm ->
            if (vm is FinishedListener) {
                vm.finished.observeForever {
                    if (it) {
                        Log.d(TAG, "showScreen: vmFinished")
                        _viewModel.startNextScreen()
                    }
                }
            } else throw ClassCastException("view model must implement FinishedListener")
        }
    }
}