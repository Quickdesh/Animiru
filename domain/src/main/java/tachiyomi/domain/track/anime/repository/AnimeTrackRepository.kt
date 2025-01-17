package tachiyomi.domain.track.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.track.anime.model.AnimeTrack

interface AnimeTrackRepository {

    suspend fun getTrackByAnimeId(id: Long): AnimeTrack?

    suspend fun getTracksByAnimeId(animeId: Long): List<AnimeTrack>

    fun getAnimeTracksAsFlow(): Flow<List<AnimeTrack>>

    fun getTracksByAnimeIdAsFlow(animeId: Long): Flow<List<AnimeTrack>>

    suspend fun delete(animeId: Long, trackerId: Long)

    suspend fun insertAnime(track: AnimeTrack)

    suspend fun insertAllAnime(tracks: List<AnimeTrack>)

    // AM (GROUPING) -->
    suspend fun getTracks(): List<AnimeTrack>
    // <-- AM (GROUPING)
}
