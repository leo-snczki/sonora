package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import dev.leosnczki.sonora.core.domain.FluidStyle
import kotlin.math.max

/** What one frame should look like. Produced from the scene state, consumed by [SceneRenderer]. */
data class SceneFrame(
    val style: FluidStyle,
    /** Position of the fluid animation; advances faster when the music plays. */
    val phase: Float,
    /** 0 shows the previous image, 1 the current one. */
    val crossfade: Float,
    /** 0 calm, 1 turbulent. */
    val scale: Float,
    /** -1 darker .. +1 brighter. */
    val brightness: Float,
) {
    companion object {
        val Still = SceneFrame(style = FluidStyle.Flow, phase = 0f, crossfade = 1f, scale = 0.5f, brightness = 0f)
    }
}

/**
 * The wallpaper's GL pipeline (ADR-0001), one per [FluidStyle]. All calls on the thread that owns
 * the context.
 * - Cover: warp the image into a small target, blur it with a few Kawase passes, then draw it to
 *   the screen.
 * - Flow, ported from the original (ADR 0003): once per image, shrink it to 192px and blur it
 *   heavily; each frame, read it through the fbm field at the screen size over the density, then
 *   draw that to the screen with one light blur tap.
 */
class SceneRenderer {
    private var warp = 0
    private var kawase = 0
    private var compose = 0
    private var flowNoise = 0
    private var flowCompose = 0
    private var flowSource = 0
    private var flowImage: Array<RenderTarget> = emptyArray()
    private var flowImageIndex = 0
    private var flowTarget: RenderTarget? = null
    private var density = 1f
    private var vertexArray = 0
    private val images = IntArray(2) // 0 = current, 1 = previous
    private var imageAspect = floatArrayOf(1f, 1f)
    private var targets: Array<RenderTarget> = emptyArray()
    private var width = 1
    private var height = 1

    fun init() {
        warp = Gl.program(Shaders.VERTEX, Shaders.WARP)
        kawase = Gl.program(Shaders.VERTEX, Shaders.KAWASE)
        compose = Gl.program(Shaders.VERTEX, Shaders.COMPOSE)
        flowNoise = Gl.program(Shaders.VERTEX, Shaders.FLOW_NOISE)
        flowCompose = Gl.program(Shaders.VERTEX, Shaders.FLOW_COMPOSE)
        val arrays = IntArray(1)
        GLES30.glGenVertexArrays(1, arrays, 0)
        vertexArray = arrays[0]
        GLES30.glGenTextures(2, images, 0)
        images.forEach { Gl.setupTexture(it) }
        upload(images[0], null)
        upload(images[1], null)
        GLES30.glGenTextures(1, arrays, 0)
        flowSource = arrays[0]
        Gl.setupTexture(flowSource)
        flowImage = arrayOf(RenderTarget(FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE), RenderTarget(FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE))
        prepareFlowImage(null)
    }

    /**
     * Uploads the images to draw: [current] fades in over [previous]. A null image is black.
     * Bitmaps should already be small (≈512px): the blur hides detail anyway.
     */
    fun setImages(current: Bitmap?, previous: Bitmap?) {
        upload(images[0], current)
        upload(images[1], previous ?: current)
        imageAspect = floatArrayOf(aspectOf(current), aspectOf(previous ?: current))
        prepareFlowImage(current)
    }

    /**
     * Flow's image, as in the original: squashed to 512px (filtered) then 192px (nearest), then
     * eight Kawase passes at growing distances. Done for both styles, so switching is instant.
     */
    private fun prepareFlowImage(bitmap: Bitmap?) {
        val small = bitmap?.let {
            Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(it, 512, 512, true), FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE, false)
        }
        upload(flowSource, small)
        GLES30.glBindVertexArray(vertexArray)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glUseProgram(kawase)
        var source = flowSource
        FLOW_IMAGE_BLUR_DISTANCES.forEachIndexed { pass, distance ->
            val destination = pass % 2
            flowImage[destination].bind()
            Gl.bindTexture(kawase, "uSource", source, unit = 0)
            GLES30.glUniform2f(Gl.uniform(kawase, "uTexel"), 1f / FLOW_IMAGE_SIZE, 1f / FLOW_IMAGE_SIZE)
            GLES30.glUniform1f(Gl.uniform(kawase, "uDistance"), distance)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            source = flowImage[destination].texture
            flowImageIndex = destination
        }
    }

    private fun aspectOf(bitmap: Bitmap?) = if (bitmap == null) 1f else bitmap.width.toFloat() / bitmap.height

    private fun upload(texture: Int, bitmap: Bitmap?) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        if (bitmap == null) {
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, java.nio.ByteBuffer.allocateDirect(4))
        } else {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        }
    }

    /**
     * Call whenever the surface size changes: render targets follow the screen. [density] is the
     * display's (dp to px): Flow draws its fluid at the screen size in dp, like the original.
     */
    fun resize(width: Int, height: Int, density: Float = 1f) {
        this.width = max(1, width)
        this.height = max(1, height)
        // The original's fallback for odd densities.
        this.density = if (density < 1f) 2.5f else density
        targets.forEach { it.release() }
        flowTarget?.release()
        // The fluid is soft: a quarter of the screen resolution is plenty.
        val w = max(1, this.width / 4)
        val h = max(1, this.height / 4)
        targets = arrayOf(RenderTarget(w, h), RenderTarget(w, h))
        flowTarget = RenderTarget(max(1, (this.width / this.density).toInt()), max(1, (this.height / this.density).toInt()))
    }

    fun draw(frame: SceneFrame) {
        GLES30.glBindVertexArray(vertexArray)
        GLES30.glDisable(GLES30.GL_BLEND)
        when (frame.style) {
            FluidStyle.Flow -> drawFlow(frame)
            FluidStyle.Cover -> drawCover(frame)
        }
    }

    private fun drawFlow(frame: SceneFrame) {
        val fluid = flowTarget ?: return
        fluid.bind()
        GLES30.glUseProgram(flowNoise)
        Gl.bindTexture(flowNoise, "uImage", flowImage[flowImageIndex].texture, unit = 0)
        GLES30.glUniform1f(Gl.uniform(flowNoise, "uTime"), frame.phase)
        GLES30.glUniform2f(Gl.uniform(flowNoise, "uResolution"), fluid.width.toFloat(), fluid.height.toFloat())
        GLES30.glUniform1f(Gl.uniform(flowNoise, "uScale"), frame.scale.coerceIn(0f, 1f) * 1.2f + 0.4f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(flowCompose)
        Gl.bindTexture(flowCompose, "uSource", fluid.texture, unit = 0)
        // The original's quirk, kept for its look: the tap offset is 1.5 texels of the 192px image.
        val step = 1.5f / FLOW_IMAGE_SIZE
        GLES30.glUniform2f(Gl.uniform(flowCompose, "uStep"), step, step)
        GLES30.glUniform1f(Gl.uniform(flowCompose, "uBrightness"), frame.brightness.coerceIn(-1f, 1f))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
    }

    private fun drawCover(frame: SceneFrame) {

        // 1. Fluid warp into target 0.
        val screenAspect = width.toFloat() / height
        targets[0].bind()
        GLES30.glUseProgram(warp)
        Gl.bindTexture(warp, "uCurrent", images[0], unit = 0)
        Gl.bindTexture(warp, "uPrevious", images[1], unit = 1)
        GLES30.glUniform1f(Gl.uniform(warp, "uMix"), frame.crossfade.coerceIn(0f, 1f))
        GLES30.glUniform1f(Gl.uniform(warp, "uPhase"), frame.phase)
        GLES30.glUniform1f(Gl.uniform(warp, "uScale"), frame.scale.coerceIn(0f, 1f))
        val crop = cropFor(screenAspect, imageAspect[0])
        GLES30.glUniform2f(Gl.uniform(warp, "uCrop"), crop.first, crop.second)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        // 2. Kawase blur, ping-ponging between the two targets.
        GLES30.glUseProgram(kawase)
        var source = 0
        BLUR_DISTANCES.forEach { distance ->
            val destination = 1 - source
            targets[destination].bind()
            Gl.bindTexture(kawase, "uSource", targets[source].texture, unit = 0)
            GLES30.glUniform2f(Gl.uniform(kawase, "uTexel"), 1f / targets[source].width, 1f / targets[source].height)
            GLES30.glUniform1f(Gl.uniform(kawase, "uDistance"), distance)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            source = destination
        }

        // 3. To the screen.
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(compose)
        Gl.bindTexture(compose, "uSource", targets[source].texture, unit = 0)
        GLES30.glUniform1f(Gl.uniform(compose, "uBrightness"), frame.brightness.coerceIn(-1f, 1f))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
    }

    /** Scale applied to centred uv so the image covers the screen without stretching. */
    private fun cropFor(screenAspect: Float, imageAspect: Float): Pair<Float, Float> =
        if (screenAspect > imageAspect) 1f to imageAspect / screenAspect else screenAspect / imageAspect to 1f

    fun release() {
        targets.forEach { it.release() }
        targets = emptyArray()
        (flowImage + listOfNotNull(flowTarget)).forEach { it.release() }
        flowImage = emptyArray()
        flowTarget = null
        GLES30.glDeleteTextures(2, images, 0)
        GLES30.glDeleteTextures(1, intArrayOf(flowSource), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vertexArray), 0)
        listOf(warp, kawase, compose, flowNoise, flowCompose).forEach { GLES30.glDeleteProgram(it) }
    }

    private companion object {
        val BLUR_DISTANCES = floatArrayOf(0f, 1f, 2f, 2f, 3f)
        const val FLOW_IMAGE_SIZE = 192
        val FLOW_IMAGE_BLUR_DISTANCES = floatArrayOf(0f, 2f, 2f, 4f, 4f, 6f, 6f, 8f)
    }
}
