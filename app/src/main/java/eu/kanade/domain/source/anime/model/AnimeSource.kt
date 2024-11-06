package eu.kanade.domain.source.anime.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import tachiyomi.domain.source.anime.model.AnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

val AnimeSource.icon: ImageBitmap?
    get() {
        return Injekt.get<AnimeExtensionManager>().getAppIconForSource(id)
            ?.toBitmap()
            ?.asImageBitmap()
    }

// AM (BROWSE) -->
private val sourceIdToExtensionMap: MutableMap<Long, AnimeExtension.Installed> by lazy {
    val map = mutableMapOf<Long, AnimeExtension.Installed>()
    Injekt.get<AnimeExtensionManager>()
        .installedExtensionsFlow
        .value
        .forEach { ext ->
            ext.sources.forEach { source ->
                map[source.id] = ext
            }
        }
    map
}

fun updateSourceIdToExtensionMap() {
    sourceIdToExtensionMap.clear()
    Injekt.get<AnimeExtensionManager>()
        .installedExtensionsFlow
        .value
        .forEach { ext ->
            ext.sources.forEach { source ->
                sourceIdToExtensionMap[source.id] = ext
            }
        }
}

val AnimeSource.installedExtension: AnimeExtension.Installed?
    get() {
        return sourceIdToExtensionMap[id]
    }
// <-- AM (BROWSE)
