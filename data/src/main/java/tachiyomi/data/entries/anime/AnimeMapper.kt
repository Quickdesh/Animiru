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
        episodeFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        // AM (SYNC) -->
        version: Long,
        @Suppress("UNUSED_PARAMETER")
        isSyncing: Long,
        // <-- AM (SYNC)
    ): Anime = Anime(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        episodeFlags = episodeFlags,
        coverLastModified = coverLastModified,
        url = url,
        // AM (CUSTOM_INFORMATION) -->
        ogTitle = title,
        ogArtist = artist,
        ogAuthor = author,
        ogDescription = description,
        ogGenre = genre,
        ogStatus = status,
        // <-- AM (CUSTOM_INFORMATION)
        thumbnailUrl = thumbnailUrl,
        updateStrategy = updateStrategy,
        initialized = initialized,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        // AM (SYNC) -->
        version = version,
        // <-- AM (SYNC)
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
        episodeFlags: Long,
        coverLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        // AM (SYNC) -->
        version: Long,
        isSyncing: Long,
        // <-- AM (SYNC)
        totalCount: Long,
        seenCount: Double,
        latestUpload: Long,
        episodeFetchedAt: Long,
        lastSeen: Long,
        bookmarkCount: Double,
        // AM (FILLERMARK) -->
        fillermarkCount: Double,
        // <-- AM (FILLERMARK)
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
            episodeFlags,
            coverLastModified,
            dateAdded,
            updateStrategy,
            calculateInterval,
            lastModifiedAt,
            favoriteModifiedAt,
            // AM (SYNC) -->
            version,
            isSyncing,
            // <-- AM (SYNC)
        ),
        category = category,
        totalEpisodes = totalCount,
        seenCount = seenCount.toLong(),
        bookmarkCount = bookmarkCount.toLong(),
        // AM (FILLERMARK) -->
        fillermarkCount = fillermarkCount.toLong(),
        // <-- AM (FILLERMARK)
        latestUpload = latestUpload,
        episodeFetchedAt = episodeFetchedAt,
        lastSeen = lastSeen,
    )
}
