package tachiyomi.data.entries.anime

import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.LibraryAnime

object AnimeMapper {
    fun mapAnime(
        id: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
    ): Anime = Anime(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        episodeFlags = chapterFlags,
        coverLastModified = coverLastModified,
        url = url,
        // AM (CU) -->
        ogTitle = title,
        ogArtist = artist,
        ogAuthor = author,
        ogDescription = description,
        ogGenre = genre,
        ogStatus = status,
        // <-- AM (CU)
        thumbnailUrl = thumbnailUrl,
        updateStrategy = updateStrategy,
        initialized = initialized,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
    )

    fun mapLibraryAnime(
        id: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        totalCount: Long,
        seenCount: Double,
        latestUpload: Long,
        episodeFetchedAt: Long,
        lastSeen: Long,
        bookmarkCount: Double,
        fillermarkCount: Double,
        category: Long,
    ): LibraryAnime = LibraryAnime(
        anime = mapAnime(
            id,
            source,
            url,
            artist,
            author,
            description,
            genre,
            title,
            status,
            thumbnailUrl,
            favorite,
            lastUpdate,
            nextUpdate,
            initialized,
            viewerFlags,
            chapterFlags,
            coverLastModified,
            dateAdded,
            updateStrategy,
            calculateInterval,
            lastModifiedAt,
            favoriteModifiedAt,
        ),
        category = category,
        totalEpisodes = totalCount,
        seenCount = seenCount.toLong(),
        bookmarkCount = bookmarkCount.toLong(),
        // AM (FILLER) -->
        fillermarkCount = fillermarkCount.toLong(),
        // <-- AM (FILLER)
        latestUpload = latestUpload,
        episodeFetchedAt = episodeFetchedAt,
        lastSeen = lastSeen,
    )
}
