package eu.kanade.tachiyomi.ui.anime

import android.os.Bundle
import androidx.compose.runtime.Immutable
import eu.kanade.domain.anime.interactor.GetAnimeWithEpisodes
import eu.kanade.domain.anime.interactor.GetDuplicateLibraryAnime
import eu.kanade.domain.anime.interactor.SetAnimeEpisodeFlags
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.AnimeUpdate
import eu.kanade.domain.anime.model.TriStateFilter
import eu.kanade.domain.anime.model.isLocal
import eu.kanade.domain.anime.model.toAnimeInfo
import eu.kanade.domain.anime.model.toDbAnime
import eu.kanade.domain.animetrack.interactor.DeleteAnimeTrack
import eu.kanade.domain.animetrack.interactor.GetAnimeTracks
import eu.kanade.domain.animetrack.interactor.InsertAnimeTrack
import eu.kanade.domain.animetrack.model.toDbTrack
import eu.kanade.domain.animetrack.model.toDomainTrack
import eu.kanade.domain.category.interactor.GetCategoriesAnime
import eu.kanade.domain.category.interactor.SetAnimeCategories
import eu.kanade.domain.category.model.Category
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.episode.interactor.UpdateEpisode
import eu.kanade.domain.episode.model.EpisodeUpdate
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.animesource.model.toSEpisode
import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.AnimeTrack
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadProvider
import eu.kanade.tachiyomi.data.download.model.AnimeDownload
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.util.episode.EpisodeSettingsHelper
import eu.kanade.tachiyomi.util.episode.getEpisodeSort
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.nullIfEmpty
import eu.kanade.tachiyomi.util.preference.asImmediateFlow
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.shouldDownloadNewEpisodes
import eu.kanade.tachiyomi.util.system.logcat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.trimOrNull
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import logcat.LogPriority
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.DateFormat
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.domain.episode.model.Episode as DomainEpisode

class AnimePresenter(
    val animeId: Long,
    val isFromSource: Boolean,
    private val preferences: PreferencesHelper = Injekt.get(),
    internal val trackManager: TrackManager = Injekt.get(),
    internal val downloadManager: AnimeDownloadManager = Injekt.get(),
    private val coverCache: AnimeCoverCache = Injekt.get(),
    private val sourceManager: AnimeSourceManager = Injekt.get(),
    internal val getAnimeAndEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getCategories: GetCategoriesAnime = Injekt.get(),
    private val deleteTrack: DeleteAnimeTrack = Injekt.get(),
    private val getTracks: GetAnimeTracks = Injekt.get(),
    private val moveAnimeToCategories: SetAnimeCategories = Injekt.get(),
    private val insertTrack: InsertAnimeTrack = Injekt.get(),
) : BasePresenter<AnimeController>() {

    private val _state: MutableStateFlow<AnimeScreenState> = MutableStateFlow(AnimeScreenState.Loading)

    val state = _state.asStateFlow()

    private val successState: AnimeScreenState.Success?
        get() = state.value as? AnimeScreenState.Success

    /**
     * Subscription to update the anime from the source.
     */
    private var fetchAnimeJob: Job? = null

    /**
     * Subscription to retrieve the new list of episodes from the source.
     */
    private var fetchEpisodesJob: Job? = null

    /**
     * Subscription to observe download status changes.
     */
    private var observeDownloadsStatusSubscription: Subscription? = null
    private var observeDownloadsProgressSubscription: Subscription? = null

    private var _trackList: List<TrackItem> = emptyList()
    val trackList get() = _trackList

    private val loggedServices by lazy { trackManager.services.filter { it.isLogged } }

    private var searchTrackerJob: Job? = null
    private var refreshTrackersJob: Job? = null

    val anime: DomainAnime?
        get() = successState?.anime

    val source: AnimeSource?
        get() = successState?.source

    val isFavoritedAnime: Boolean
        get() = anime?.favorite ?: false

    val processedEpisodes: Sequence<EpisodeItem>?
        get() = successState?.processedEpisodes

    // AM -->
    private val customAnimeManager: CustomAnimeManager by injectLazy()
    // AM <--

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private fun updateSuccessState(func: (AnimeScreenState.Success) -> AnimeScreenState.Success) {
        _state.update { if (it is AnimeScreenState.Success) func(it) else it }
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        // Anime info - start

        presenterScope.launchIO {
            if (!getAnimeAndEpisodes.awaitAnime(animeId).favorite) {
                EpisodeSettingsHelper.applySettingDefaults(animeId)
            }

            getAnimeAndEpisodes.subscribe(animeId)
                .collectLatest { (anime, episodes) ->
                    val episodeItems = episodes.toEpisodeItems(anime)
                    _state.update { currentState ->
                        when (currentState) {
                            // Initialize success state
                            AnimeScreenState.Loading -> AnimeScreenState.Success(
                                anime = anime,
                                source = Injekt.get<AnimeSourceManager>().getOrStub(anime.source),
                                dateRelativeTime = preferences.relativeTime().get(),
                                dateFormat = preferences.dateFormat(),
                                isFromSource = isFromSource,
                                trackingAvailable = trackManager.hasLoggedAnimeServices(),
                                episodes = episodeItems,
                            )

                            // Update state
                            is AnimeScreenState.Success -> currentState.copy(anime = anime, episodes = episodeItems)
                        }
                    }

                    observeTrackers()
                    observeTrackingCount()
                    observeDownloads()

                    if (!anime.initialized) {
                        fetchAllFromSource(manualFetch = false)
                    }
                }
        }

        preferences.incognitoMode()
            .asImmediateFlow { incognito ->
                updateSuccessState { it.copy(isIncognitoMode = incognito) }
            }
            .launchIn(presenterScope)

        preferences.downloadedOnly()
            .asImmediateFlow { downloadedOnly ->
                updateSuccessState { it.copy(isDownloadedOnlyMode = downloadedOnly) }
            }
            .launchIn(presenterScope)
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        fetchAnimeFromSource(manualFetch)
        fetchEpisodesFromSource(manualFetch)
    }

    // Anime info - start
    /**
     * Fetch anime information from source.
     */
    private fun fetchAnimeFromSource(manualFetch: Boolean = false) {
        if (fetchAnimeJob?.isActive == true) return
        fetchAnimeJob = presenterScope.launchIO {
            updateSuccessState { it.copy(isRefreshingInfo = true) }
            try {
                successState?.let {
                    val networkAnime = it.source.getAnimeDetails(it.anime.toAnimeInfo())
                    updateAnime.awaitUpdateFromSource(it.anime, networkAnime, manualFetch)
                }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchAnimeInfoError(e) }
            }
            updateSuccessState { it.copy(isRefreshingInfo = false) }
        }
    }

    // AM -->
    fun updateAnimeInfo(
        title: String?,
        author: String?,
        artist: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) {
        val state = successState ?: return
        var anime = state.anime
        if (state.anime.isLocal()) {
            val newTitle = if (title.isNullOrBlank()) anime.url else title.trim()
            val newAuthor = author?.trimOrNull()
            val newArtist = artist?.trimOrNull()
            val newDesc = description?.trimOrNull()
            anime = anime.copy(
                ogTitle = newTitle,
                ogAuthor = author?.trimOrNull(),
                ogArtist = artist?.trimOrNull(),
                ogDescription = description?.trimOrNull(),
                ogGenre = tags?.nullIfEmpty(),
                ogStatus = status ?: 0,
            )
            (sourceManager.get(LocalAnimeSource.ID) as LocalAnimeSource).updateAnimeInfo(anime.toSAnime())
            launchIO {
                updateAnime.await(
                    AnimeUpdate(
                        anime.id,
                        title = newTitle,
                        author = newAuthor,
                        artist = newArtist,
                        description = newDesc,
                        genre = tags,
                        status = status,
                    ),
                )
            }
        } else {
            val genre = if (!tags.isNullOrEmpty() && tags != state.anime.ogGenre) {
                tags
            } else {
                null
            }
            customAnimeManager.saveAnimeInfo(
                CustomAnimeManager.AnimeJson(
                    state.anime.id,
                    title?.trimOrNull(),
                    author?.trimOrNull(),
                    artist?.trimOrNull(),
                    description?.trimOrNull(),
                    genre,
                    status.takeUnless { it == state.anime.ogStatus },
                ),
            )
            anime = anime.copy()
        }

        updateSuccessState { successState ->
            successState.copy(anime = anime)
        }
    }
    // AM <--

    /**
     * Update favorite status of anime, (removes / adds) anime (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        onAdded: () -> Unit,
        onRequireCategory: (anime: DomainAnime, availableCats: List<Category>) -> Unit,
        onDuplicateExists: ((DomainAnime) -> Unit)?,
    ) {
        val state = successState ?: return
        presenterScope.launchIO {
            val anime = state.anime

            if (isFavoritedAnime) {
                // Remove from library
                if (updateAnime.awaitUpdateFavorite(anime.id, false)) {
                    // Remove covers and update last modified in db
                    if (anime.toDbAnime().removeCovers() > 0) {
                        updateAnime.awaitUpdateCoverLastModified(anime.id)
                    }
                    launchUI { onRemoved() }
                }
            } else {
                // Add to library
                // First, check if duplicate exists if callback is provided
                if (onDuplicateExists != null) {
                    val duplicate = getDuplicateLibraryAnime.await(anime.title, anime.source)
                    if (duplicate != null) {
                        launchUI { onDuplicateExists(duplicate) }
                        return@launchIO
                    }
                }

                // Now check if user previously set categories, when available
                val categories = getCategories()
                val defaultCategoryId = preferences.defaultAnimeCategory().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    // Default category set
                    defaultCategory != null -> {
                        val result = updateAnime.awaitUpdateFavorite(anime.id, true)
                        if (!result) return@launchIO
                        moveAnimeToCategory(defaultCategory)
                        launchUI { onAdded() }
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = updateAnime.awaitUpdateFavorite(anime.id, true)
                        if (!result) return@launchIO
                        moveAnimeToCategory(null)
                        launchUI { onAdded() }
                    }

                    // Choose a category
                    else -> launchUI { onRequireCategory(anime, categories) }
                }
            }
        }
    }

    /**
     * Returns true if the anime has any downloads.
     */
    fun hasDownloads(): Boolean {
        val anime = successState?.anime ?: return false
        return downloadManager.getDownloadCount(anime) > 0
    }

    /**
     * Deletes all the downloads for the anime.
     */
    fun deleteDownloads() {
        val state = successState ?: return
        downloadManager.deleteAnime(state.anime, state.source)
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await()
    }

    /**
     * Gets the category id's the anime is in, if the anime is not in a category, returns the default id.
     *
     * @param anime the anime to get categories from.
     * @return Array of category ids the anime is in, if none returns default id
     */
    fun getAnimeCategoryIds(anime: DomainAnime): Array<Long> {
        val categories = runBlocking { getCategories.await(anime.id) }
        return categories.map { it.id }.toTypedArray()
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: DomainAnime, categories: List<Category>) {
        moveAnimeToCategories(categories)
        presenterScope.launchIO {
            updateAnime.awaitUpdateFavorite(anime.id, true)
        }
    }

    /**
     * Move the given anime to categories.
     *
     * @param categories the selected categories.
     */
    private fun moveAnimeToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        presenterScope.launchIO {
            moveAnimeToCategories.await(animeId, categoryIds)
        }
    }

    /**
     * Move the given anime to the category.
     *
     * @param category the selected category, or null for default category.
     */
    private fun moveAnimeToCategory(category: Category?) {
        moveAnimeToCategories(listOfNotNull(category))
    }

    private fun observeTrackingCount() {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            getTracks.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .map { tracks ->
                    val loggedServicesId = loggedServices.map { it.id }
                    tracks.filter { it.syncId in loggedServicesId }.size
                }
                .collectLatest { trackingCount ->
                    updateSuccessState { it.copy(trackingCount = trackingCount) }
                }
        }
    }

    // Anime info - end

    // Episodes list - start

    private fun observeDownloads() {
        observeDownloadsStatusSubscription?.let { remove(it) }
        observeDownloadsStatusSubscription = downloadManager.queue.getStatusObservable()
            .observeOn(Schedulers.io())
            .onBackpressureBuffer()
            .filter { download -> download.anime.id == successState?.anime?.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { _, it -> updateDownloadState(it) },
                { _, error ->
                    logcat(LogPriority.ERROR, error)
                },
            )

        observeDownloadsProgressSubscription?.let { remove(it) }
        observeDownloadsProgressSubscription = downloadManager.queue.getPreciseProgressObservable()
            .observeOn(Schedulers.io())
            .onBackpressureLatest()
            .filter { download -> download.anime.id == successState?.anime?.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeLatestCache(
                { _, download -> updateDownloadState(download) },
                { _, error -> logcat(LogPriority.ERROR, error) },
            )
    }

    private fun updateDownloadState(download: AnimeDownload) {
        updateSuccessState { successState ->
            val modifiedIndex = successState.episodes.indexOfFirst { it.episode.id == download.episode.id }
            if (modifiedIndex < 0) return@updateSuccessState successState

            val newEpisodes = successState.episodes.toMutableList().apply {
                val item = removeAt(modifiedIndex)
                    .copy(downloadState = download.status, downloadProgress = download.progress)
                add(modifiedIndex, item)
            }
            successState.copy(episodes = newEpisodes)
        }
    }

    private fun List<DomainEpisode>.toEpisodeItems(anime: DomainAnime): List<EpisodeItem> {
        return map { episode ->
            val activeDownload = downloadManager.queue.find { episode.id == it.episode.id }
            val downloaded = downloadManager.isEpisodeDownloaded(episode.name, episode.scanlator, anime.title, anime.source)
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> AnimeDownload.State.DOWNLOADED
                else -> AnimeDownload.State.NOT_DOWNLOADED
            }

            // AM  -->
            val animeSource = Injekt.get<AnimeSourceManager>().getOrStub(anime.source)
            val context = view!!.applicationContext!!

            val downloadedFileSize =
                if (preferences.showDownloadedEpisodeSize() && downloadState == AnimeDownload.State.DOWNLOADED) {
                    AnimeDownloadProvider(context).getDownloadedEpisodeFileSizeBytes(
                        episode.name,
                        episode.scanlator,
                        anime.title,
                        animeSource,
                    )
                } else null
            // AM  <--

            EpisodeItem(
                episode = episode,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                downloadedFileSize
            )
        }
    }

    /**
     * Requests an updated list of episodes from the source.
     */
    private fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        if (fetchEpisodesJob?.isActive == true) return
        fetchEpisodesJob = presenterScope.launchIO {
            updateSuccessState { it.copy(isRefreshingEpisode = true) }
            try {
                successState?.let { successState ->
                    val episodes = successState.source.getEpisodeList(successState.anime.toAnimeInfo())
                        .map { it.toSEpisode() }

                    val (newEpisodes, _) = syncEpisodesWithSource.await(
                        episodes,
                        successState.anime,
                        successState.source,
                    )

                    if (manualFetch) {
                        downloadNewEpisodes(newEpisodes)
                    }
                }
            } catch (e: Throwable) {
                withUIContext { view?.onFetchEpisodesError(e) }
            }
            updateSuccessState { it.copy(isRefreshingEpisode = false) }
        }
    }

    /**
     * Returns the next unseen episode or null if everything is seen.
     */
    fun getNextUnseenEpisode(): DomainEpisode? {
        val successState = successState ?: return null
        return successState.processedEpisodes.map { it.episode }.let { episodes ->
            if (successState.anime.sortDescending()) {
                episodes.findLast { !it.seen }
            } else {
                episodes.find { !it.seen }
            }
        }
    }

    fun getUnseenEpisodes(checkDownloads: Boolean = true): List<DomainEpisode> {
        return if (checkDownloads) successState?.processedEpisodes
            ?.filter { (episode, dlStatus) -> !episode.seen && dlStatus == AnimeDownload.State.NOT_DOWNLOADED }
            ?.map { it.episode }
            ?.toList()
            ?: emptyList()
        // AM -->
        else successState?.processedEpisodes
            ?.filter { (episode) -> !episode.seen }
            ?.map { it.episode }
            ?.toList()
            ?: emptyList()
        // AM <--
    }

    fun getUnseenEpisodesSorted(checkDownloads: Boolean = true): List<DomainEpisode> {
        val anime = successState?.anime ?: return emptyList()
        val episodes = getUnseenEpisodes(checkDownloads).sortedWith(getEpisodeSort(anime))
        return if (anime.sortDescending()) episodes.reversed() else episodes
    }

    fun startDownloadingNow(episodeId: Long) {
        downloadManager.startDownloadNow(episodeId)
    }

    fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.queue.find { episodeId == it.episode.id } ?: return
        downloadManager.deletePendingDownload(activeDownload)
        updateDownloadState(activeDownload.apply { status = AnimeDownload.State.NOT_DOWNLOADED })
    }

    fun markPreviousEpisodeSeen(pointer: DomainEpisode) {
        val successState = successState ?: return
        val episodes = processedEpisodes.orEmpty().map { it.episode }.toList()
        val prevEpisodes = if (successState.anime.sortDescending()) episodes.asReversed() else episodes
        val pointerPos = prevEpisodes.indexOf(pointer)
        if (pointerPos != -1) markEpisodesSeen(prevEpisodes.take(pointerPos), true)
    }

    /**
     * Mark the selected episode list as seen/unseen.
     * @param episodes the list of selected episodes.
     * @param seen whether to mark episodes as seen or unseen.
     */
    fun markEpisodesSeen(episodes: List<DomainEpisode>, seen: Boolean) {
        presenterScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                values = episodes.toTypedArray(),
            )
        }
    }

    /**
     * Downloads the given list of episodes with the manager.
     * @param episodes the list of episodes to download.
     */
    fun downloadEpisodes(episodes: List<DomainEpisode>, alt: Boolean = false) {
        val anime = successState?.anime ?: return
        if (alt) {
            downloadManager.downloadEpisodesAlt(anime, episodes)
        } else {
            downloadManager.downloadEpisodes(anime, episodes)
        }
    }

    /**
     * Bookmarks the given list of episodes.
     * @param episodes the list of episodes to bookmark.
     */
    fun bookmarkEpisodes(episodes: List<DomainEpisode>, bookmarked: Boolean) {
        presenterScope.launchIO {
            episodes
                .filterNot { it.bookmark == bookmarked }
                .map { EpisodeUpdate(id = it.id, bookmark = bookmarked) }
                .let { updateEpisode.awaitAll(it) }
        }
    }

    /**
     * Fillermarks the given list of episodes.
     * @param selectedEpisodes the list of episodes to fillermark.
     */
    fun fillermarkEpisodes(episodes: List<DomainEpisode>, fillermarked: Boolean) {
        presenterScope.launchIO {
            episodes
                .filterNot { it.fillermark == fillermarked }
                .map { EpisodeUpdate(id = it.id, fillermark = fillermarked) }
                .let { updateEpisode.awaitAll(it) }
        }
    }

    /**
     * Deletes the given list of episode.
     * @param episodes the list of episodes to delete.
     */
    fun deleteEpisodes(episodes: List<DomainEpisode>) {
        launchIO {
            try {
                updateSuccessState { successState ->
                    val deletedIds = downloadManager
                        .deleteEpisodes(episodes, successState.anime, successState.source)
                        .map { it.id }
                    val deletedEpisodes = successState.episodes.filter { deletedIds.contains(it.episode.id) }
                    if (deletedEpisodes.isEmpty()) return@updateSuccessState successState

                    // TODO: Don't do this fake status update
                    val newEpisodes = successState.episodes.toMutableList().apply {
                        deletedEpisodes.forEach {
                            val index = indexOf(it)
                            val toAdd = removeAt(index)
                                .copy(downloadState = AnimeDownload.State.NOT_DOWNLOADED, downloadProgress = 0)
                            add(index, toAdd)
                        }
                    }
                    successState.copy(episodes = newEpisodes)
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun downloadNewEpisodes(episodes: List<DomainEpisode>) {
        presenterScope.launchIO {
            val anime = successState?.anime ?: return@launchIO
            val categories = getCategories.await(anime.id).map { it.id }
            if (episodes.isEmpty() || !anime.shouldDownloadNewEpisodes(categories, preferences)) return@launchIO
            downloadEpisodes(episodes)
        }
    }

    /**
     * Sets the seen filter and requests an UI update.
     * @param state whether to display only unseen episodes or all episodes.
     */
    fun setUnseenFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_UNSEEN
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_SEEN
        }
        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetUnseenFilter(anime, flag)
        }
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded episodes or all episodes.
     */
    fun setDownloadedFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_DOWNLOADED
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_NOT_DOWNLOADED
        }

        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetDownloadedFilter(anime, flag)
        }
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked episodes or all episodes.
     */
    fun setBookmarkedFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_BOOKMARKED
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_NOT_BOOKMARKED
        }

        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetBookmarkFilter(anime, flag)
        }
    }

    /**
     * Sets the fillermark filter and requests an UI update.
     * @param state whether to display only fillermarked episodes or all episodes.
     */
    fun setFillermarkedFilter(state: State) {
        val anime = successState?.anime ?: return

        val flag = when (state) {
            State.IGNORE -> DomainAnime.SHOW_ALL
            State.INCLUDE -> DomainAnime.EPISODE_SHOW_FILLERMARKED
            State.EXCLUDE -> DomainAnime.EPISODE_SHOW_NOT_FILLERMARKED
        }

        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetFillermarkFilter(anime, flag)
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetDisplayMode(anime, mode)
        }
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Long) {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(anime, sort)
        }
    }

    // Episodes list - end

    // Track sheet - start

    private fun observeTrackers() {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            getTracks.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .map { tracks ->
                    val dbTracks = tracks.map { it.toDbTrack() }
                    loggedServices.map { service ->
                        TrackItem(dbTracks.find { it.sync_id.toLong() == service.id }, service)
                    }
                }
                .collectLatest { trackItems ->
                    _trackList = trackItems
                    withContext(Dispatchers.Main) {
                        view?.onNextTrackers(trackItems)
                    }
                }
        }
    }

    fun refreshTrackers() {
        refreshTrackersJob?.cancel()
        refreshTrackersJob = launchIO {
            supervisorScope {
                try {
                    trackList
                        .map {
                            async {
                                val track = it.track ?: return@async null

                                val updatedTrack = it.service.refresh(track)

                                val domainTrack = updatedTrack.toDomainTrack() ?: return@async null
                                insertTrack.await(domainTrack)
                            }
                        }
                        .awaitAll()

                    withUIContext { view?.onTrackingRefreshDone() }
                } catch (e: Throwable) {
                    withUIContext { view?.onTrackingRefreshError(e) }
                }
            }
        }
    }

    fun trackingSearch(query: String, service: TrackService) {
        searchTrackerJob?.cancel()
        searchTrackerJob = launchIO {
            try {
                val results = service.searchAnime(query)
                withUIContext { view?.onTrackingSearchResults(results) }
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingSearchResultsError(e) }
            }
        }
    }

    fun registerTracking(item: AnimeTrack?, service: TrackService) {
        val successState = successState ?: return
        if (item != null) {
            item.anime_id = successState.anime.id
            launchIO {
                try {
                    val allEpisodes = successState.episodes
                        .map { it.episode.toDbEpisode() }
                    val hasSeenEpisodes = allEpisodes.any { it.seen }
                    service.bind(item, hasSeenEpisodes)

                    item.toDomainTrack(idRequired = false)?.let { track ->
                        insertTrack.await(track)
                    }
                } catch (e: Throwable) {
                    withUIContext { view?.applicationContext?.toast(e.message) }
                }
            }
        } else {
            unregisterTracking(service)
        }
    }

    fun unregisterTracking(service: TrackService) {
        val anime = successState?.anime ?: return

        presenterScope.launchIO {
            deleteTrack.await(anime.id, service.id)
        }
    }

    private fun updateRemote(track: AnimeTrack, service: TrackService) {
        launchIO {
            try {
                service.update(track)

                track.toDomainTrack(idRequired = false)?.let {
                    insertTrack.await(it)
                }

                withUIContext { view?.onTrackingRefreshDone() }
            } catch (e: Throwable) {
                withUIContext { view?.onTrackingRefreshError(e) }

                // Restart on error to set old values
                observeTrackers()
            }
        }
    }

    fun setTrackerStatus(item: TrackItem, index: Int) {
        val track = item.track!!
        track.status = item.service.getStatusListAnime()[index]
        if (track.status == item.service.getCompletionStatus() && track.total_episodes != 0) {
            track.last_episode_seen = track.total_episodes.toFloat()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerScore(item: TrackItem, index: Int) {
        val track = item.track!!
        track.score = item.service.indexToScore(index)
        updateRemote(track, item.service)
    }

    fun setTrackerLastEpisodeSeen(item: TrackItem, episodeNumber: Int) {
        val track = item.track!!
        if (track.last_episode_seen == 0F && track.last_episode_seen < episodeNumber && track.status != item.service.getRewatchingStatus()) {
            track.status = item.service.getWatchingStatus()
        }
        track.last_episode_seen = episodeNumber.toFloat()
        if (track.total_episodes != 0 && track.last_episode_seen.toInt() == track.total_episodes) {
            track.status = item.service.getCompletionStatus()
        }
        updateRemote(track, item.service)
    }

    fun setTrackerStartDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.started_watching_date = date
        updateRemote(track, item.service)
    }

    fun setTrackerFinishDate(item: TrackItem, date: Long) {
        val track = item.track!!
        track.finished_watching_date = date
        updateRemote(track, item.service)
    }

    // Track sheet - end
}

sealed class AnimeScreenState {
    @Immutable
    object Loading : AnimeScreenState()

    @Immutable
    data class Success(
        val anime: DomainAnime,
        val source: AnimeSource,
        val dateRelativeTime: Int,
        val dateFormat: DateFormat,
        val isFromSource: Boolean,
        val episodes: List<EpisodeItem>,
        val trackingAvailable: Boolean = false,
        val trackingCount: Int = 0,
        val isRefreshingInfo: Boolean = false,
        val isRefreshingEpisode: Boolean = false,
        val isIncognitoMode: Boolean = false,
        val isDownloadedOnlyMode: Boolean = false,
    ) : AnimeScreenState() {

        val processedEpisodes: Sequence<EpisodeItem>
            get() = episodes.applyFilters(anime)

        /**
         * Applies the view filters to the list of episodes obtained from the database.
         * @return an observable of the list of episodes filtered and sorted.
         */
        private fun List<EpisodeItem>.applyFilters(anime: DomainAnime): Sequence<EpisodeItem> {
            val isLocalAnime = anime.isLocal()
            val unseenFilter = anime.unseenFilter
            val downloadedFilter = anime.downloadedFilter
            val bookmarkedFilter = anime.bookmarkedFilter
            val fillermarkedFilter = anime.fillermarkedFilter
            return asSequence()
                .filter { (episode) ->
                    when (unseenFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> !episode.seen
                        TriStateFilter.ENABLED_NOT -> episode.seen
                    }
                }
                .filter { (episode) ->
                    when (bookmarkedFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> episode.bookmark
                        TriStateFilter.ENABLED_NOT -> !episode.bookmark
                    }
                }
                .filter { (episode) ->
                    when (fillermarkedFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> episode.fillermark
                        TriStateFilter.ENABLED_NOT -> !episode.fillermark
                    }
                }
                .filter {
                    when (downloadedFilter) {
                        TriStateFilter.DISABLED -> true
                        TriStateFilter.ENABLED_IS -> it.isDownloaded || isLocalAnime
                        TriStateFilter.ENABLED_NOT -> !it.isDownloaded && !isLocalAnime
                    }
                }
                .sortedWith { (episode1), (episode2) -> getEpisodeSort(anime).invoke(episode1, episode2) }
        }
    }
}

@Immutable
data class EpisodeItem(
    val episode: DomainEpisode,
    val downloadState: AnimeDownload.State,
    val downloadProgress: Int,
    val downloadedFileSize:Long?
) {
    val isDownloaded = downloadState == AnimeDownload.State.DOWNLOADED
}
