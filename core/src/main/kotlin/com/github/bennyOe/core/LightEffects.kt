package com.github.bennyOe.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import kotlin.math.sin

enum class LightEffectType {
    NONE,
    FIRE,
    PULSE,
    FAULTY_LAMP,
    LIGHTNING,
    COLOR_CYCLE
}

data class LightEffectParameters(
    var fireIntensity: Float = 1f,
    var pulseSpeed: Float = 2f,
    var pulseMinIntensity: Float = 0.5f,
    var pulseMaxIntensity: Float = 1.5f,
    var faultyLampChanceToFlicker: Float = 0.05f,
    var lightningMinDelay: Float = 3f,
    var lightningMaxDelay: Float = 8f,
    var colorCycleSpeed: Float = 20f,
)

/**
 * Applies the selected light effect to the given `GameLight` instance.
 *
 * This function should be called in the `update` method of the light.
 * It delegates to the appropriate effect implementation based on the `effect` property of the light.
 *
 * @param light The `GameLight` to which the effect will be applied.
 */
fun applyLightEffect(light: GameLight) {
    val params = light.effectParams
    when (light.effect) {
        LightEffectType.FIRE -> fire(light, params.fireIntensity)
        LightEffectType.PULSE -> pulse(light, params.pulseSpeed, params.pulseMinIntensity, params.pulseMaxIntensity)
        LightEffectType.FAULTY_LAMP -> faultyLamp(light, params.faultyLampChanceToFlicker)
        LightEffectType.LIGHTNING -> lightning(light, params.lightningMinDelay, params.lightningMaxDelay)
        LightEffectType.COLOR_CYCLE -> colorCycle(light, params.colorCycleSpeed)
        else -> Unit
    }
}

private fun fire(light: GameLight, intensity: Float) {
    val delta = Gdx.graphics.deltaTime
    light.flickerTimer -= delta

    if (light.flickerTimer <= 0f) {
        light.flickerTimer = (Math.random() * 0.12f + 0.08f).toFloat()

        val intensityVariation = (Math.random().toFloat() - 0.5f) * intensity * 0.5f
        light.currentTargetIntensity = (light.baseIntensity + intensityVariation).coerceIn(light.baseIntensity * 0.7f, light.baseIntensity * 1.3f)

        val rVariation = (Math.random().toFloat() - 0.5f) * 0.2f * intensity
        val newR = (light.baseColor.r + rVariation).coerceIn(0.8f, 1.0f)

        val gVariation = (Math.random().toFloat() - 0.5f) * 0.4f * intensity
        val newG = (light.baseColor.g + gVariation).coerceIn(0.3f, 0.5f)

        val newB = light.baseColor.b * 0.1f

        light.currentTargetColor.set(newR, newG, newB, light.baseColor.a)
    }

    val lerpAlpha = 0.5f
    light.shaderLight.intensity += (light.currentTargetIntensity - light.shaderLight.intensity) * lerpAlpha
    light.shaderLight.color.lerp(light.currentTargetColor, lerpAlpha)

    light.b2dLight.setColor(light.shaderLight.color.r, light.shaderLight.color.g, light.shaderLight.color.b, light.b2dLight.color.a)
    light.b2dLight.distance = light.baseDistance + (light.shaderLight.intensity - light.baseIntensity)
}

private fun pulse(light: GameLight, pulseSpeed: Float, minIntensity: Float, maxIntensity: Float) {
    light.elapsedTime += Gdx.graphics.deltaTime
    val sinValue = sin(light.elapsedTime * pulseSpeed)
    val normalizedValue = (sinValue + 1f) / 2f
    val targetIntensity = minIntensity + (maxIntensity - minIntensity) * normalizedValue
    light.shaderLight.intensity = targetIntensity
    light.b2dLight.distance = light.baseDistance * targetIntensity
}

private fun faultyLamp(light: GameLight, chanceToFlicker: Float) {
    if (Math.random() < chanceToFlicker) {
        light.shaderLight.intensity = light.baseIntensity * (1f + (Math.random() * 0.5f).toFloat())
        light.shaderLight.color.set(light.baseColor.r, light.baseColor.g, light.baseColor.b, 1f)
    } else {
        light.shaderLight.intensity = light.baseIntensity * 0.1f
        light.shaderLight.color.set(light.baseColor.r * 0.5f, light.baseColor.g * 0.5f, light.baseColor.b * 0.5f, 0.5f)
    }
    light.b2dLight.distance = light.baseDistance * light.shaderLight.intensity
    light.b2dLight.setColor(light.shaderLight.color)
}

private fun lightning(light: GameLight, minDelay: Float, maxDelay: Float) {
    light.flickerTimer -= Gdx.graphics.deltaTime
    if (light.flickerTimer > 0.1f) {
        light.shaderLight.intensity = 0f
    } else if (light.flickerTimer > 0f) {
        light.shaderLight.intensity = light.baseIntensity * 5f
        light.shaderLight.color = Color.WHITE
    } else {
        light.flickerTimer = (minDelay + Math.random() * (maxDelay - minDelay)).toFloat()
    }
    light.b2dLight.distance = light.baseDistance * light.shaderLight.intensity * 2f
    light.b2dLight.setColor(light.shaderLight.color)
}

private fun colorCycle(light: GameLight, cycleSpeed: Float) {
    light.elapsedTime += Gdx.graphics.deltaTime
    val hue = (light.elapsedTime * cycleSpeed) % 360f
    light.shaderLight.color.fromHsv(hue, 1.0f, 1.0f)
    val sinValue = sin(light.elapsedTime * 0.5f)
    light.shaderLight.intensity = light.baseIntensity + sinValue * 0.2f
    light.b2dLight.setColor(light.shaderLight.color)
    light.b2dLight.distance = light.baseDistance * light.shaderLight.intensity
}
