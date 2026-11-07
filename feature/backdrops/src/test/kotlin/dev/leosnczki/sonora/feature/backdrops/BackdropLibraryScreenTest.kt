package dev.leosnczki.sonora.feature.backdrops

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.Backdrops
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackdropLibraryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : BackdropLibraryActions {
        override fun choose(id: BackdropId) { calls += "choose=${id.value}" }
        override fun saveAlbumArt() { calls += "save" }
        override fun delete(id: BackdropId) { calls += "delete=${id.value}" }
        override fun back() { calls += "back" }
    }

    private val mine = MineBackdrop(BackdropId("u1"), "Blue Album", image = null)

    /** TalkBack's way to reach an action that has no visible control of its own. */
    private fun runCustomAction(node: String, label: String) {
        val custom = compose.onNodeWithContentDescription(node).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { custom.single { it.label == label }.action() }
    }

    private fun show(state: BackdropLibraryState) = compose.setContent { BackdropLibraryScreen(state, actions) }

    @Test
    fun the_chosen_backdrop_is_marked_and_tapping_another_chooses_it() {
        show(BackdropLibraryState(chosen = Backdrops.Default))

        compose.onNodeWithContentDescription("Dusk")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "selected"))
        compose.onNodeWithContentDescription("Lagoon").performScrollTo().performClick()

        assertEquals(listOf("choose=builtin:lagoon"), calls)
    }

    @Test
    fun mine_has_an_empty_state_with_the_save_action() {
        show(BackdropLibraryState(chosen = Backdrops.Default, canSaveAlbumArt = true))
        compose.onNodeWithText("Mine", ignoreCase = true).performClick()

        compose.onNodeWithText("No saved Backdrops yet").assertIsDisplayed()
        compose.onNodeWithText("Save current Album Art", ignoreCase = true).performClick()

        assertEquals(listOf("save"), calls)
    }

    @Test
    fun saving_is_unavailable_with_a_reason_when_nothing_plays() {
        show(BackdropLibraryState(chosen = Backdrops.Default, canSaveAlbumArt = false))
        compose.onNodeWithText("Mine", ignoreCase = true).performClick()

        compose.onNodeWithText("Save current Album Art", ignoreCase = true).assertIsNotEnabled()
        compose.onNodeWithText("Play some music to save its Album Art").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deleting_asks_first_and_is_an_accessibility_action_too() {
        show(BackdropLibraryState(chosen = Backdrops.Default, mine = listOf(mine)))
        compose.onNodeWithText("Mine", ignoreCase = true).performClick()

        runCustomAction("Blue Album", "Delete")
        compose.onNodeWithText("Delete this Backdrop?").assertIsDisplayed()
        compose.onNodeWithText("DELETE").performClick()

        assertEquals(listOf("delete=u1"), calls)
    }

    @Test
    fun cancelling_the_delete_keeps_it() {
        show(BackdropLibraryState(chosen = Backdrops.Default, mine = listOf(mine)))
        compose.onNodeWithText("Mine", ignoreCase = true).performClick()

        runCustomAction("Blue Album", "Delete")
        compose.onNodeWithText("CANCEL").performClick()

        assertEquals(emptyList(), calls)
    }
}
