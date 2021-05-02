package com.android.signlanguage.ui.lesson

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.signlanguage.FinishedListener
import com.android.signlanguage.ViewModelInitListener

class LessonFinishedViewModel : ViewModel(), FinishedListener {
    private val _finished = MutableLiveData(false)
    override val finished: LiveData<Boolean> = _finished

    fun finish() {
        _finished.value = true
    }
}