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

    companion object {
        private const val TAG = "SignLetterExerciseFragment"

        private const val SIGN_BUNDLE = "sign"

        fun newInstance(sign: Char): SignLetterExerciseFragment {
            val args = Bundle()
            args.putChar(SIGN_BUNDLE, sign)
            val fragment = SignLetterExerciseFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var _viewModel: SignLetterExerciseViewModel
    override var viewModelInitialized: ((viewModel: ViewModel) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSignLetterExerciseBinding.inflate(inflater, container, false)

        val factory = SignLetterExerciseViewModelFactory(requireArguments().getChar(SIGN_BUNDLE))
        _viewModel = ViewModelProvider(this, factory).get(SignLetterExerciseViewModel::class.java)
        viewModelInitialized?.invoke(_viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        return binding.root
    }
}