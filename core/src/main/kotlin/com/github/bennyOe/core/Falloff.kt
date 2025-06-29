package com.github.bennyOe.core

import com.badlogic.gdx.math.Vector3
import ktx.math.vec3

data class Falloff(val constant: Float, val linear: Float, val quadratic: Float) {
    fun toVector3(): Vector3 = vec3(constant, linear, quadratic)

    companion object {
        /**
         * Computes continuous falloff coefficients based on a given distance.
         *
         * @param distance The distance at which the light is considered to effectively end.
         * @param profile A parameter (from 0.0 to 1.0) controlling the shape of the falloff curve.
         *                0.0 = more linear, 1.0 = more quadratic.
         */
        fun fromDistance(distance: Float, profile: Float = 0.5f): Falloff {
            if (distance <= 0f) return Falloff(1f, 0f, 0f)

            val constant = 1.0f
            // We want the brightness to be approximately 1/256 at the given 'distance'.
            // 255 = linear * distance + quadratic * distance^2
            // This is an equation with two unknowns. The 'profile' parameter controls
            // the ratio between linear and quadratic falloff.
            val quadratic = (255f * profile) / (distance * distance)
            val linear = (255f * (1f - profile)) / distance

            return Falloff(constant, linear, quadratic)
        }
        val DEFAULT = fromDistance(30f) // default value with a middle distance
    }
}
