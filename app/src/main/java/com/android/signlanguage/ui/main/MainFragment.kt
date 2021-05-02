package com.android.signlanguage.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.android.signlanguage.R
import com.android.signlanguage.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    companion object {
        private const val TAG = "MainFragment"

        fun newInstance(): MainFragment {
            val args = Bundle()

            val fragment = MainFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var _viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMainBinding.inflate(inflater, container, false)

        _viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = _viewModel

        _viewModel.lessonStarted = {
            findNavController().navigate(R.id.action_mainFragment_to_lessonFragment)
        }

        return binding.root
    }
}