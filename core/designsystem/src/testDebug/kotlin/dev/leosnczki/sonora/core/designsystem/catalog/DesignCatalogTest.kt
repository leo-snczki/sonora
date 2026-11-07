package dev.leosnczki.sonora.core.designsystem.catalog

import android.graphics.Bitmap
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w900dp-h1400dp-xhdpi")
class DesignCatalogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun catalog_renders_every_component_over_a_white_wallpaper() {
        compose.setContent { DesignCatalog(CatalogWallpaper.White, Modifier.width(900.dp)) }

        listOf("PRIMARY", "SECONDARY", "GROUP, LOCKED", "Needs a permission first", "Empty state", "GROUP, OPEN")
            .forEach { compose.onNodeWithText(it, substring = true, ignoreCase = true).assertExists() }

        val out = System.getProperty("sonora.catalogOut") ?: return
        // captureToImage waits for a redraw that never settles with the spinner running; draw the view instead.
        val view = compose.activity.window.decorView
        val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(image)) }
        File(out, "catalog.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
