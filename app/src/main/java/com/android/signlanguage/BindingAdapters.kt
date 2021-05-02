package com.android.signlanguage

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.android.signlanguage.model.languages.EnglishLanguage
import com.android.signlanguage.ui.lesson.LessonFragment

@BindingAdapter("sign")
fun bindSignImage(imgView: ImageView, sign: Char) {
    val drawable = EnglishLanguage.getDrawable(sign)
    imgView.setImageResource(drawable)
}