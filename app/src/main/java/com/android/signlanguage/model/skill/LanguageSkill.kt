package com.android.signlanguage.model.skill

class LanguageSkill(val language: Language) {
    val unlockedSigns: MutableList<SignSkill> = mutableListOf()
}