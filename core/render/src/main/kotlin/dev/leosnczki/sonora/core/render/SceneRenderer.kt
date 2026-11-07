package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import dev.leosnczki.sonora.core.domain.CoverOrientation
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.SceneDirector
import kotlin.math.max

/** What one frame should look like. Produced from the scene state, consumed by [SceneRenderer]. */
data class SceneFrame(
    val style: FluidStyle,
    /** The last pass that puts the fluid on the screen. */
    val effect: GraphicsEffect,
    /** Which way Cover shows the image. */
    val orientation: CoverOrientation,
    /** Position of the fluid animation; advances faster when the music plays. */
    val phase: Float,
    /** 0 shows the previous image, 1 the current one. */
    val crossfade: Float,
    /** 0 calm, 1 turbulent. */
    val scale: Float,
    /** -1 darker .. +1 brighter. */
    val brightness: Float,
    /** How far in RGB Flow's colours may move towards a new image this frame. */
    val flowStep: Float,
) {
    companion object {
        val Still = SceneFrame(style = FluidStyle.Flow, effect = GraphicsEffect.Blur, orientation = CoverOrientation.Upright, phase = 0f, crossfade = 1f, scale = 0.5f, brightness = 0f, flowStep = SceneDirector.FLOW_SNAP)
    }
}

/**
 * The wallpaper's GL pipeline (ADR-0001), one per [FluidStyle]. All calls on the thread that owns
 * the context.
 * - Cover: warp the image into a small target, blur it with a few Kawase passes, then draw it to
 *   the screen.
 * - Flow, ported from the original (ADR-0003): once per image, shrink it to 192px and blur it
 *   heavily; each frame, walk the shown colours a step towards it, read them through the fbm
 *   field at the screen size over the density, then draw that to the screen with one light blur
 *   tap.
 */
class SceneRenderer {
    private var warp = 0
    private var kawase = 0
    private var compose = 0
    private var flowNoise = 0
    private var flowCompose = 0
    private var flowFade = 0
    private var flutedGlass = 0
    /** The brightness Flow's image was last pre-blurred with: the original multiplies in every pass. */
    private var flowBrightness = 0f
    /** The colours Flow shows, ping-ponged by the fade: [flowShownIndex] is the latest. */
    private var flowShown: Array<RenderTarget> = emptyArray()
    private var flowShownIndex = 0
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
        flowFade = Gl.program(Shaders.VERTEX, Shaders.FLOW_FADE)
        flutedGlass = Gl.program(Shaders.VERTEX, Shaders.FLUTED_GLASS)
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
        flowShown = arrayOf(RenderTarget(FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE), RenderTarget(FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE))
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
            val squashed = Bitmap.createScaledBitmap(it, FLOW_SQUASH_SIZE, FLOW_SQUASH_SIZE, true)
            Bitmap.createScaledBitmap(squashed, FLOW_IMAGE_SIZE, FLOW_IMAGE_SIZE, false)
        }
        upload(flowSource, small)
        blurFlowImage()
    }

    /** The original's pre-blur, with its brightness multiply in every pass. */
    private fun blurFlowImage() {
        GLES30.glBindVertexArray(vertexArray)
        GLES30.glDisable(GLES30.GL_BLEND)
        val gain = flowGain(flowBrightness)
        flowImageIndex = blur(flowSource, flowImage, FLOW_IMAGE_BLUR_DISTANCES, firstDestination = 0, gain = gain)
    }

    /**
     * Kawase passes at [distances], from [source] into the [pair] of same-sized targets in turn,
     * starting with [firstDestination]. Returns the index in [pair] of the target with the result.
     */
    private fun blur(source: Int, pair: Array<RenderTarget>, distances: FloatArray, firstDestination: Int, gain: Float = 1f): Int {
        GLES30.glUseProgram(kawase)
        GLES30.glUniform1f(Gl.uniform(kawase, "uGain"), gain)
        var texture = source
        var destination = firstDestination
        distances.forEach { distance ->
            val target = pair[destination]
            target.bind()
            Gl.bindTexture(kawase, "uSource", texture, unit = 0)
            GLES30.glUniform2f(Gl.uniform(kawase, "uTexel"), 1f / target.width, 1f / target.height)
            GLES30.glUniform1f(Gl.uniform(kawase, "uDistance"), distance)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            texture = target.texture
            destination = 1 - destination
        }
        return 1 - destination
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
        this.density = if (density < 1f) FLOW_FALLBACK_DENSITY else density
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
        val brightness = frame.brightness.coerceIn(-1f, 1f)
        if (brightness != flowBrightness) {
            // The new pre-blurred image becomes the target: the fade walks the colours to it.
            flowBrightness = brightness
            blurFlowImage()
        }
        // In both styles, so Flow's colours are current whenever the style switches to it.
        fadeFlow(frame.flowStep)
        when (frame.style) {
            FluidStyle.Flow -> drawFlow(frame)
            FluidStyle.Cover -> drawCover(frame)
        }
    }

    private fun fadeFlow(step: Float) {
        val next = 1 - flowShownIndex
        flowShown[next].bind()
        GLES30.glUseProgram(flowFade)
        Gl.bindTexture(flowFade, "uShown", flowShown[flowShownIndex].texture, unit = 0)
        Gl.bindTexture(flowFade, "uTarget", flowImage[flowImageIndex].texture, unit = 1)
        GLES30.glUniform1f(Gl.uniform(flowFade, "uStep"), step)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        flowShownIndex = next
    }

    private fun drawFlow(frame: SceneFrame) {
        val fluid = flowTarget ?: return
        fluid.bind()
        GLES30.glUseProgram(flowNoise)
        Gl.bindTexture(flowNoise, "uImage", flowShown[flowShownIndex].texture, unit = 0)
        GLES30.glUniform1f(Gl.uniform(flowNoise, "uTime"), frame.phase)
        GLES30.glUniform2f(Gl.uniform(flowNoise, "uResolution"), fluid.width.toFloat(), fluid.height.toFloat())
        GLES30.glUniform1f(Gl.uniform(flowNoise, "uScale"), flowScale(frame.scale))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        if (frame.effect == GraphicsEffect.FlutedGlass) return drawFlutedGlass(fluid.texture)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(flowCompose)
        Gl.bindTexture(flowCompose, "uSource", fluid.texture, unit = 0)
        // The original's quirk, kept for its look: the tap offset is 1.5 texels of the 192px image.
        val step = 1.5f / FLOW_IMAGE_SIZE
        GLES30.glUniform2f(Gl.uniform(flowCompose, "uStep"), step, step)
        GLES30.glUniform1f(Gl.uniform(flowCompose, "uGain"), flowGain(flowBrightness))
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
        GLES30.glUniform2f(
            Gl.uniform(warp, "uFlip"),
            if (frame.orientation.mirrorHorizontal) 1f else 0f,
            if (frame.orientation.flipVertical) 1f else 0f,
        )
        GLES30.glUniform1f(Gl.uniform(warp, "uBrightness"), frame.brightness.coerceIn(-1f, 1f))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

        // 2. Kawase blur, ping-ponging between the two targets.
        val source = blur(targets[0].texture, targets, BLUR_DISTANCES, firstDestination = 1)

        // 3. To the screen.
        if (frame.effect == GraphicsEffect.FlutedGlass) return drawFlutedGlass(targets[source].texture)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(compose)
        Gl.bindTexture(compose, "uSource", targets[source].texture, unit = 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
    }

    /** The Fluted Glass effect from [source] to the screen, as in the original: 16 strips per screen height. */
    private fun drawFlutedGlass(source: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        GLES30.glUseProgram(flutedGlass)
        Gl.bindTexture(flutedGlass, "uSource", source, unit = 0)
        GLES30.glUniform1f(Gl.uniform(flutedGlass, "uSegments"), 16f * width / height)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
    }

    /** Scale applied to centred uv so the image covers the screen without stretching. */
    private fun cropFor(screenAspect: Float, imageAspect: Float): Pair<Float, Float> =
        if (screenAspect > imageAspect) 1f to imageAspect / screenAspect else screenAspect / imageAspect to 1f

    fun release() {
        targets.forEach { it.release() }
        targets = emptyArray()
        (flowImage + flowShown + listOfNotNull(flowTarget)).forEach { it.release() }
        flowImage = emptyArray()
        flowShown = emptyArray()
        flowTarget = null
        GLES30.glDeleteTextures(2, images, 0)
        GLES30.glDeleteTextures(1, intArrayOf(flowSource), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vertexArray), 0)
        listOf(warp, kawase, compose, flowNoise, flowCompose, flowFade, flutedGlass).forEach { GLES30.glDeleteProgram(it) }
    }

    private companion object {
        val BLUR_DISTANCES = floatArrayOf(0f, 1f, 2f, 2f, 3f)
        const val FLOW_IMAGE_SIZE = 192

        /** The original first squashes the image to this square, filtered, then shrinks it unfiltered. */
        const val FLOW_SQUASH_SIZE = 512

        /** The original's density for displays that report less than 1. */
        const val FLOW_FALLBACK_DENSITY = 2.5f

        /** The original's brightness multiply, applied in each pass: ×(1 + 0.05·b). */
        fun flowGain(brightness: Float) = 1f + brightness * 0.05f

        /** The original's mapping from the 0..1 scale setting to how wide a window of the image is read. */
        fun flowScale(scale: Float) = scale.coerceIn(0f, 1f) * 1.2f + 0.4f
        val FLOW_IMAGE_BLUR_DISTANCES = floatArrayOf(0f, 2f, 2f, 4f, 4f, 6f, 6f, 8f)
    }
}
