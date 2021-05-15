package com.android.signlanguage.ui.lesson

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.R
import com.android.signlanguage.ViewModelInitListener
import com.android.signlanguage.databinding.FragmentLessonBinding
import com.android.signlanguage.model.skill.UserSkill

class LessonFragment : Fragment() {

    companion object {
        private const val TAG = "LessonFragment"
    }

    private lateinit var _viewModel: LessonViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: ${hashCode()}")

        UserSkill.getInstance(requireContext())

        val binding = FragmentLessonBinding.inflate(inflater, container, false)

        val factory = LessonViewModelFactory { onCurrentScreenChanged(it) }
        _viewModel = ViewModelProvider(this, factory).get(LessonViewModel::class.java)
        _viewModel.currentScreenChanged = {
            onCurrentScreenChanged(it)
        }

        _viewModel.finished.observe(viewLifecycleOwner) {
            if (it != null) {
                findNavController().navigateUp()
                UserSkill.save(requireContext())
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
                    if (it != null) {
                        Log.d(TAG, "showScreen: vmFinished")
                        _viewModel.startNextScreen(!it)
                    }
                }
            } else throw ClassCastException("view model must implement FinishedListener")
        }
    }
}