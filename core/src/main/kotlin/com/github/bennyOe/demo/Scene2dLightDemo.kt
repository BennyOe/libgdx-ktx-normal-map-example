package com.github.bennyOe.demo

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.bennyOe.core.GameLight
import com.github.bennyOe.core.Scene2dLightEngine
import com.github.bennyOe.scene2d.LightActor
import com.github.bennyOe.scene2d.NormalMappedActor
import ktx.math.vec2

/**
 * A lightweight demo showcasing how to set up and use the `Scene2dLightEngine` with libGDX's scene2d framework.
 *
 * This example demonstrates:
 * - Initializing a `Scene2dLightEngine` and integrating it with a `Stage`
 * - Adding different types of lights: directional, point, and spot lights
 * - Creating `LightActor` instances to visually represent and manipulate lights within the scene
 * - Updating light positions based on user input (e.g., moving a point light with the mouse)
 * - Rendering lights and scene2d actors together, including custom wall rendering
 * - Proper resource management and cleanup
 *
 * Useful as a starting point for integrating dynamic 2D lighting into scene2d-based games or applications.
 */
class Scene2dLightDemo : AbstractLightDemo() {
    private lateinit var stage: Stage
    private lateinit var lightEngine: Scene2dLightEngine

    private lateinit var directionalLight: GameLight.Directional
    private lateinit var pointLight: GameLight.Point
    private lateinit var spotLight: GameLight.Spot
    private lateinit var pointActor: LightActor
    private lateinit var spotActor: LightActor

    override fun show() {
        super.show()
        stage = Stage(viewport, batch)

        lightEngine = Scene2dLightEngine(rayHandler, cam, batch, viewport, stage)

        directionalLight = lightEngine.addDirectionalLight(
            Color(0.8f, 0.8f, 1f, 0.45f),
            -45f,
            2.8f,
            40f
        )

        pointLight = lightEngine.addPointLight(
            vec2(6f, 6f),
            Color(1f, 0.5f, 0.2f, 1f),
            2f,
            7f,
            1f,
            1f
        )
        pointActor = LightActor(pointLight)
        stage.addActor(pointActor)

        spotLight = lightEngine.addSpotLight(
            vec2(6f, 5f),
            Color(0.2f, 0.5f, 1f, 1f),
            0f,
            75f,
            4f,
            10f,
            0.5f,
            2f
        )
        spotActor = LightActor(spotLight)
        stage.addActor(spotActor)

        val wallActor = Image(wall)
        wallActor.setPosition(0f, 0f)
        wallActor.setSize(9f, 19f)
        stage.addActor(wallActor)

        val woodActor = NormalMappedActor(wood, woodNormals)
        woodActor.setPosition(9f, 0f)
        woodActor.setSize(9f, 19f)
        stage.addActor(woodActor)
    }

    override fun resize(width: Int, height: Int) {
        lightEngine.resize(width, height)
    }

    override fun render(delta: Float) {
        world.step(1 / 60f, 6, 2)
        cam.update()
        viewport.apply()

        val mouse = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        val worldPos = viewport.unproject(mouse)
        pointActor.setPosition(worldPos.x, worldPos.y)

        stage.act(delta)
        lightEngine.update()
        batch.projectionMatrix = cam.combined

        lightEngine.renderLights { engine ->
            for (actor in stage.actors) {
                    engine.draw(actor)
            }
        }

        debugRenderer.render(world, cam.combined)
    }

    override fun dispose() {
        lightEngine.dispose()
        stage.dispose()
        debugRenderer.dispose()
        super.dispose()
    }
}
