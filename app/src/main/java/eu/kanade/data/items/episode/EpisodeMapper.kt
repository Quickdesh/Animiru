package eu.kanade.data.items.episode

import eu.kanade.domain.items.episode.model.Episode

val episodeMapper: (Long, Long, String, String, String?, Boolean, Boolean, /* AM (FM) --> */Boolean, /* <-- AM (FM) */ Long, Long, Float, Long, Long, Long) -> Episode =
    { id, animeId, url, name, scanlator, seen, bookmark, /* AM (FM) --> */fillermark, /* <-- AM (FM) */ lastSecondSeen, totalSeconds, episodeNumber, sourceOrder, dateFetch, dateUpload ->
        Episode(
            id = id,
            animeId = animeId,
            seen = seen,
            bookmark = bookmark,
            // AM (FM) -->
            fillermark = fillermark,
            // <-- AM (FM)
            lastSecondSeen = lastSecondSeen,
            totalSeconds = totalSeconds,
            dateFetch = dateFetch,
            sourceOrder = sourceOrder,
            url = url,
            name = name,
            dateUpload = dateUpload,
            episodeNumber = episodeNumber,
            scanlator = scanlator,
        )
    }
