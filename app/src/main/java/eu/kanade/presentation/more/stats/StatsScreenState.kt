package eu.kanade.presentation.more.stats

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.more.stats.data.StatsData

sealed class StatsScreenState {
    @Immutable
    object Loading : StatsScreenState()

    @Immutable
    data class SuccessAnime(
        val overview: StatsData.AnimeOverview,
        val titles: StatsData.AnimeTitles,
        val episodes: StatsData.Episodes,
        val trackers: StatsData.Trackers,
    ) : StatsScreenState()
}
