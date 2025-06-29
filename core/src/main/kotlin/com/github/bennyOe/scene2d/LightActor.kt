package com.github.bennyOe.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.github.bennyOe.core.GameLight

class LightActor(private val light: GameLight) : Actor() {
    init {
        when (light) {
            is GameLight.Point -> setPosition(light.position.x, light.position.y)
            is GameLight.Spot -> setPosition(light.position.x, light.position.y)
            else -> Unit
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        when (light) {
            is GameLight.Point -> light.position.set(x, y)
            is GameLight.Spot -> light.position.set(x, y)
            else -> Unit
        }

        light.update()
    }
}
