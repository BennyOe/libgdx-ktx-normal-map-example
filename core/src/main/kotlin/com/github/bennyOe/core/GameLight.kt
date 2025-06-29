package com.github.bennyOe.core

import box2dLight.ConeLight
import box2dLight.Light
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2

interface IGameLight {
    fun update()
}

sealed class GameLight(
    open val shaderLight: ShaderLight,
    open var b2dLight: Light,
    internal val baseIntensity: Float = shaderLight.intensity,
    internal val baseColor: Color = shaderLight.color,
    internal val baseDistance: Float = b2dLight.distance,

    ) : IGameLight {

    var effect: LightEffectType? = null
    val effectParams: LightEffectParameters = LightEffectParameters()

    internal var flickerTimer = 0f
    internal var elapsedTime = 0f
    internal val currentTargetColor = baseColor
    internal var currentTargetIntensity = baseIntensity

    abstract override fun update()

    var color: Color
        get() = shaderLight.color
        set(value) {
            shaderLight.color = value
            b2dLight.color = value
        }

    data class Directional(
        override val shaderLight: ShaderLight.Directional,
        override var b2dLight: Light,
    ) : GameLight(shaderLight, b2dLight) {
        var intensity: Float
            get() = shaderLight.intensity
            set(value) {
                shaderLight.intensity = value
            }


        var direction: Float
            get() = shaderLight.direction
            set(value) {
                shaderLight.direction = value
                b2dLight.direction = value
            }

        override fun update() {
            b2dLight.setColor(shaderLight.color.r, shaderLight.color.g, shaderLight.color.b, b2dLight.color.a)
        }
    }

    data class Point(
        override val shaderLight: ShaderLight.Point,
        override var b2dLight: Light,
        var shaderBalance: Float = 1.0f,
        var isFlickering: Boolean = true,
    ) : GameLight(shaderLight, b2dLight) {
        var position: Vector2
            get() = shaderLight.position
            set(value) {
                shaderLight.position = value
                b2dLight.position = value
            }

        // --- Independent Properties ---
        var shaderIntensity: Float
            get() = shaderLight.intensity
            set(value) {
                shaderLight.intensity = value
            }

        var distance: Float
            get() = b2dLight.distance
            set(value) {
                b2dLight.distance = value
                shaderLight.falloff = Falloff.fromDistance(value).toVector3()
            }

        override fun update() {
            b2dLight.setColor(shaderLight.color.r, shaderLight.color.g, shaderLight.color.b, b2dLight.color.a)
            b2dLight.position = shaderLight.position

            applyLightEffect(this)
        }
    }

    data class Spot(
        override val shaderLight: ShaderLight.Spot,
        override var b2dLight: Light,
        var shaderBalance: Float = 1.0f,
        var isFlickering: Boolean = false,
    ) : GameLight(shaderLight, b2dLight) {
        var position: Vector2
            get() = shaderLight.position
            set(value) {
                shaderLight.position = value
                b2dLight.position = value
            }

        var direction: Float
            get() = shaderLight.directionDegree
            set(value) {
                shaderLight.directionDegree = value
                b2dLight.direction = value
            }

        // --- Independent Properties ---
        var shaderIntensity: Float
            get() = shaderLight.intensity
            set(value) {
                shaderLight.intensity = value
            }

        var distance: Float
            get() = b2dLight.distance
            set(value) {
                b2dLight.distance = value
                shaderLight.falloff = Falloff.fromDistance(value).toVector3()
            }

        var coneDegree: Float
            get() = shaderLight.coneDegree
            set(value) {
                shaderLight.coneDegree = value
                (b2dLight as ConeLight).coneDegree = value / 2f
            }

        override fun update() {
            b2dLight.setColor(shaderLight.color.r, shaderLight.color.g, shaderLight.color.b, b2dLight.color.a)
            b2dLight.position = shaderLight.position

            applyLightEffect(this)
        }
    }
}
