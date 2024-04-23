package eu.kanade.tachiyomi.data.track

import android.content.Context
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.jellyfin.Jellyfin
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import eu.kanade.tachiyomi.data.track.simkl.Simkl

class TrackerManager(context: Context) {

    companion object {
        const val ANILIST = 2L
        const val KITSU = 3L
        const val SIMKL = 101L
        const val JELLYFIN = 102L
    }

    val myAnimeList = MyAnimeList(1L)
    val aniList = Anilist(ANILIST)
    val kitsu = Kitsu(KITSU)
    val shikimori = Shikimori(4L)
    val bangumi = Bangumi(5L)
    val simkl = Simkl(SIMKL)
    val jellyfin = Jellyfin(JELLYFIN)

    val trackers: List<BaseTracker> = listOf(
        myAnimeList,
        aniList,
        kitsu,
        shikimori,
        bangumi,
        simkl,
        jellyfin,
    )

    fun loggedInTrackers() = trackers.filter { it.isLoggedIn }

    fun get(id: Long) = trackers.find { it.id == id }
}
