package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.recents.RecentsTab
import tachiyomi.i18n.MR

enum class StartScreen(val titleRes: StringResource, val tab: Tab) {
    ANIME(MR.strings.label_library, AnimeLibraryTab),

    // AM (RECENTS) -->
    UPDATES(MR.strings.label_recent_updates, RecentsTab),
    HISTORY(MR.strings.label_recent_manga, RecentsTab),

    // <-- AM (RECENTS)
    // AM (BROWSE) -->
    BROWSE(MR.strings.browse, BrowseTab()),
    // <-- AM (BROWSE)
}
