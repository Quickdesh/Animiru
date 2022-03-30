package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode

/**
 * -R> = regex conversion.
 */
object EpisodeRecognition {
    /**
     * All cases with Ch.xx
     * Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation -R> 4
     */
    private val basic = Regex("""(?<=ep\.) *([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used when only one number occurrence
     * Example: Bleach 567: Down With Snowwhite -R> 567
     */
    private val occurrence = Regex("""([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used when anime title removed
     * Example: Solanin 028 Vol. 2 -> 028 Vol.2 -> 028Vol.2 -R> 028
     */
    private val withoutAnime = Regex("""^([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?""")

    /**
     * Regex used to remove unwanted tags
     * Example Prison School 12 v.1 vol004 version1243 volume64 -R> Prison School 12
     */
    private val unwanted = Regex("""(?<![a-z])(v|ver|vol|version|volume|season|s).?[0-9]+""")

    /**
     * Regex used to remove unwanted whitespace
     * Example One Piece 12 special -R> One Piece 12special
     */
    private val unwantedWhiteSpace = Regex("""(\s)(extra|special|omake)""")

    fun parseEpisodeNumber(episode: SEpisode, anime: SAnime) {
        // If episode number is known return.
        if (episode.episode_number == -2F || episode.episode_number > -1F) {
            return
        }

        // Get episode title with lower case
        var name = episode.name.lowercase()

        // Remove comma's from episode.
        name = name.replace(',', '.')

        // Remove unwanted white spaces.
        unwantedWhiteSpace.findAll(name).let {
            it.forEach { occurrence -> name = name.replace(occurrence.value, occurrence.value.trim()) }
        }

        // Remove unwanted tags.
        unwanted.findAll(name).let {
            it.forEach { occurrence -> name = name.replace(occurrence.value, "") }
        }

        // Check base case ep.xx
        if (updateEpisode(basic.find(name), episode)) {
            return
        }

        // Check one number occurrence.
        val occurrences: MutableList<MatchResult> = arrayListOf()
        occurrence.findAll(name).let {
            it.forEach { occurrence -> occurrences.add(occurrence) }
        }

        if (occurrences.size == 1) {
            if (updateEpisode(occurrences[0], episode)) {
                return
            }
        }

        // Remove anime title from episode title.
        val nameWithoutAnime = name.replace(anime.originalTitle.lowercase(), "").trim()

        // Check if first value is number after title remove.
        if (updateEpisode(withoutAnime.find(nameWithoutAnime), episode)) {
            return
        }

        // Take the first number encountered.
        if (updateEpisode(occurrence.find(nameWithoutAnime), episode)) {
            return
        }
    }

    /**
     * Check if volume is found and update episode
     * @param match result of regex
     * @param episode episode object
     * @return true if volume is found
     */
    private fun updateEpisode(match: MatchResult?, episode: SEpisode): Boolean {
        match?.let {
            val initial = it.groups[1]?.value?.toFloat()!!
            val subEpisodeDecimal = it.groups[2]?.value
            val subEpisodeAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subEpisodeDecimal, subEpisodeAlpha)
            episode.episode_number = initial.plus(addition)
            return true
        }
        return false
    }

    /**
     * Check for decimal in received strings
     * @param decimal decimal value of regex
     * @param alpha alpha value of regex
     * @return decimal/alpha float value
     */
    private fun checkForDecimal(decimal: String?, alpha: String?): Float {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toFloat()
        }

        if (!alpha.isNullOrEmpty()) {
            if (alpha.contains("extra")) {
                return .99f
            }

            if (alpha.contains("omake")) {
                return .98f
            }

            if (alpha.contains("special")) {
                return .97f
            }

            return if (alpha[0] == '.') {
                // Take value after (.)
                parseAlphaPostFix(alpha[1])
            } else {
                parseAlphaPostFix(alpha[0])
            }
        }

        return .0f
    }

    /**
     * x.a -> x.1, x.b -> x.2, etc
     */
    private fun parseAlphaPostFix(alpha: Char): Float {
        return ("0." + (alpha.code - 96).toString()).toFloat()
    }
}
