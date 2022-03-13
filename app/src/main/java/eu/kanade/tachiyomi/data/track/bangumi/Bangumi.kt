package eu.kanade.tachiyomi.data.track.bangumi

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.injectLazy

class Bangumi(private val context: Context, id: Int) : TrackService(id) {

    private val json: Json by injectLazy()

    private val interceptor by lazy { BangumiInterceptor(this) }

    private val api by lazy { BangumiApi(client, interceptor) }

    @StringRes
    override fun nameRes() = R.string.tracker_bangumi

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun displayScore(track: AnimeTrack): String {
        return track.score.toInt().toString()
    }

    private suspend fun add(track: AnimeTrack): AnimeTrack {
        return api.addLibAnime(track)
    }

    override suspend fun update(track: AnimeTrack, didWatchEpisode: Boolean): AnimeTrack {
        if (track.status != COMPLETED) {
            if (didWatchEpisode) {
                if (track.last_episode_seen.toInt() == track.total_episodes && track.total_episodes > 0) {
                    track.status = COMPLETED
                } else {
                    track.status = WATCHING
                }
            }
        }

        return api.updateLibAnime(track)
    }

    override suspend fun bind(track: AnimeTrack, hasReadChapters: Boolean): AnimeTrack {
        val statusTrack = api.statusLibAnime(track)
        val remoteTrack = api.findLibAnime(track)
        return if (remoteTrack != null && statusTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id

            if (track.status != COMPLETED) {
                track.status = if (hasReadChapters) WATCHING else statusTrack.status
            }

            track.status = statusTrack.status
            track.score = statusTrack.score
            track.last_episode_seen = statusTrack.last_episode_seen
            track.total_episodes = remoteTrack.total_episodes
            refresh(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = if (hasReadChapters) WATCHING else PLANNING
            track.score = 0F
            add(track)
            update(track)
        }
    }

    override suspend fun searchAnime(query: String): List<AnimeTrackSearch> {
        return api.searchAnime(query)
    }

    override suspend fun refresh(track: AnimeTrack): AnimeTrack {
        val remoteStatusTrack = api.statusLibAnime(track)
        track.copyPersonalFrom(remoteStatusTrack!!)
        api.findLibAnime(track)?.let { remoteTrack ->
            track.total_episodes = remoteTrack.total_episodes
        }
        return track
    }

    override fun getLogo() = R.drawable.ic_tracker_bangumi

    override fun getLogoColor() = Color.rgb(240, 145, 153)

    override fun getStatusListAnime(): List<Int> {
        return listOf(WATCHING, COMPLETED, ON_HOLD, DROPPED, PLANNING)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            WATCHING -> getString(R.string.watching)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLANNING -> getString(R.string.plan_to_watch)
            else -> ""
        }
    }

    override fun getWatchingStatus(): Int = WATCHING

    override fun getRewatchingStatus(): Int = -1

    override fun getCompletionStatus(): Int = COMPLETED

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String) {
        try {
            val oauth = api.accessToken(code)
            interceptor.newAuth(oauth)
            saveCredentials(oauth.user_id.toString(), oauth.access_token)
        } catch (e: Throwable) {
            logout()
        }
    }

    fun saveToken(oauth: OAuth?) {
        preferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): OAuth? {
        return try {
            json.decodeFromString<OAuth>(preferences.trackToken(this).get())
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }

    companion object {
        const val WATCHING = 3
        const val COMPLETED = 2
        const val ON_HOLD = 4
        const val DROPPED = 5
        const val PLANNING = 1
    }
}
