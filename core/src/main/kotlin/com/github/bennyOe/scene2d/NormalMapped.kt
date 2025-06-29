package com.github.bennyOe.scene2d

import com.badlogic.gdx.graphics.Texture

interface NormalMapped {
    val diffuseTexture: Texture
    val normalMapTexture: Texture?
    val specularTexture: Texture?
}
