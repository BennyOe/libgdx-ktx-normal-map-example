package com.github.bennyOe.core.utils

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.Viewport

/**
 * Converts a world position to normalized screen space coordinates.
 *
 * @param world The world position as a [Vector3].
 * @param cam The [OrthographicCamera] used for projection.
 * @param viewport The [Viewport] defining the screen area.
 * @return The position in normalized screen space as a [Vector3].
 */
fun worldToScreenSpace(
    world: Vector3,
    cam: OrthographicCamera,
    viewport: Viewport
): Vector3 {
    val tmp = cam.project(world.cpy())
    tmp.x = (tmp.x - viewport.screenX) / viewport.screenWidth
    tmp.y = (tmp.y - viewport.screenY) / viewport.screenHeight
    return tmp
}
