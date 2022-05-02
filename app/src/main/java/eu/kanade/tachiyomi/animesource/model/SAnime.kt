package eu.kanade.tachiyomi.animesource.model

import eu.kanade.tachiyomi.data.database.models.AnimeImpl
import tachiyomi.animesource.model.AnimeInfo
import java.io.Serializable

interface SAnime : Serializable {

    var url: String

    var title: String

    var artist: String?

    var author: String?

    var description: String?

    var genre: String?

    var status: Int

    var thumbnail_url: String?

    var initialized: Boolean

    val originalTitle: String
        get() = (this as? AnimeImpl)?.ogTitle ?: title
    val originalAuthor: String?
        get() = (this as? AnimeImpl)?.ogAuthor ?: author
    val originalArtist: String?
        get() = (this as? AnimeImpl)?.ogArtist ?: artist
    val originalDescription: String?
        get() = (this as? AnimeImpl)?.ogDesc ?: description
    val originalGenre: String?
        get() = (this as? AnimeImpl)?.ogGenre ?: genre
    val originalStatus: Int
        get() = (this as? AnimeImpl)?.ogStatus ?: status

    fun copyFrom(other: SAnime) {
        title = other.originalTitle

        if (other.author != null) {
            author = other.originalAuthor
        }

        if (other.artist != null) {
            artist = other.originalArtist
        }

        if (other.description != null) {
            description = other.originalDescription
        }

        if (other.genre != null) {
            genre = other.originalGenre
        }

        if (other.thumbnail_url != null) {
            thumbnail_url = other.thumbnail_url
        }

        status = other.status

        if (!initialized) {
            initialized = other.initialized
        }
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SAnime {
            return SAnimeImpl()
        }
    }
}

fun SAnime.toAnimeInfo(): AnimeInfo {
    return AnimeInfo(
        key = this.url,
        title = this.title,
        artist = this.artist ?: "",
        author = this.author ?: "",
        description = this.description ?: "",
        genres = this.genre?.split(", ") ?: emptyList(),
        status = this.status,
        cover = this.thumbnail_url ?: "",
    )
}

fun AnimeInfo.toSAnime(): SAnime {
    val animeInfo = this
    return SAnime.create().apply {
        url = animeInfo.key
        title = animeInfo.title
        artist = animeInfo.artist
        author = animeInfo.author
        description = animeInfo.description
        genre = animeInfo.genres.joinToString(", ")
        status = animeInfo.status
        thumbnail_url = animeInfo.cover
    }
}