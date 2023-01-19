package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy
import java.io.File

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<anime>/<episode>
 *
 * @param context the application context.
 */
class AnimeDownloadProvider(private val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val scope = MainScope()

    /**
     * The root directory for downloads.
     */
    private var downloadsDir = preferences.downloadsDirectory().get().let {
        val dir = UniFile.fromUri(context, it.toUri())
        DiskUtil.createNoMediaFile(dir, context)
        dir
    }

    init {
        preferences.downloadsDirectory().asFlow()
            .onEach { downloadsDir = UniFile.fromUri(context, it.toUri()) }
            .launchIn(scope)
    }

    /**
     * Returns the download directory for a anime. For internal use only.
     *
     * @param animeTitle the title of the anime to query.
     * @param source the source of the anime.
     */
    internal fun getAnimeDir(animeTitle: String, source: AnimeSource): UniFile {
        try {
            return downloadsDir
                .createDirectory(getSourceDirName(source))
                .createDirectory(getAnimeDirName(animeTitle))
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Invalid download directory" }
            throw Exception(context.getString(R.string.invalid_download_dir))
        }
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: AnimeSource): UniFile? {
        return downloadsDir.findFile(getSourceDirName(source), true)
    }

    /**
     * Returns the download directory for a anime if it exists.
     *
     * @param animeTitle the title of the anime to query.
     * @param source the source of the anime.
     */
    fun findAnimeDir(animeTitle: String, source: AnimeSource): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getAnimeDirName(animeTitle), true)
    }

    /**
     * Returns the download directory for a episode if it exists.
     *
     * @param episodeName the name of the episode to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param animeTitle the title of the anime to query.
     * @param source the source of the episode.
     */
    fun findEpisodeDir(episodeName: String, chapterScanlator: String?, animeTitle: String, source: AnimeSource): UniFile? {
        val animeDir = findAnimeDir(animeTitle, source)
        return getValidEpisodeDirNames(episodeName, chapterScanlator).asSequence()
            .mapNotNull { animeDir?.findFile(it, true) }
            .firstOrNull()
    }

    /**
     * Returns a list of downloaded directories for the episodes that exist.
     *
     * @param episodes the episodes to query.
     * @param anime the anime of the episode.
     * @param source the source of the episode.
     */
    fun findEpisodeDirs(episodes: List<Episode>, anime: Anime, source: AnimeSource): List<UniFile> {
        val animeDir = findAnimeDir(anime.title, source) ?: return emptyList()
        return episodes.mapNotNull { episode ->
            getValidEpisodeDirNames(episode.name, episode.scanlator).asSequence()
                .mapNotNull { animeDir.findFile(it) }
                .firstOrNull()
        }
    }

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: AnimeSource): String {
        return DiskUtil.buildValidFilename(source.toString())
    }

    /**
     * Returns the download directory name for a anime.
     *
     * @param animeTitle the title of the anime to query.
     */
    fun getAnimeDirName(animeTitle: String): String {
        return DiskUtil.buildValidFilename(animeTitle)
    }

    /**
     * Returns the episode directory name for a episode.
     *
     * @param episodeName the name of the episode to query.
     * @param chapterScanlator scanlator of the chapter to query
     */
    fun getEpisodeDirName(episodeName: String, chapterScanlator: String?): String {
        return DiskUtil.buildValidFilename(
            when {
                chapterScanlator != null -> "${chapterScanlator}_$episodeName"
                else -> episodeName
            },
        )
    }

    /**
     * Returns valid downloaded episode directory names.
     *
     * @param episodeName the name of the episode to query.
     * @param chapterScanlator scanlator of the chapter to query
     */
    fun getValidEpisodeDirNames(episodeName: String, chapterScanlator: String?): List<String> {
        val episodeDirName = getEpisodeDirName(episodeName, chapterScanlator)
        return listOf(episodeDirName)
    }

    // AM -->
    /**
     * Returns an episode file size in bytes.
     * Returns null if the episode is not found in expected location
     *
     * @param episodeName the name of the episode to query.
     * @param episodeScanlator scanlator of the episode to query
     * @param animeTitle the title of the anime
     * @param animeSource the source of the anime
     */
    fun getDownloadedEpisodeFileSizeBytes(
        episodeName: String,
        episodeScanlator: String?,
        animeTitle: String,
        animeSource: AnimeSource,
    ): Long? {
        return findEpisodeDir(episodeName, episodeScanlator, animeTitle, animeSource)
            ?.filePath?.let {
                DiskUtil.getDirectorySize(File(it))
            }
    }
    // AM <--
}
