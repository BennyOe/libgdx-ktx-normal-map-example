package com.github.bennyOe.core

import box2dLight.ConeLight
import box2dLight.DirectionalLight
import box2dLight.PointLight
import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.viewport.Viewport
import com.github.bennyOe.core.utils.worldToScreenSpace
import ktx.assets.disposeSafely
import ktx.math.vec3
import ktx.math.vec4
import kotlin.math.cos
import kotlin.math.sin

abstract class AbstractLightEngine(
    val rayHandler: RayHandler,
    val cam: OrthographicCamera,
    val batch: SpriteBatch,
    val viewport: Viewport,
    val useDiffuseLight: Boolean,
    val maxShaderLights: Int = 20,
) {
    protected val vertShader: FileHandle = Gdx.files.internal("shader/light.vert")
    protected val fragShader: FileHandle = Gdx.files.internal("shader/light.frag")
    protected lateinit var shader: ShaderProgram
    protected lateinit var shaderAmbient: Color
    protected val lights = mutableListOf<GameLight>()
    protected val shaderLights get() = lights.take(maxShaderLights)
    protected var normalInfluenceValue: Float = 1f
    protected var lastNormalMap: Texture? = null
    private val density = Gdx.graphics.backBufferScale


    init {
        setupShader()
        RayHandler.useDiffuseLight(useDiffuseLight)
        setShaderAmbientLight(Color(1f, 1f, 1f, 0.2f))
        batch.shader = shader
    }

    private fun setupShader() {
        ShaderProgram.pedantic = false
        shader = ShaderProgram(vertShader, fragShader)

        if (!shader.isCompiled) {
            throw GdxRuntimeException("Could not compile shader: ${shader.log}")
        }

        shader.bind()
        shader.setUniformi("u_normals", 1)
    }

    /**
     * Adds an existing [GameLight] instance to the light engine.
     *
     * This method is useful if you have created a custom light elsewhere (e.g., from another system or serialized state)
     * and want to register it with the light engine for updates, rendering, and uniform synchronization.
     *
     * Unlike the dedicated creation methods ([addPointLight], [addDirectionalLight], [addSpotLight]), this function does
     * not create a new light but instead integrates a pre-existing one into the engine's rendering and update pipeline.
     *
     * Note: The added light must be compatible with the shader and Box2D lighting setup managed by this engine.
     *
     * @param light The [GameLight] instance to be managed and rendered by the engine.
     */
    fun addLight(light: GameLight) {
        if (lights.contains(light)) return

        val newB2dLight = when (light) {
            is GameLight.Directional -> DirectionalLight(
                rayHandler,
                light.b2dLight.rayNum,
                light.b2dLight.color,
                light.b2dLight.direction
            )

            is GameLight.Point -> PointLight(
                rayHandler,
                light.b2dLight.rayNum,
                light.b2dLight.color,
                light.b2dLight.distance,
                light.b2dLight
                    .position.x,
                light.b2dLight.position.y
            )

            is GameLight.Spot -> ConeLight(
                rayHandler,
                light.b2dLight.rayNum,
                light.b2dLight.color,
                light.b2dLight.distance,
                light.b2dLight.position.x,
                light.b2dLight.position.y,
                light.b2dLight.direction,
                (light.b2dLight as ConeLight).coneDegree
            )
        }
        light.b2dLight = newB2dLight

        lights.add(light)
    }

    /**
     * Adds a new directional light to the scene. This light simulates a distant light source,
     * like the sun, where all light rays are parallel.
     *
     * The light is composed of a [ShaderLight] for visual effects on sprites and a
     * [box2dLight.DirectionalLight] for interactions within the Box2D world.
     *
     * @param color The color of the light. The alpha component is multiplied by the intensity.
     * @param direction The direction of the light in degrees, where 0 degrees points to the right (along the positive X-axis).
     * @param shaderIntensity The brightness of the light. This value is multiplied with the color's alpha component.
     * @param elevation The elevation of the light source in degrees. An elevation of 0 means the light is parallel to the XY plane.
     * An elevation of 90 degrees would mean the light shines straight down from the Z-axis. This is used to calculate the 3D light vector for the shader.
     * @param rays The number of rays used for the Box2D light. More rays produce higher quality shadows but are more performance-intensive.
     * @return The created [GameLight.Directional] instance, which can be used to modify the light's properties later.
     */
    fun addDirectionalLight(
        color: Color,
        direction: Float,
        shaderIntensity: Float,
        elevation: Float = 1f,
        rays: Int = 128
    ): GameLight.Directional {
        val correctedDirection = -direction
        val shaderLight = ShaderLight.Directional(
            color = color,
            intensity = shaderIntensity,
            direction = correctedDirection,
            elevation = elevation,
        )
        val b2dLight = DirectionalLight(
            rayHandler,
            rays,
            color,
            correctedDirection,
        )

        val gameLight = GameLight.Directional(shaderLight, b2dLight)

        lights.add(gameLight)
        return gameLight
    }

    /**
     * Adds a new point light to the scene. This light emanates from a single
     * point in all directions.
     *
     * The light is composed of a [ShaderLight.Point] for visual effects on sprites and a
     * [box2dLight.PointLight] for interactions within the Box2D world.
     *
     * @param position The world position of the light source.
     * @param color The color of the light. The alpha component is multiplied by the intensity.
     * @param shaderIntensity The base intensity of the light, affecting both the visual shader and the b2dLight.
     * @param b2dDistance The maximum range of the light. This defines the radius for shadow casting and the falloff calculation.
     * @param falloffProfile A value between 0.0 and 1.0 that controls the shape of the light's falloff. 0.0 is more linear, 1.0 is strongly quadratic.
     * @param shaderBalance A multiplier to fine-tune the visual intensity of the shader light relative to the b2dLight's base intensity.
     * @param rays The number of rays used for the Box2D light. More rays produce higher quality shadows but are more performance-intensive.
     * @return The created [GameLight.Point] instance, which can be used to modify the light's properties later.
     */
    fun addPointLight(
        position: Vector2,
        color: Color,
        shaderIntensity: Float = 1f,
        b2dDistance: Float = 1f,
        falloffProfile: Float = 0.5f,
        shaderBalance: Float = 0.5f,
        rays: Int = 128,
    ): GameLight.Point {
        val falloff = Falloff.fromDistance(b2dDistance, falloffProfile).toVector3()

        val shaderLight = ShaderLight.Point(
            color = color,
            intensity = shaderIntensity,
            position = position,
            falloff = falloff,
        )
        val b2dLight = PointLight(
            rayHandler,
            rays,
            color,
            b2dDistance,
            position.x,
            position.y
        )

        val gameLight = GameLight.Point(shaderLight, b2dLight, shaderBalance)

        lights.add(gameLight)
        return gameLight
    }

    /**
     * Adds a new spotlight to the scene, which emits light in a cone shape from a specific point.
     *
     * The light is composed of a [ShaderLight.Spot] for visual effects on sprites and a
     * [box2dLight.ConeLight] for interactions within the Box2D world.
     *
     * @param position The world position of the light source.
     * @param color The color of the light. The alpha component is multiplied by the intensity.
     * @param direction The direction the light is pointing in degrees (e.g., 0 is right, 90 is up).
     * @param coneDegree The **full** angle of the light cone in degrees. A value of 60 creates a 60-degree wide cone.
     * @param shaderIntensity The base intensity of the light, affecting both the visual shader and the b2dLight.
     * @param b2dDistance The maximum range of the light. This defines the radius for shadow casting and the falloff calculation.
     * @param falloffProfile A value between 0.0 and 1.0 that controls the shape of the light's falloff. 0.0 is more linear, 1.0 is strongly quadratic.
     * @param shaderBalance A multiplier to fine-tune the visual intensity of the shader light relative to the b2dLight's base intensity.
     * @param rays The number of rays used for the Box2D light. More rays produce higher quality shadows but are more performance-intensive.
     * @return The created [GameLight.Spot] instance, which can be used to modify the light's properties later.
     */
    fun addSpotLight(
        position: Vector2,
        color: Color,
        direction: Float,
        coneDegree: Float,
        shaderIntensity: Float = 1f,
        b2dDistance: Float = 1f,
        falloffProfile: Float = 0f,
        shaderBalance: Float = 0.5f,
        rays: Int = 128,
    ): GameLight.Spot {
        val falloff = Falloff.fromDistance(b2dDistance, falloffProfile).toVector3()

        val shaderLight = ShaderLight.Spot(
            color = color,
            intensity = shaderIntensity,
            position = position,
            falloff = falloff,
            directionDegree = direction,
            coneDegree = coneDegree,
        )
        val b2dLight = ConeLight(
            rayHandler,
            rays,
            color,
            b2dDistance,
            position.x,
            position.y,
            direction,
            coneDegree / 2,
        )

        val gameLight = GameLight.Spot(shaderLight, b2dLight, shaderBalance)

        lights.add(gameLight)
        return gameLight

    }

    /**
     * Sets how strongly the normal map influences the lighting effect.
     * @param normalInfluenceValue A value from 0.0 (no influence, flat lighting) to 1.0 (full influence).
     */
    fun setNormalInfluence(normalInfluenceValue: Float) {
        this.normalInfluenceValue = normalInfluenceValue
    }

    /**
     * Removes a specific light from the engine.
     * @param light The [GameLight] instance to remove.
     */
    fun removeLight(light: GameLight) {
        light.b2dLight.remove()
        lights.remove(light)
        shader.bind()
        shader.setUniformi("lightCount", lights.size)
    }

    /**
     * Removes all dynamic lights from the engine.
     */
    fun clearLights() {
        lights.clear()
        rayHandler.removeAll()
        shader.bind()
        shader.setUniformi("lightCount", 0)
    }

    /**
     * Sets the ambient light for the scene in the shader.
     * This is the base light color and intensity that affects all objects,
     * regardless of dynamic lights.
     * @param ambient The [Color] to use for ambient light. The color's alpha component acts as the intensity.
     */
    fun setShaderAmbientLight(ambient: Color) {
        shaderAmbient = ambient
    }

    /**
     * Updates the state of all lights. This method should be called once per frame.
     *
     * It iterates through all [GameLight] instances and synchronizes their properties (like color, position, or distance)
     * with the underlying Box2D light objects. This ensures that any changes made to the lights
     * are applied before they are rendered.
     */
    fun update() = lights.forEach { it.update() }


    /**
     * Updates and binds all uniform values required by the lighting shader.
     *
     * This method should be called before each render pass to ensure the shader receives
     * up-to-date information about the current lights, ambient settings, screen size, and normal influence.
     *
     * ### What this includes:
     * - Ambient light color and intensity via `ambient`.
     * - Number of active shader lights via `lightCount`.
     * - Normal influence strength via `normalInfluence`.
     * - Viewport offset and size in pixels (used for screen-space calculations).
     * - For each light:
     *   - `lightType` — 0 = directional, 1 = point, 2 = spot.
     *   - `lightColor` — light color * intensity.
     *   - `lightDir` — light direction vector (for directional and spot lights).
     *   - `lightPos` — light position in screen space (for point and spot lights).
     *   - `falloff` — attenuation values (for point and spot lights).
     *   - `coneAngle` — cosine of half cone angle (for spot lights only).
     *
     * Notes:
     * - Only the first [maxShaderLights] lights are considered for shader lighting.
     * - This method assumes the shader is already bound and `batch.shader` is not null.
     */
    protected fun applyShaderUniforms() {
        val shader = batch.shader ?: return
        shader.bind()
        shader.setUniformi("lightCount", shaderLights.size)
        shader.setUniformf("normalInfluence", normalInfluenceValue)
        shader.setUniformf("ambient", shaderAmbient)


        // Scale the viewport uniforms to match the physical pixel space of gl_FragCoord.
        val screenX = viewport.screenX * density
        val screenY = viewport.screenY * density
        val screenW = viewport.screenWidth * density
        val screenH = viewport.screenHeight * density

        shader.setUniformf("u_viewportOffset", screenX, screenY)
        shader.setUniformf("u_viewportSize", screenW, screenH)

        for (i in shaderLights.indices) {
            val gameLight = lights[i]
            val data = lights[i].shaderLight
            val prefix = "[$i]"
            shader.setUniformf("lightColor$prefix", vec4(data.color.r, data.color.g, data.color.b, data.color.a * data.intensity))

            when (data) {
                is ShaderLight.Directional -> {
                    shader.setUniformi("lightType$prefix", 0)

                    val dirRad = Math.toRadians(data.direction.toDouble()).toFloat()
                    val eleRad = Math.toRadians(data.elevation.toDouble()).toFloat()

                    val directionVector = vec3(
                        cos(dirRad) * cos(eleRad),
                        sin(dirRad) * cos(eleRad),
                        sin(eleRad)
                    ).nor()

                    shader.setUniformf("lightDir$prefix", directionVector)
                }

                is ShaderLight.Point -> {
                    shader.setUniformi("lightType$prefix", 1)

                    val pointLight = gameLight as GameLight.Point
                    val shaderIntensity = data.intensity * pointLight.shaderBalance
                    shader.setUniformf("lightColor$prefix", vec4(data.color.r, data.color.g, data.color.b, data.color.a * shaderIntensity))

                    val screenPos = worldToScreenSpace(vec3(data.position.x, data.position.y, 0f), cam, viewport)
                    shader.setUniformf("lightPos[$i]", screenPos)
                    shader.setUniformf("falloff$prefix", data.falloff)
                }

                is ShaderLight.Spot -> {
                    shader.setUniformi("lightType$prefix", 2)

                    val pointLight = gameLight as GameLight.Spot
                    val shaderIntensity = data.intensity * pointLight.shaderBalance
                    shader.setUniformf("lightColor$prefix", vec4(data.color.r, data.color.g, data.color.b, data.color.a * shaderIntensity))

                    val screenPos = worldToScreenSpace(vec3(data.position.x, data.position.y, 0f), cam, viewport)
                    shader.setUniformf("lightPos[$i]", screenPos)
                    shader.setUniformf("falloff$prefix", data.falloff)

                    val rad = Math.toRadians(data.directionDegree.toDouble()).toFloat()
                    val directionVector = vec3(cos(rad), sin(rad), 0f)

                    shader.setUniformf("lightDir$prefix", directionVector)
                    shader.setUniformf("coneAngle$prefix", cos(Math.toRadians(data.coneDegree.toDouble() * 0.5)).toFloat())
                }
            }
        }
    }

    open fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        val scale = Gdx.graphics.backBufferScale
        rayHandler.setCombinedMatrix(cam)
        shader.bind()
        shader.setUniformf("resolution", width.toFloat() * scale, height.toFloat() * scale)
    }

    fun dispose() {
        rayHandler.disposeSafely()
        shader.disposeSafely()
    }

}
