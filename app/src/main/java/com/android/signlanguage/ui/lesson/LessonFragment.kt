package com.android.signlanguage.ui.lesson

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.R
import com.android.signlanguage.databinding.FragmentLessonBinding
import com.android.signlanguage.ui.lesson.exercises.Exercise
import com.android.signlanguage.ui.lesson.exercises.letter_camera.LetterCameraExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.letter_sign.LetterSignExerciseFragment
import com.android.signlanguage.ui.lesson.exercises.letter_sign.LetterSignExerciseViewModel
import com.android.signlanguage.ui.lesson.exercises.sign_letter.SignLetterExerciseFragment

class LessonFragment : Fragment() {

    companion object {
        init {
            System.loadLibrary("mediapipe_jni")
            System.loadLibrary("opencv_java3")
        }

        val signsDictionary = mapOf(
            Pair('A', R.drawable.letter_a),
            Pair('B', R.drawable.letter_b),
            Pair('C', R.drawable.letter_c),
            Pair('D', R.drawable.letter_d),
            Pair('E', R.drawable.letter_e),
        )
        val maxSigns = signsDictionary.size
    }

    private lateinit var _viewModel: LessonViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLessonBinding.inflate(inflater, container, false)

        _viewModel = ViewModelProvider(this).get(LessonViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        _viewModel.currentExercise.observe(viewLifecycleOwner) {
            showExercise(it)
        }

        _viewModel.finished.observe(viewLifecycleOwner) {
            if (it) {
                if (parentFragmentManager.fragments.size > 0) {
                    val fragmentTransaction = parentFragmentManager.beginTransaction()
                        .hide(parentFragmentManager.fragments[0])
                        .commit()
                }

                Toast.makeText(context, "Congratulations! You won!", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun showExercise(exerciseToShow: Exercise) {
        val exerciseFragment = when (exerciseToShow) {
            Exercise.LETTER_CAMERA -> LetterCameraExerciseFragment()
            Exercise.SIGN_LETTER -> SignLetterExerciseFragment()
            Exercise.LETTER_SIGN -> LetterSignExerciseFragment()
        }
        val fragmentTransaction = parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            .replace(R.id.exercise_fragment_container, exerciseFragment)
            .commit()

        exerciseFragment.viewModelInitialized = { vm ->
            if (vm is FinishedListener) {
                vm.finished.observe(viewLifecycleOwner) {
                    if (it)
                        _viewModel.startNextExercise()
                }
            } else throw ClassCastException("view model must implement FinishedListener")
        }
    }
}