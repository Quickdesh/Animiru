package eu.kanade.tachiyomi.data.track.shikimori

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.AnimeTrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class ShikimoriApi(private val client: OkHttpClient, interceptor: ShikimoriInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibAnime(track: AnimeTrack, user_id: String): AnimeTrack {
        return withIOContext {
            val payload = buildJsonObject {
                putJsonObject("user_rate") {
                    put("user_id", user_id)
                    put("target_id", track.media_id)
                    put("target_type", "Manga")
                    put("chapters", track.last_episode_seen.toInt())
                    put("score", track.score.toInt())
                    put("status", track.toShikimoriStatus())
                }
            }
            authClient.newCall(
                POST(
                    "$apiUrl/v2/user_rates",
                    body = payload.toString().toRequestBody(jsonMime)
                )
            ).await()
            track
        }
    }
    suspend fun updateLibAnime(track: AnimeTrack, user_id: String): AnimeTrack = addLibAnime(track, user_id)

    suspend fun searchAnime(search: String): List<AnimeTrackSearch> {
        return withIOContext {
            val url = "$apiUrl/animes".toUri().buildUpon()
                .appendQueryParameter("order", "popularity")
                .appendQueryParameter("search", search)
                .appendQueryParameter("limit", "20")
                .build()
            authClient.newCall(GET(url.toString()))
                .await()
                .parseAs<JsonArray>()
                .let { response ->
                    response.map {
                        jsonToAnimeSearch(it.jsonObject)
                    }
                }
        }
    }

    private fun jsonToAnimeSearch(obj: JsonObject): AnimeTrackSearch {
        return AnimeTrackSearch.create(TrackManager.SHIKIMORI).apply {
            media_id = obj["id"]!!.jsonPrimitive.int
            title = obj["name"]!!.jsonPrimitive.content
            total_episodes = obj["episodes"]!!.jsonPrimitive.int
            cover_url = baseUrl + obj["image"]!!.jsonObject["preview"]!!.jsonPrimitive.content
            summary = ""
            tracking_url = baseUrl + obj["url"]!!.jsonPrimitive.content
            publishing_status = obj["status"]!!.jsonPrimitive.content
            publishing_type = obj["kind"]!!.jsonPrimitive.content
            start_date = obj["aired_on"]!!.jsonPrimitive.contentOrNull ?: ""
        }
    }

    private fun jsonToAnimeTrack(obj: JsonObject, animes: JsonObject): AnimeTrack {
        return AnimeTrack.create(TrackManager.SHIKIMORI).apply {
            title = animes["name"]!!.jsonPrimitive.content
            media_id = obj["id"]!!.jsonPrimitive.int
            total_episodes = animes["episodes"]!!.jsonPrimitive.int
            last_episode_seen = obj["episodes"]!!.jsonPrimitive.float
            score = obj["score"]!!.jsonPrimitive.int.toFloat()
            status = toTrackStatus(obj["status"]!!.jsonPrimitive.content)
            tracking_url = baseUrl + animes["url"]!!.jsonPrimitive.content
        }
    }

    suspend fun findLibAnime(track: AnimeTrack, user_id: String): AnimeTrack? {
        return withIOContext {
            val urlAnimes = "$apiUrl/mangas".toUri().buildUpon()
                .appendPath(track.media_id.toString())
                .build()
            val animes = authClient.newCall(GET(urlAnimes.toString()))
                .await()
                .parseAs<JsonObject>()

            val url = "$apiUrl/v2/user_rates".toUri().buildUpon()
                .appendQueryParameter("user_id", user_id)
                .appendQueryParameter("target_id", track.media_id.toString())
                .appendQueryParameter("target_type", "Anime")
                .build()
            authClient.newCall(GET(url.toString()))
                .await()
                .parseAs<JsonArray>()
                .let { response ->
                    if (response.size > 1) {
                        throw Exception("Too much mangas in response")
                    }
                    val entry = response.map {
                        jsonToAnimeTrack(it.jsonObject, animes)
                    }
                    entry.firstOrNull()
                }
        }
    }

    fun getCurrentUser(): Int {
        return runBlocking {
            authClient.newCall(GET("$apiUrl/users/whoami"))
                .await()
                .parseAs<JsonObject>()
                .let {
                    it["id"]!!.jsonPrimitive.int
                }
        }
    }

    suspend fun accessToken(code: String): OAuth {
        return withIOContext {
            client.newCall(accessTokenRequest(code))
                .await()
                .parseAs()
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        oauthUrl,
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("redirect_uri", redirectUrl)
            .build()
    )

    companion object {
        private const val clientId = "tQLOaRzbA0gJ4WSlsq6sQcsRWAAk-t8RIhssui6fQ1w"
        private const val clientSecret = "95WTl3ePbcXJtVYkiWiP4bQUtJL9oGbbneqKZ6VOwhs"

        private const val baseUrl = "https://shikimori.one"
        private const val apiUrl = "$baseUrl/api"
        private const val oauthUrl = "$baseUrl/oauth/token"
        private const val loginUrl = "$baseUrl/oauth/authorize"

        private const val redirectUrl = "animiru://shikimori-auth"

        fun authUrl(): Uri =
            loginUrl.toUri().buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUrl)
                .appendQueryParameter("response_type", "code")
                .build()

        fun refreshTokenRequest(token: String) = POST(
            oauthUrl,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", token)
                .build()
        )
    }
}
