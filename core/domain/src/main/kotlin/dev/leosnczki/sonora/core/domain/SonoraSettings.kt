package dev.leosnczki.sonora.core.domain

/**
 * Everything the user can set. Each group has an override switch, like the original: while it is
 * off, the group's defaults apply and its own values are kept for when it is turned back on.
 */
data class SonoraSettings(
    val fluidOverride: Boolean,
    val fluidScale: Float,
    val speedMin: Float,
    val speedMax: Float,
    /** Set once the required setup steps were all done, so the setup card can shrink to what is missing. */
    val setupCompletedOnce: Boolean,
) {
    val effectiveFluid: FluidSettings
        get() = if (fluidOverride) FluidSettings(fluidScale, speedMin, speedMax) else FluidSettings.Default

    companion object {
        val Default = SonoraSettings(
            fluidOverride = false,
            fluidScale = FluidSettings.Default.scale,
            speedMin = FluidSettings.Default.speedMin,
            speedMax = FluidSettings.Default.speedMax,
            setupCompletedOnce = false,
        )
    }
}
