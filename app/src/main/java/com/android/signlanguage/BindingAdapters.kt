package com.android.signlanguage

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.android.signlanguage.ui.lesson.LessonFragment

@BindingAdapter("sign")
fun bindSignImage(imgView: ImageView, sign: Char) {
    LessonFragment.signsDictionary[sign]?.let {
        imgView.setImageResource(it)
    }
}