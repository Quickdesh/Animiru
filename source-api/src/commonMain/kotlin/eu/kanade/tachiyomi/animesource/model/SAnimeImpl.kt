@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

class SAnimeImpl : SAnime {

    override lateinit var url: String

    // AM (CUSTOM_INFORMATION) -->
    override var title: String = ""
    // <-- AM (CUSTOM_INFORMATION)

    override var artist: String? = null

    override var author: String? = null

    override var description: String? = null

    override var genre: String? = null

    override var status: Int = 0

    override var thumbnail_url: String? = null

    override var initialized: Boolean = false

    override var update_strategy: AnimeUpdateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE

    // AM (CUSTOM_INFORMATION) -->
    override val originalTitle: String
        get() = title
    override val originalAuthor: String?
        get() = author
    override val originalArtist: String?
        get() = artist
    override val originalDescription: String?
        get() = description
    override val originalGenre: String?
        get() = genre
    override val originalStatus: Int
        get() = status
    // <-- AM (CUSTOM_INFORMATION)
}
