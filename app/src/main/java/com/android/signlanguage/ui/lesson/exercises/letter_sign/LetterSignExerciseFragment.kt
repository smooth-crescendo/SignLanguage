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

class LetterSignExerciseFragment : Fragment(), ViewModelInitListener {
    private lateinit var _viewModel: LetterSignExerciseViewModel

    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLetterSignExerciseBinding.inflate(inflater, container, false)

        _viewModel = ViewModelProvider(this).get(LetterSignExerciseViewModel::class.java)
        viewModelInitialized?.invoke(_viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }
}