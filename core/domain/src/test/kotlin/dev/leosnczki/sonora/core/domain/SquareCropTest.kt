package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SquareCropTest {
    private val landscape = SquareCrop(imageWidth = 400, imageHeight = 200)

    @Test
    fun at_first_the_largest_centred_square_is_taken() {
        assertEquals(CropRect(left = 100f, top = 0f, size = 200f), landscape.sourceRect())
    }

    @Test
    fun zooming_in_takes_a_smaller_square_around_the_same_centre() {
        assertEquals(CropRect(left = 150f, top = 50f, size = 100f), landscape.zoomIn(2f).sourceRect())
    }

    @Test
    fun zoom_stays_between_one_and_four() {
        assertEquals(1f, landscape.zoomOut(2f).zoom)
        assertEquals(4f, landscape.zoomIn(10f).zoom)
    }

    @Test
    fun panning_never_leaves_the_image() {
        val panned = landscape.pan(dx = -1_000f, dy = 0f)

        assertEquals(CropRect(left = 0f, top = 0f, size = 200f), panned.sourceRect())
        assertEquals(CropRect(left = 200f, top = 0f, size = 200f), landscape.pan(dx = 1_000f, dy = 1_000f).sourceRect())
    }

    @Test
    fun rotating_a_quarter_turn_swaps_the_sides_the_crop_works_on() {
        val turned = landscape.rotateRight()

        assertEquals(90, turned.rotation)
        assertEquals(200 to 400, turned.width to turned.height)
        assertEquals(CropRect(left = 0f, top = 100f, size = 200f), turned.sourceRect())
    }

    @Test
    fun four_turns_either_way_come_back() {
        assertEquals(0, (1..4).fold(landscape) { crop, _ -> crop.rotateLeft() }.rotation)
        assertEquals(270, landscape.rotateLeft().rotation)
    }

    @Test
    fun flipping_twice_undoes_it() {
        assertTrue(landscape.flip().flipped)
        assertEquals(landscape, landscape.flip().flip())
    }
}
