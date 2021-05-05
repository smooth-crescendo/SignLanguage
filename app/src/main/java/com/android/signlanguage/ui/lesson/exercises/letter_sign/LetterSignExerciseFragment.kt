package com.android.signlanguage.ui.lesson.exercises.letter_sign

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.ViewModelInitListener
import com.android.signlanguage.databinding.FragmentLetterSignExerciseBinding
import com.android.signlanguage.ui.lesson.Exercise
import com.android.signlanguage.ui.lesson.ExerciseRules
import com.android.signlanguage.ui.lesson.exercises.letter_camera.LetterCameraExerciseFragment

class LetterSignExerciseFragment : Fragment(), ViewModelInitListener, Exercise {

    companion object : ExerciseRules {
        private const val TAG = "LetterSignExerciseFragment"

        private const val SIGN_BUNDLE = "sign"
        override val unlockedSignsRequired: Int = 2

        fun newInstance(sign: Char): LetterSignExerciseFragment {
            val args = Bundle()
            args.putChar(SIGN_BUNDLE, sign)
            val fragment = LetterSignExerciseFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var _viewModel: LetterSignExerciseViewModel

    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    override val sign: Char
        get() = requireArguments().getChar(SIGN_BUNDLE)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLetterSignExerciseBinding.inflate(inflater, container, false)

        val factory = LetterSignExerciseViewModelFactory(sign)
        _viewModel = ViewModelProvider(this, factory).get(LetterSignExerciseViewModel::class.java)
        viewModelInitialized?.invoke(_viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }
}