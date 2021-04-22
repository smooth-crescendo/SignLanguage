package com.android.signlanguage.ui.lesson.exercises.sign_letter

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import com.android.signlanguage.databinding.FragmentSignLetterExerciseBinding
import com.android.signlanguage.ViewModelInitListener

class SignLetterExerciseFragment : Fragment(), ViewModelInitListener {
    private lateinit var _viewModel: SignLetterExerciseViewModel
    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSignLetterExerciseBinding.inflate(inflater, container, false)

        _viewModel = ViewModelProvider(this).get(SignLetterExerciseViewModel::class.java)
        viewModelInitialized?.invoke(_viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }
}