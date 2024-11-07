// AM (GROUPING) -->
package eu.kanade.tachiyomi.data.track

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.jellyfin.Jellyfin
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl
import tachiyomi.i18n.MR

enum class TrackStatus(val int: Int, val res: StringResource) {
    WATCHING(1, MR.strings.watching),
    REPEATING(2, MR.strings.repeating_anime),
    PLAN_TO_WATCH(3, MR.strings.plan_to_watch),
    PAUSED(4, MR.strings.on_hold),
    COMPLETED(5, MR.strings.completed),
    DROPPED(6, MR.strings.dropped),
    OTHER(7, MR.strings.not_tracked),
    ;

    companion object {
        fun parseTrackerStatus(trackerManager: TrackerManager, tracker: Long, status: Long): TrackStatus? {
            return when (tracker) {
                trackerManager.myAnimeList.id -> {
                    when (status) {
                        MyAnimeList.WATCHING -> WATCHING
                        MyAnimeList.COMPLETED -> COMPLETED
                        MyAnimeList.ON_HOLD -> PAUSED
                        MyAnimeList.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        MyAnimeList.DROPPED -> DROPPED
                        MyAnimeList.REWATCHING -> REPEATING
                        else -> null
                    }
                }
                trackerManager.aniList.id -> {
                    when (status) {
                        Anilist.WATCHING -> WATCHING
                        Anilist.COMPLETED -> COMPLETED
                        Anilist.ON_HOLD -> PAUSED
                        Anilist.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Anilist.DROPPED -> DROPPED
                        Anilist.REWATCHING -> REPEATING
                        else -> null
                    }
                }
                trackerManager.kitsu.id -> {
                    when (status) {
                        Kitsu.WATCHING -> WATCHING
                        Kitsu.COMPLETED -> COMPLETED
                        Kitsu.ON_HOLD -> PAUSED
                        Kitsu.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Kitsu.DROPPED -> DROPPED
                        else -> null
                    }
                }
                trackerManager.shikimori.id -> {
                    when (status) {
                        Shikimori.READING -> WATCHING
                        Shikimori.COMPLETED -> COMPLETED
                        Shikimori.ON_HOLD -> PAUSED
                        Shikimori.PLAN_TO_READ -> PLAN_TO_WATCH
                        Shikimori.DROPPED -> DROPPED
                        Shikimori.REREADING -> REPEATING
                        else -> null
                    }
                }
                trackerManager.bangumi.id -> {
                    when (status) {
                        Bangumi.READING -> WATCHING
                        Bangumi.COMPLETED -> COMPLETED
                        Bangumi.ON_HOLD -> PAUSED
                        Bangumi.PLAN_TO_READ -> PLAN_TO_WATCH
                        Bangumi.DROPPED -> DROPPED
                        else -> null
                    }
                }
                trackerManager.simkl.id -> {
                    when (status) {
                        Simkl.WATCHING -> WATCHING
                        Simkl.COMPLETED -> COMPLETED
                        Simkl.ON_HOLD -> PAUSED
                        Simkl.PLAN_TO_WATCH -> PLAN_TO_WATCH
                        Simkl.NOT_INTERESTING -> DROPPED
                        else -> null
                    }
                }
                trackerManager.jellyfin.id -> {
                    when (status) {
                        Jellyfin.WATCHING -> WATCHING
                        Jellyfin.COMPLETED -> COMPLETED
                        Jellyfin.UNSEEN -> PLAN_TO_WATCH
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}
// <-- AM (GROUPING)
