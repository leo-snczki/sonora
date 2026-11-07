package dev.leosnczki.sonora.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import dev.leosnczki.sonora.core.designsystem.catalog.CatalogWallpaper
import dev.leosnczki.sonora.core.designsystem.catalog.DesignCatalog

/** Debug builds only: the design system catalog over each fake wallpaper. */
class DesignCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CatalogWallpaper.entries.forEach { DesignCatalog(it) }
            }
        }
    }
}
