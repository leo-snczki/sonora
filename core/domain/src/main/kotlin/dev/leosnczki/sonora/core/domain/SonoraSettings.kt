package dev.leosnczki.sonora.core.domain

/**
 * Everything the user can set. Each group has an override switch, like the original: while it is
 * off, the group's defaults apply and its own values are kept for when it is turned back on.
 * The [fluidStyle] sits outside the override: it is a choice of look, not fine-tuning.
 */
data class SonoraSettings(
    val fluidStyle: FluidStyle,
    val fluidOverride: Boolean,
    val fluidScale: Float,
    val speedMin: Float,
    val speedMax: Float,
    /** Set once the required setup steps were all done, so the setup card can shrink to what is missing. */
    val setupCompletedOnce: Boolean,
) {
    val effectiveFluid: FluidSettings
        get() = if (fluidOverride) FluidSettings(fluidStyle, fluidScale, speedMin, speedMax) else FluidSettings.Default.copy(style = fluidStyle)

    companion object {
        val Default = SonoraSettings(
            fluidStyle = FluidSettings.Default.style,
            fluidOverride = false,
            fluidScale = FluidSettings.Default.scale,
            speedMin = FluidSettings.Default.speedMin,
            speedMax = FluidSettings.Default.speedMax,
            setupCompletedOnce = false,
        )
    }
}
