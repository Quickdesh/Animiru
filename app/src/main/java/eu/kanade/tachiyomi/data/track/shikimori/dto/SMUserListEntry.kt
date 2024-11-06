package eu.kanade.tachiyomi.data.track.shikimori.dto

import eu.kanade.tachiyomi.data.database.models.anime.AnimeTrack
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi
import eu.kanade.tachiyomi.data.track.shikimori.toTrackStatus
import kotlinx.serialization.Serializable

@Serializable
data class SMUserListEntry(
    val id: Long,
    val chapters: Double,
    val episodes: Double,
    val score: Int,
    val status: String,
) {

    fun toAnimeTrack(trackId: Long, anime: SMEntry): AnimeTrack {
        return AnimeTrack.create(trackId).apply {
            title = anime.name
            remote_id = this@SMUserListEntry.id
            total_episodes = anime.chapters!!
            library_id = this@SMUserListEntry.id
            last_episode_seen = this@SMUserListEntry.episodes
            score = this@SMUserListEntry.score.toDouble()
            status = toTrackStatus(this@SMUserListEntry.status)
            tracking_url = ShikimoriApi.BASE_URL + anime.url
        }
    }
}
