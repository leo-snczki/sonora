package dev.leosnczki.sonora.feature.backdrops

import android.graphics.Bitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.leosnczki.sonora.core.domain.SquareCrop
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CropScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var used: SquareCrop? = null
    private var cancelled = false
    private val image = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)

    private fun show() = compose.setContent { CropScreen(image, SquareCrop(image.width, image.height), onUse = { used = it }, onCancel = { cancelled = true }) }

    @Test
    fun rotate_flip_and_zoom_are_labelled_buttons() {
        show()

        listOf("Rotate left", "Rotate right", "Flip", "Zoom in", "Zoom out").forEach {
            compose.onNodeWithContentDescription(it).assertHasClickAction()
        }
    }

    @Test
    fun using_the_crop_hands_over_what_was_set() {
        show()

        compose.onNodeWithContentDescription("Rotate right").performClick()
        compose.onNodeWithContentDescription("Flip").performClick()
        compose.onNodeWithContentDescription("Zoom in").performClick()
        compose.onNodeWithText("Use", ignoreCase = true).performClick()

        val crop = used!!
        assertEquals(90, crop.rotation)
        assertEquals(true, crop.flipped)
        assertEquals(true, crop.zoom > 1f)
    }

    @Test
    fun cancelling_hands_over_nothing() {
        show()

        compose.onNodeWithText("Cancel", ignoreCase = true).performClick()

        assertEquals(true, cancelled)
        assertNull(used)
    }
}
