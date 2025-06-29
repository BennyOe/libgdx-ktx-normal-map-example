package com.github.bennyOe

import com.github.bennyOe.demo.LightDemo
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync

class GgdxNormalMapExample : KtxGame<KtxScreen>() {
    override fun create() {
        KtxAsync.initiate()

        addScreen(LightDemo())
        setScreen<LightDemo>()
    }
}



