package com.github.bennyOe.scene2d

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor

class NormalMappedActor(
    override val diffuseTexture: Texture,
    override val normalMapTexture: Texture? = null,
    override val specularTexture: Texture? = null,
) : Actor(), NormalMapped {
    init {
        setSize(diffuseTexture.width.toFloat(), diffuseTexture.height.toFloat())
    }
}
