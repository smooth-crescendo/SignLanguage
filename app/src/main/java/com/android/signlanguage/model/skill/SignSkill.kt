package com.android.signlanguage.model.skill

import kotlin.math.log

class SignSkill(var sign: Char, var step: Int = 0) {

    val skill: Double
        get() {
            var eq = (log((0.5 + step.toDouble()) * 2, 1.16)) / 30
            if (eq > 1.0)
                eq = 1.0
            return eq
        }
}