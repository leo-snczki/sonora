package dev.leosnczki.sonora.core.domain

/** The last pass that puts the fluid on the screen. */
enum class GraphicsEffect {
    /** A light blur, the original's default. */
    Blur,

    /** Vertical strips of reeded glass. */
    FlutedGlass,
}

/** How the fluid is finished on screen. [brightness] runs from -1 (darker) to +1 (brighter). */
data class LookSettings(val effect: GraphicsEffect, val brightness: Float) {
    companion object {
        val Default = LookSettings(effect = GraphicsEffect.Blur, brightness = 0f)
    }
}

/**
 * Everything the user can set. Each group has an override switch, like the original: while it is
 * off, the group's defaults apply and its own values are kept for when it is turned back on.
 * The [fluidStyle] and Cover's orientation sit outside the override: they are choices of look,
 * not fine-tuning.
 */
data class SonoraSettings(
    val fluidStyle: FluidStyle,
    val coverOrientation: CoverOrientation,
    val fluidOverride: Boolean,
    val fluidScale: Float,
    val speedMin: Float,
    val speedMax: Float,
    val graphicsOverride: Boolean,
    val graphicsEffect: GraphicsEffect,
    val miscOverride: Boolean,
    val brightness: Float,
    val beatOverride: Boolean,
    /** 0..1; how strongly the fluid follows the beat. */
    val beatStrength: Float,
    val albumArtOverride: Boolean,
    val keepAfterPause: Boolean,
    val keepAfterReboot: Boolean,
    /** Shown when there is no Album Art to show. */
    val backdrop: BackdropId,
    /** Set once the required setup steps were all done, so the setup card can shrink to what is missing. */
    val setupCompletedOnce: Boolean,
) {
    val effectiveFluid: FluidSettings
        get() = if (fluidOverride) FluidSettings(fluidStyle, fluidScale, speedMin, speedMax) else FluidSettings.defaultsFor(fluidStyle)

    val effectiveLook: LookSettings
        get() = LookSettings(
            effect = if (graphicsOverride) graphicsEffect else LookSettings.Default.effect,
            brightness = if (miscOverride) brightness else LookSettings.Default.brightness,
        )

    /** Beat Pulse is off by default, like the original's Live Beats: no strength unless its group is on. */
    val effectiveBeatStrength: Float
        get() = if (beatOverride) beatStrength else 0f

    val effectiveAlbumArtKeep: AlbumArtKeep
        get() = if (albumArtOverride) AlbumArtKeep(keepAfterPause, keepAfterReboot) else AlbumArtKeep.Default

    companion object {
        val Default = SonoraSettings(
            fluidStyle = FluidSettings.Default.style,
            coverOrientation = CoverOrientation.Upright,
            fluidOverride = false,
            fluidScale = FluidSettings.Default.scale,
            speedMin = FluidSettings.Default.speedMin,
            speedMax = FluidSettings.Default.speedMax,
            graphicsOverride = false,
            graphicsEffect = LookSettings.Default.effect,
            miscOverride = false,
            brightness = LookSettings.Default.brightness,
            beatOverride = false,
            beatStrength = 0.5f,
            albumArtOverride = false,
            keepAfterPause = AlbumArtKeep.Default.afterPause,
            keepAfterReboot = AlbumArtKeep.Default.afterReboot,
            backdrop = Backdrops.Default,
            setupCompletedOnce = false,
        )
    }
}
