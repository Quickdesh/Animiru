package eu.kanade.domain.source.service

import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.preference.getEnum
import tachiyomi.domain.library.model.LibraryDisplayMode

class SourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    // Common options

    fun sourceDisplayMode() = preferenceStore.getObject(
        "pref_display_mode_catalogue",
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    fun enabledLanguages() = preferenceStore.getStringSet(
        "source_languages",
        LocaleHelper.getDefaultEnabledLanguages(),
    )

    fun showNsfwSource() = preferenceStore.getBoolean("show_nsfw_source", true)

    fun migrationSortingMode() = preferenceStore.getEnum(
        "pref_migration_sorting",
        SetMigrateSorting.Mode.ALPHABETICAL,
    )

    fun migrationSortingDirection() = preferenceStore.getEnum(
        "pref_migration_direction",
        SetMigrateSorting.Direction.ASCENDING,
    )

    fun animeExtensionRepos() = preferenceStore.getStringSet("anime_extension_repos", emptySet())

    fun trustedExtensions() = preferenceStore.getStringSet(
        Preference.appStateKey("trusted_extensions"),
        emptySet(),
    )

    // Mixture Sources

    fun disabledAnimeSources() = preferenceStore.getStringSet("hidden_anime_catalogues", emptySet())

    fun pinnedAnimeSources() = preferenceStore.getStringSet("pinned_anime_catalogues", emptySet())

    fun lastUsedAnimeSource() = preferenceStore.getLong(
        Preference.appStateKey("last_anime_catalogue_source"),
        -1,
    )

    fun animeExtensionUpdatesCount() = preferenceStore.getInt("animeext_updates_count", 0)

    fun hideInAnimeLibraryItems() = preferenceStore.getBoolean(
        "browse_hide_in_anime_library_items",
        false,
    )
}
