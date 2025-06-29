package com.github.bennyOe.core

import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.bennyOe.scene2d.NormalMappedActor

class Scene2dLightEngine(
    rayHandler: RayHandler,
    cam: OrthographicCamera,
    batch: SpriteBatch,
    viewport: Viewport,
    val stage: Stage?,
    useDiffuseLight: Boolean = true,
    maxShaderLights: Int = 20,
) : AbstractLightEngine(rayHandler, cam, batch, viewport, useDiffuseLight, maxShaderLights) {
    /**
     * Performs the complete lighting render pass using normal mapping and Box2D shadows.
     *
     * This function sets up the shader, uploads all light properties, invokes a user-provided lambda to render the
     * scene with lighting, and then renders Box2D-based shadows on top. It must be called once per frame.
     *
     * ### What this function does:
     * - Configures the batch with the shader and camera matrix.
     * - Applies lighting-related uniforms to the shader (light count, color, falloff, direction, etc.).
     * - Calls the [drawScene] lambda where you render all visible objects using your own draw logic.
     * - Renders Box2D shadows via [RayHandler].
     *
     * ### Requirements inside [drawScene]:
     * - **Normal map must be bound to texture unit 1** before calling `batch.draw(...)`.
     * - **Diffuse texture must be bound to texture unit 0** before calling `batch.draw(...)`.
     * - Use the batch normally for rendering your sprites — lighting will be automatically applied by the shader.
     *
     * @param drawScene Lambda in which your game scene should be rendered with lighting applied.
     */
    fun renderLights(drawScene: (Scene2dLightEngine) -> Unit) {
        batch.projectionMatrix = cam.combined
        viewport.apply()
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.shader = shader
        applyShaderUniforms()
        batch.begin()

        drawScene(this)

        batch.end()
        batch.shader = null

        rayHandler.setCombinedMatrix(cam)
        rayHandler.updateAndRender()
    }

    /**
     * Draws a Scene2D [Actor] with optional normal mapping.
     *
     * If the actor is a [NormalMappedActor], it binds the normal map to texture unit 1
     * and the diffuse texture to unit 0, and renders it with lighting effects applied.
     * If the actor is not normal-mapped, it disables normal mapping in the shader and
     * renders the actor using the standard [Actor.draw] method.
     *
     * This method must be called **within** the [renderLights] lambda to ensure lighting
     * uniforms and shader context are active.
     *
     * @param actor The [Actor] to render. May or may not have an associated normal map.
     */
    fun draw(actor: Actor) {
        if (actor is NormalMappedActor) {
            if (lastNormalMap == null || lastNormalMap != actor.normalMapTexture) {
                batch.flush()
            }
            shader.bind()
            shader.setUniformi("u_useNormalMap", 1)

            actor.normalMapTexture.bind(1)
            actor.diffuseTexture.bind(0)
            batch.draw(actor.diffuseTexture, actor.x, actor.y, actor.width, actor.height)
            lastNormalMap = actor.normalMapTexture
        } else {
            if (lastNormalMap != null) batch.flush()
            shader.bind()
            shader.setUniformi("u_useNormalMap", 0)
            actor.draw(batch, 1.0f)
            lastNormalMap = null
        }
    }

    override fun resize(width: Int, height: Int) {
        if (stage == null) return
        stage.viewport.update(width, height, true)
        super.resize(width, height)
    }
}
