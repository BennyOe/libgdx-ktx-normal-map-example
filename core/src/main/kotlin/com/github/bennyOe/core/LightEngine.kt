package com.github.bennyOe.core

import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.Viewport

class LightEngine(
    rayHandler: RayHandler,
    cam: OrthographicCamera,
    batch: SpriteBatch,
    viewport: Viewport,
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
    fun renderLights(drawScene: (LightEngine) -> Unit) {
        batch.projectionMatrix = cam.combined
        viewport.apply()
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.shader = shader
        applyShaderUniforms()
        batch.begin()

        lastNormalMap = null
        lastSpecularMap = null
        drawScene(this)

        batch.end()
        batch.shader = null

        rayHandler.setCombinedMatrix(cam)
        rayHandler.updateAndRender()
    }

    /**
     * Draws a textured quad with a diffuse, a normal and a specular map, binding them to the correct texture units.
     *
     * This method binds the provided diffuse texture to texture unit 0, the normal map to texture unit 1 and the specular map to texture unit 2,
     * ensuring they are properly used by the lighting shader. It should only be called within the [renderLights] lambda.
     *
     * If the normal map or specular map differs from the previously used one, the batch is flushed to prevent texture conflicts.
     *
     * @param diffuse The diffuse texture (base color).
     * @param normals The corresponding normal map texture.
     * @param specular The corresponding specular map texture.
     * @param x The x-position in world coordinates.
     * @param y The y-position in world coordinates.
     * @param width The width of the quad to draw.
     * @param height The height of the quad to draw.
     */
    fun draw(
        diffuse: Texture,
        normals: Texture,
        specular: Texture,
        x: Float, y: Float,
        width: Float, height: Float,
    ) {
        if (lastNormalMap == null || normals != lastNormalMap) {
            batch.flush()
        }
        shader.bind()
        shader.setUniformi("u_useNormalMap", 1)
        shader.setUniformi("u_useSpecularMap", 1)

        if (lastSpecularMap == null || specular != lastSpecularMap) {
            batch.flush()
        }

        normals.bind(1)
        specular.bind(2)
        diffuse.bind(0)
        batch.draw(diffuse, x, y, width, height)
        lastNormalMap = normals
        lastSpecularMap = specular
    }

    /**
     * Draws a textured quad with a diffuse and normal map, binding them to the correct texture units.
     *
     * This method binds the provided diffuse texture to texture unit 0 and the normal map to texture unit 1,
     * ensuring they are properly used by the lighting shader. It should only be called within the [renderLights] lambda.
     *
     * If the normal map differs from the previously used one, the batch is flushed to prevent texture conflicts.
     *
     * @param diffuse The diffuse texture (base color).
     * @param normals The corresponding normal map texture.
     * @param x The x-position in world coordinates.
     * @param y The y-position in world coordinates.
     * @param width The width of the quad to draw.
     * @param height The height of the quad to draw.
     */
    fun draw(
        diffuse: Texture,
        normals: Texture,
        x: Float, y: Float,
        width: Float, height: Float,
    ) {
        if (lastNormalMap == null || normals != lastNormalMap) {
            batch.flush()
        }
        shader.bind()
        shader.setUniformi("u_useNormalMap", 1)
        shader.setUniformi("u_useSpecularMap", 0)

        normals.bind(1)
        diffuse.bind(0)
        batch.draw(diffuse, x, y, width, height)
        lastNormalMap = normals
        lastSpecularMap = null
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    /**
     * Draws a textured quad using only a diffuse texture, without normal mapping.
     *
     * This method disables normal mapping by informing the shader to ignore the normal map.
     * It is useful for objects that do not require dynamic lighting effects, such as UI elements,
     * background layers, or sprites meant to remain unaffected by lighting.
     *
     * This function must also be called within the [renderLights] lambda to ensure the shader context is valid.
     *
     * @param diffuse The diffuse texture (base color).
     * @param x The x-position in world coordinates.
     * @param y The y-position in world coordinates.
     * @param width The width of the quad to draw.
     * @param height The height of the quad to draw.
     */
    fun draw(diffuse: Texture, x: Float, y: Float, width: Float, height: Float) {
        if (lastNormalMap != null) {
            batch.flush()
        }

        shader.bind()
        shader.setUniformi("u_useNormalMap", 0)
        shader.setUniformi("u_useSpecularMap", 0)

        diffuse.bind(0)
        batch.draw(diffuse, x, y, width, height)
        lastNormalMap = null
        lastSpecularMap = null
    }

}
