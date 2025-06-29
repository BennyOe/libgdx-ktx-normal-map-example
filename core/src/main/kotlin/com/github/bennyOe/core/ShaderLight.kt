package com.github.bennyOe.core

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

sealed class ShaderLight {
    abstract var color: Color
    abstract var intensity: Float

    data class Directional(
        override var color: Color,
        override var intensity: Float,
        var direction: Float,
        var elevation: Float,
    ) : ShaderLight()

    data class Point(
        override var color: Color,
        override var intensity: Float,
        var position: Vector2,
        var falloff: Vector3,
    ) : ShaderLight()

    data class Spot(
        override var color: Color,
        override var intensity: Float,
        var position: Vector2,
        var falloff: Vector3,
        var directionDegree: Float,
        var coneDegree: Float,
    ) : ShaderLight()
}
