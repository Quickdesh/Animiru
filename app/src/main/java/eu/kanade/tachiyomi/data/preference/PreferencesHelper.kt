package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import eu.kanade.domain.animesource.interactor.SetMigrateSorting
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsService
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.ui.animelib.setting.DisplayModeSetting
import eu.kanade.tachiyomi.ui.animelib.setting.SortDirectionSetting
import eu.kanade.tachiyomi.ui.animelib.setting.SortModeSetting
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDevFlavor
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import eu.kanade.domain.anime.model.Anime as DomainAnime
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferenceValues as Values

class PreferencesHelper(val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val flowPrefs = FlowSharedPreferences(prefs)

    private val defaultDownloadsDir = File(
        Environment.getExternalStorageDirectory().absolutePath + File.separator +
            context.getString(R.string.app_name),
        "downloads",
    ).toUri()

    private val defaultBackupDir = File(
        Environment.getExternalStorageDirectory().absolutePath + File.separator +
            context.getString(R.string.app_name),
        "backup",
    ).toUri()

    fun startScreen() = prefs.getInt(Keys.startScreen, 1)

    fun confirmExit() = prefs.getBoolean(Keys.confirmExit, false)

    fun sideNavIconAlignment() = flowPrefs.getInt("pref_side_nav_icon_alignment", 0)

    fun useAuthenticator() = flowPrefs.getBoolean("use_biometric_lock", false)

    fun lockAppAfter() = flowPrefs.getInt("lock_app_after", 0)

    fun lastAppUnlock() = flowPrefs.getLong("last_app_unlock", 0)

    fun secureScreen() = flowPrefs.getEnum("secure_screen_v2", Values.SecureScreenMode.INCOGNITO)

    fun hideNotificationContent() = prefs.getBoolean(Keys.hideNotificationContent, false)

    fun autoUpdateMetadata() = prefs.getBoolean(Keys.autoUpdateMetadata, false)

    fun autoUpdateTrackers() = prefs.getBoolean(Keys.autoUpdateTrackers, false)

    fun themeMode() = flowPrefs.getEnum(
        "pref_theme_mode_key",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { Values.ThemeMode.system } else { Values.ThemeMode.light },
    )

    fun appTheme() = flowPrefs.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) { Values.AppTheme.MONET } else { Values.AppTheme.DEFAULT },
    )

    fun themeDarkAmoled() = flowPrefs.getBoolean("pref_theme_dark_amoled_key", false)

    fun preserveWatchingPosition() = prefs.getBoolean(Keys.preserveWatchingPosition, false)

    fun pipEpisodeToasts() = prefs.getBoolean(Keys.pipEpisodeToasts, true)

    fun pipOnExit() = prefs.getBoolean(Keys.pipOnExit, false)

    fun playerBrightnessValue() = flowPrefs.getFloat("player_brightness_value", -1.0F)

    fun playerVolumeValue() = flowPrefs.getFloat("player_volume_value", -1.0F)

    fun autoplayEnabled() = flowPrefs.getBoolean("pref_auto_play_enabled", false)

    fun invertedPlaybackTxt() = flowPrefs.getBoolean("pref_invert_playback_txt", false)

    fun invertedDurationTxt() = flowPrefs.getBoolean("pref_invert_duration_txt", false)

    fun mpvConf() = prefs.getString(Keys.mpvConf, "")

    fun defaultPlayerOrientationType() = prefs.getString(Keys.defaultPlayerOrientationType, "10")!!.toInt()

    fun adjustOrientationVideoDimensions() = prefs.getBoolean(Keys.adjustOrientationVideoDimensions, true)

    fun defaultPlayerOrientationLandscape() = prefs.getString(Keys.defaultPlayerOrientationLandscape, "6")!!.toInt()

    fun defaultPlayerOrientationPortrait() = prefs.getString(Keys.defaultPlayerOrientationPortrait, "7")!!.toInt()

    fun getPlayerSpeed() = prefs.getFloat(Keys.playerSpeed, 1F)

    fun setPlayerSpeed(newSpeed: Float) = prefs.edit {
        putFloat(Keys.playerSpeed, newSpeed)
    }

    fun getPlayerSmoothSeek() = prefs.getBoolean(Keys.playerSmoothSeek, false)

    fun getPlayerViewMode() = prefs.getInt(Keys.playerViewMode, 1)

    fun playerFullscreen() = prefs.getBoolean("player_fullscreen", true)

    fun setPlayerViewMode(newMode: Int) = prefs.edit {
        putInt(Keys.playerViewMode, newMode)
    }

    fun hideControls() = prefs.getBoolean("player_hide_controls", false)

    fun screenshotSubtitles() = flowPrefs.getBoolean("pref_screenshot_subtitles", false)

    fun gestureVolumeBrightness() = flowPrefs.getBoolean("pref_gesture_volume_brightness", true)

    fun gestureHorizontalSeek() = flowPrefs.getBoolean("pref_gesture_horizontal_seek", true)

    fun alwaysUseExternalPlayer() = prefs.getBoolean(Keys.alwaysUseExternalPlayer, false)

    fun externalPlayerPreference() = prefs.getString(Keys.externalPlayerPreference, "")

    fun progressPreference() = prefs.getString(Keys.progressPreference, "0.85F")!!.toFloat()

    fun defaultIntroLength() = flowPrefs.getInt(Keys.defaultIntroLength, 85)

    fun skipLengthPreference() = prefs.getString(Keys.skipLengthPreference, "10")!!.toInt()

    fun readerTheme() = flowPrefs.getInt("pref_reader_theme_key", 1)

    fun navigationModePager() = flowPrefs.getInt("reader_navigation_mode_pager", 0)

    fun navigationModeWebtoon() = flowPrefs.getInt("reader_navigation_mode_webtoon", 0)

    fun portraitColumns() = flowPrefs.getInt("pref_library_columns_portrait_key", 0)

    fun landscapeColumns() = flowPrefs.getInt("pref_library_columns_landscape_key", 0)

    fun autoUpdateTrack() = prefs.getBoolean(Keys.autoUpdateTrack, true)

    fun lastUsedAnimeSource() = flowPrefs.getLong("last_anime_catalogue_source", -1)

    fun lastUsedAnimeCategory() = flowPrefs.getInt("last_used_anime_category", 0)

    fun lastVersionCode() = flowPrefs.getInt("last_version_code", 0)

    fun sourceDisplayMode() = flowPrefs.getEnum("pref_display_mode_catalogue", DisplayModeSetting.COMPACT_GRID)

    fun enabledLanguages() = flowPrefs.getStringSet("source_languages", setOf("all", "en", Locale.getDefault().language))

    fun trackUsername(sync: TrackService) = prefs.getString(Keys.trackUsername(sync.id), "")

    fun trackPassword(sync: TrackService) = prefs.getString(Keys.trackPassword(sync.id), "")

    fun setTrackCredentials(sync: TrackService, username: String, password: String) {
        prefs.edit {
            putString(Keys.trackUsername(sync.id), username)
            putString(Keys.trackPassword(sync.id), password)
        }
    }

    fun trackToken(sync: TrackService) = flowPrefs.getString(Keys.trackToken(sync.id), "")

    fun connectionToken(sync: ConnectionsService) = flowPrefs.getString(Keys.connectionToken(sync.id), "")

    fun anilistScoreType() = flowPrefs.getString("anilist_score_type", Anilist.POINT_10)

    fun backupsDirectory() = flowPrefs.getString("backup_directory", defaultBackupDir.toString())

    fun relativeTime() = flowPrefs.getInt("relative_time", 7)

    fun dateFormat(format: String = flowPrefs.getString(Keys.dateFormat, "").get()): DateFormat = when (format) {
        "" -> DateFormat.getDateInstance(DateFormat.SHORT)
        else -> SimpleDateFormat(format, Locale.getDefault())
    }

    fun downloadsDirectory() = flowPrefs.getString("download_directory", defaultDownloadsDir.toString())

    fun useExternalDownloader() = prefs.getBoolean(Keys.useExternalDownloader, false)

    fun externalDownloaderSelection() = prefs.getString(Keys.externalDownloaderSelection, "")

    fun downloadOnlyOverWifi() = prefs.getBoolean(Keys.downloadOnlyOverWifi, true)

    fun showDownloadedEpisodeSize() = prefs.getBoolean(Keys.showDownloadedEpisodeSize, true) // AM

    fun numberOfBackups() = flowPrefs.getInt("backup_slots", 2)

    fun backupInterval() = flowPrefs.getInt("backup_interval", 0)

    fun backupFlags() = flowPrefs.getStringSet("backup_flags", setOf(FLAG_CATEGORIES, FLAG_EPISODES, FLAG_HISTORY, FLAG_TRACK))

    fun removeAfterSeenSlots() = prefs.getInt(Keys.removeAfterSeenSlots, -1)

    fun removeAfterMarkedAsSeen() = prefs.getBoolean(Keys.removeAfterMarkedAsSeen, false)

    fun downloadAfterSeenSlots() = flowPrefs.getInt(Keys.downloadAfterSeenSlots, 0)

    fun trackOnAddingToLibrary() = flowPrefs.getBoolean(Keys.trackOnAddingToLibrary, true)

    fun downloadOnAddingToLibrary() = flowPrefs.getInt(Keys.downloadOnAddingToLibrary, 1)

    fun removeBookmarkedChapters() = prefs.getBoolean(Keys.removeBookmarkedChapters, false)

    fun removeExcludeAnimeCategories() = flowPrefs.getStringSet("remove_exclude_categories_anime", emptySet())

    fun libraryUpdateInterval() = flowPrefs.getInt("pref_library_update_interval_key", 24)

    fun defaultUserAgent() = flowPrefs.getString(Keys.defaultUserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:103.0) Gecko/20100101 Firefox/103.0")

    fun libraryUpdateDeviceRestriction() = flowPrefs.getStringSet("library_update_restriction", setOf(DEVICE_ONLY_ON_WIFI))
    fun libraryUpdateMangaRestriction() = flowPrefs.getStringSet("library_update_manga_restriction", setOf(ANIME_HAS_UNSEEN, ANIME_NON_COMPLETED, ANIME_NON_SEEN))

    fun showUpdatesNavBadge() = flowPrefs.getBoolean("library_update_show_tab_badge", false)
    fun unreadUpdatesCount() = flowPrefs.getInt("library_unread_updates_count", 0)
    fun unseenUpdatesCount() = flowPrefs.getInt("library_unseen_updates_count", 0)

    fun animelibUpdateCategories() = flowPrefs.getStringSet("animelib_update_categories", emptySet())

    fun animelibUpdateCategoriesExclude() = flowPrefs.getStringSet("animelib_update_categories_exclude", emptySet())

    fun libraryDisplayMode() = flowPrefs.getEnum("pref_display_mode_library", DisplayModeSetting.COMPACT_GRID)

    fun downloadBadge() = flowPrefs.getBoolean("display_download_badge", false)

    fun localBadge() = flowPrefs.getBoolean("display_local_badge", true)

    fun downloadedOnly() = flowPrefs.getBoolean("pref_downloaded_only", false)

    fun unreadBadge() = flowPrefs.getBoolean("display_unread_badge", true)

    fun languageBadge() = flowPrefs.getBoolean("display_language_badge", false)

    fun animeCategoryTabs() = flowPrefs.getBoolean("display_anime_category_tabs", true)

    fun animeCategoryNumberOfItems() = flowPrefs.getBoolean("display_number_of_items_anime", false)

    fun filterDownloaded() = flowPrefs.getInt(Keys.filterDownloaded, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterUnread() = flowPrefs.getInt(Keys.filterUnread, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterStarted() = flowPrefs.getInt(Keys.filterStarted, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterCompleted() = flowPrefs.getInt(Keys.filterCompleted, ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun filterTracking(name: Int) = flowPrefs.getInt("${Keys.filterTracked}_$name", ExtendedNavigationView.Item.TriStateGroup.State.IGNORE.value)

    fun librarySortingMode() = flowPrefs.getEnum(Keys.librarySortingMode, SortModeSetting.ALPHABETICAL)
    fun librarySortingAscending() = flowPrefs.getEnum(Keys.librarySortingDirection, SortDirectionSetting.ASCENDING)

    fun migrationSortingMode() = flowPrefs.getEnum(Keys.migrationSortingMode, SetMigrateSorting.Mode.ALPHABETICAL)
    fun migrationSortingDirection() = flowPrefs.getEnum(Keys.migrationSortingDirection, SetMigrateSorting.Direction.ASCENDING)

    fun automaticExtUpdates() = flowPrefs.getBoolean("automatic_ext_updates", true)

    fun showNsfwSource() = flowPrefs.getBoolean("show_nsfw_source", true)

    fun extensionUpdatesCount() = flowPrefs.getInt("ext_updates_count", 0)

    fun animeextensionUpdatesCount() = flowPrefs.getInt("animeext_updates_count", 0)

    fun lastAppCheck() = flowPrefs.getLong("last_app_check", 0)

    fun lastExtCheck() = flowPrefs.getLong("last_ext_check", 0)

    fun lastAnimeExtCheck() = flowPrefs.getLong("last_anime_ext_check", 0)

    fun searchPinnedSourcesOnly() = prefs.getBoolean(Keys.searchPinnedSourcesOnly, false)

    fun disabledSources() = flowPrefs.getStringSet("hidden_catalogues", emptySet())

    fun disabledAnimeSources() = flowPrefs.getStringSet("hidden_catalogues", emptySet())

    fun pinnedAnimeSources() = flowPrefs.getStringSet("pinned_anime_catalogues", emptySet())

    fun downloadNewEpisode() = flowPrefs.getBoolean(Keys.downloadNewEpisode, false)

    fun downloadNewEpisodeCategories() = flowPrefs.getStringSet("download_new_categories_anime", emptySet())

    fun downloadNewEpisodeCategoriesExclude() = flowPrefs.getStringSet("download_new_categories_exclude_anime", emptySet())

    fun lang() = flowPrefs.getString("app_language", "")

    fun defaultAnimeCategory() = prefs.getInt(Keys.defaultAnimeCategory, -1)

    fun categorizedDisplaySettings() = flowPrefs.getBoolean("categorized_display", false)

    fun skipRead() = prefs.getBoolean(Keys.skipRead, false)

    fun skipFiltered() = prefs.getBoolean(Keys.skipFiltered, true)

    fun migrateFlags() = flowPrefs.getInt("migrate_flags", Int.MAX_VALUE)

    fun trustedSignatures() = flowPrefs.getStringSet("trusted_signatures", emptySet())

    fun dohProvider() = prefs.getInt(Keys.dohProvider, -1)

    fun lastSearchQuerySearchSettings() = flowPrefs.getString("last_search_query", "")

    fun filterEpisodeBySeen() = prefs.getInt(Keys.defaultEpisodeFilterBySeen, DomainAnime.SHOW_ALL.toInt())

    fun filterEpisodeByDownloaded() = prefs.getInt(Keys.defaultEpisodeFilterByDownloaded, DomainAnime.SHOW_ALL.toInt())

    fun filterEpisodeByBookmarked() = prefs.getInt(Keys.defaultEpisodeFilterByBookmarked, DomainAnime.SHOW_ALL.toInt())

    fun filterEpisodeByFillermarked() = prefs.getInt(Keys.defaultEpisodeFilterByFillermarked, DomainAnime.SHOW_ALL.toInt())

    fun sortEpisodeBySourceOrNumber() = prefs.getInt(Keys.defaultEpisodeSortBySourceOrNumber, DomainAnime.EPISODE_SORTING_SOURCE.toInt())

    fun displayEpisodeByNameOrNumber() = prefs.getInt(Keys.defaultEpisodeDisplayByNameOrNumber, DomainAnime.EPISODE_DISPLAY_NAME.toInt())

    fun sortEpisodeByAscendingOrDescending() = prefs.getInt(Keys.defaultEpisodeSortByAscendingOrDescending, DomainAnime.EPISODE_SORT_DESC.toInt())

    fun incognitoMode() = flowPrefs.getBoolean("incognito_mode", false)

    fun tabletUiMode() = flowPrefs.getEnum("tablet_ui_mode", Values.TabletUiMode.AUTOMATIC)

    fun bottomBarLabels() = flowPrefs.getBoolean("pref_show_bottom_bar_labels", true)

    fun showNavUpdates() = flowPrefs.getBoolean("pref_show_updates_button", true)

    fun showNavHistory() = flowPrefs.getBoolean("pref_show_history_button", true)

    fun extensionInstaller() = flowPrefs.getEnum(
        "extension_installer",
        if (DeviceUtil.isMiui) Values.ExtensionInstaller.LEGACY else Values.ExtensionInstaller.PACKAGEINSTALLER,
    )

    fun verboseLogging() = prefs.getBoolean(Keys.verboseLogging, isDevFlavor)

    fun duplicatePinnedSources() = flowPrefs.getBoolean("duplicate_pinned_sources", false)

    fun enableDiscordRPC() = flowPrefs.getBoolean(Keys.enableDiscordRPC, false)

    fun discordRPCStatus() = flowPrefs.getString(Keys.discordRPCStatus, "online")

    fun setEpisodeSettingsDefault(anime: Anime) {
        prefs.edit {
            putInt(Keys.defaultEpisodeFilterBySeen, anime.seenFilter)
            putInt(Keys.defaultEpisodeFilterByDownloaded, anime.downloadedFilter)
            putInt(Keys.defaultEpisodeFilterByBookmarked, anime.bookmarkedFilter)
            putInt(Keys.defaultEpisodeFilterByFillermarked, anime.fillermarkedFilter)
            putInt(Keys.defaultEpisodeSortBySourceOrNumber, anime.sorting)
            putInt(Keys.defaultEpisodeDisplayByNameOrNumber, anime.displayMode)
            putInt(
                Keys.defaultEpisodeSortByAscendingOrDescending,
                if (anime.sortDescending()) DomainAnime.EPISODE_SORT_DESC.toInt() else DomainAnime.EPISODE_SORT_ASC.toInt(),
            )
        }
    }
}
