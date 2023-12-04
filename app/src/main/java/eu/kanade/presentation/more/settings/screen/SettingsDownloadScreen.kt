package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.presentation.util.collectAsState
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsDownloadScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_downloads

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetMangaCategories>() }
        val allCategories by getCategories.subscribe().collectAsState(
            initial = runBlocking { getCategories.await() },
        )
        val getAnimeCategories = remember { Injekt.get<GetAnimeCategories>() }
        val allAnimeCategories by getAnimeCategories.subscribe().collectAsState(
            initial = runBlocking { getAnimeCategories.await() },
        )

        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }

        // AM (FILE-SIZE) -->
        LaunchedEffect(Unit) {
            downloadPreferences.showEpisodeFileSize().changes()
                .drop(1)
                .collectLatest { value ->
                    if (value) {
                        Injekt.get<AnimeDownloadCache>().invalidateCache()
                    }
                }
        }
        // <-- AM (FILE-SIZE)

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = downloadPreferences.downloadOnlyOverWifi(),
                title = stringResource(MR.strings.connected_to_wifi),
            ),
            // AM (FS) -->
            Preference.PreferenceItem.SwitchPreference(
                pref = downloadPreferences.showEpisodeFileSize(),
                title = stringResource(R.string.pref_show_downloaded_episode_file_size),
            ),
            // <-- AM (FS)
            Preference.PreferenceItem.ListPreference(
                pref = downloadPreferences.numberOfDownloads(),
                title = stringResource(MR.strings.pref_download_slots),
                entries = (1..5).associateWith { it.toString() },
            ),
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.download_slots_info)),
            getDeleteEpisodesGroup(
                downloadPreferences = downloadPreferences,
                categories = allAnimeCategories,
            ),
            getAutoDownloadGroup(
                downloadPreferences = downloadPreferences,
                allAnimeCategories = allAnimeCategories,
            ),
            getDownloadAheadGroup(downloadPreferences = downloadPreferences),
            getExternalDownloaderGroup(
                downloadPreferences = downloadPreferences,
                basePreferences = basePreferences,
            ),
        )
    }

    @Composable
    private fun getEpisodesGroup(
        downloadPreferences: DownloadPreferences,
        categories: List<Category>,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_delete_episodes),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = downloadPreferences.removeAfterMarkedAsSeen(),
                    title = stringResource(MR.strings.pref_remove_after_marked_as_seen),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = downloadPreferences.removeAfterSeenSlots(),
                    title = stringResource(MR.strings.pref_remove_after_read),
                    entries = mapOf(
                        -1 to stringResource(MR.strings.disabled),
                        0 to stringResource(MR.strings.last_read_episode),
                        1 to stringResource(MR.string.second_to_last_episode),
                        2 to stringResource(MR.string.third_to_last_episode),
                        3 to stringResource(MR.string.fourth_to_last_episode),
                        4 to stringResource(MR.string.fifth_to_last_episode),
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = downloadPreferences.removeBookmarkedEpisodes(),
                    title = stringResource(MR.strings.pref_remove_bookmarked_episodes),
                ),
                getExcludedCategoriesPreference(
                    downloadPreferences = downloadPreferences,
                    categories = { categories },
                ),
            ),
        )
    }

    @Composable
    private fun getExcludedCategoriesPreference(
        downloadPreferences: DownloadPreferences,
        categories: () -> List<Category>,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            pref = downloadPreferences.removeExcludeAnimeCategories(),
            title = stringResource(MR.string.pref_remove_exclude_categories),
            entries = categories().associate { it.id.toString() to it.visualName },
        )
    }

    @Composable
    private fun getAutoDownloadGroup(
        downloadPreferences: DownloadPreferences,
        allAnimeCategories: List<Category>,
    ): Preference.PreferenceGroup {
        val downloadNewEpisodesPref = downloadPreferences.downloadNewEpisodes()
        val downloadNewEpisodeCategoriesPref = downloadPreferences.downloadNewEpisodeCategories()
        val downloadNewEpisodeCategoriesExcludePref = downloadPreferences.downloadNewEpisodeCategoriesExclude()

        val downloadNewEpisodes by downloadNewEpisodesPref.collectAsState()

        val includedAnime by downloadNewEpisodeCategoriesPref.collectAsState()
        val excludedAnime by downloadNewEpisodeCategoriesExcludePref.collectAsState()
        var showAnimeDialog by rememberSaveable { mutableStateOf(false) }
        if (showAnimeDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.anime_categories),
                message = stringResource(MR.strings.pref_download_new_categories_details),
                items = allAnimeCategories,
                initialChecked = includedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                initialInversed = excludedAnime.mapNotNull { id -> allAnimeCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showAnimeDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    downloadNewEpisodeCategoriesPref.set(
                        newIncluded.fastMap { it.id.toString() }.toSet(),
                    )
                    downloadNewEpisodeCategoriesExcludePref.set(
                        newExcluded.fastMap { it.id.toString() }.toSet(),
                    )
                    showAnimeDialog = false
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_auto_download),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = downloadNewEpisodesPref,
                    title = stringResource(MR.strings.pref_download_new_episodes),
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.anime_categories),
                    subtitle = getCategoriesLabel(
                        allCategories = allAnimeCategories,
                        included = includedAnime,
                        excluded = excludedAnime,
                    ),
                    onClick = { showAnimeDialog = true },
                    enabled = downloadNewEpisodes,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    pref = downloadPreferences.notDownloadFillermarkedItems(),
                    title = stringResource(R.string.pref_not_download_fillermarked_items),
                ),
            ),
        )
    }

    @Composable
    private fun getDownloadAheadGroup(
        downloadPreferences: DownloadPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.download_ahead),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    pref = downloadPreferences.autoDownloadWhileWatching(),
                    title = stringResource(MR.strings.auto_download_while_watching),
                    entries = listOf(0, 2, 3, 5, 10).associateWith {
                        if (it == 0) {
                            stringResource(MR.strings.disabled)
                        } else {
                            pluralStringResource(
                                MR.plurals.next_unseen_episodes,
                                count = it,
                                it,
                            )
                        }
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(MR.strings.download_ahead_info),
                ),
            ),
        )
    }

    @Composable
    private fun getExternalDownloaderGroup(
        downloadPreferences: DownloadPreferences,
        basePreferences: BasePreferences,
    ): Preference.PreferenceGroup {
        val useExternalDownloader = downloadPreferences.useExternalDownloader()
        val externalDownloaderPreference = downloadPreferences.externalDownloaderSelection()

        val pm = basePreferences.context.packageManager
        val installedPackages = pm.getInstalledPackages(0)
        val supportedDownloaders = installedPackages.filter {
            when (it.packageName) {
                "idm.internet.download.manager" -> true
                "idm.internet.download.manager.plus" -> true
                "idm.internet.download.manager.adm.lite" -> true
                "com.dv.adm" -> true
                else -> false
            }
        }
        val packageNames = supportedDownloaders.map { it.packageName }
        val packageNamesReadable = supportedDownloaders
            .map { pm.getApplicationLabel(it.applicationInfo).toString() }

        val packageNamesMap: Map<String, String> =
            packageNames.zip(packageNamesReadable)
                .toMap()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_external_downloader),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    pref = useExternalDownloader,
                    title = stringResource(MR.strings.pref_use_external_downloader),
                ),
                Preference.PreferenceItem.ListPreference(
                    pref = externalDownloaderPreference,
                    title = stringResource(MR.strings.pref_external_downloader_selection),
                    entries = mapOf("" to "None") + packageNamesMap,
                ),
            ),
        )
    }
}
