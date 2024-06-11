package tachiyomi.domain.history.anime.model

import tachiyomi.domain.entries.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.entries.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    // AM (CUSTOM_INFORMATION) -->
    val ogTitle: String,
    // <-- AM (CUSTOM_INFORMATION)
    val episodeNumber: Double,
    val seenAt: Date?,
    val coverData: AnimeCover,
) {
    // AM (CUSTOM_INFORMATION) -->
    val title: String = customAnimeManager.get(animeId)?.title ?: ogTitle

    companion object {
        private val customAnimeManager: GetCustomAnimeInfo by injectLazy()
    }
    // <-- AM (CUSTOM_INFORMATION)
}
