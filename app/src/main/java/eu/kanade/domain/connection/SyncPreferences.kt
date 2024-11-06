// AM (SYNC) -->
package eu.kanade.domain.connection

import eu.kanade.domain.connection.models.SyncSettings
import eu.kanade.tachiyomi.data.connection.syncmiru.models.SyncTriggerOptions
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.util.UUID

class SyncPreferences(
    private val preferenceStore: PreferenceStore,
) {
    // AM (SYNC_YOMI) -->
    fun clientHost() = preferenceStore.getString("connection_sync_client_host", "https://sync.animiru.net")
    fun clientAPIKey() = preferenceStore.getString("connection_sync_client_api_key", "")
    // <-- AM (SYNC_YOMI)

    fun lastSyncTimestamp() = preferenceStore.getLong(Preference.appStateKey("last_sync_timestamp"), 0L)

    fun lastSyncEtag() = preferenceStore.getString("sync_etag", "")

    fun syncInterval() = preferenceStore.getInt("sync_interval", 0)

    // AM (SYNC_DRIVE) -->
    fun googleDriveAccessToken() = preferenceStore.getString(
        Preference.appStateKey("connection_google_drive_access_token"),
        "",
    )
    fun googleDriveRefreshToken() = preferenceStore.getString(
        Preference.appStateKey("connection_google_drive_refresh_token"),
        "",
    )
    // <-- AM (SYNC_DRIVE)

    fun isSyncEnabled() = googleDriveRefreshToken().get().isNotBlank() || clientAPIKey().get().isNotBlank()

    fun uniqueDeviceID(): String {
        val uniqueIDPreference = preferenceStore.getString("unique_device_id", "")

        // Retrieve the current value of the preference
        var uniqueID = uniqueIDPreference.get()
        if (uniqueID.isBlank()) {
            uniqueID = UUID.randomUUID().toString()
            uniqueIDPreference.set(uniqueID)
        }

        return uniqueID
    }

    fun getSyncSettings(): SyncSettings {
        return SyncSettings(
            animelibEntries = preferenceStore.getBoolean("sync_anime_lib_entries", true).get(),
            animeCategories = preferenceStore.getBoolean("sync_anime_categories", true).get(),
            episodes = preferenceStore.getBoolean("sync_episodes", true).get(),
            animeTracking = preferenceStore.getBoolean("sync_anime_tracking", true).get(),
            animeHistory = preferenceStore.getBoolean("sync_anime_history", true).get(),
            appSettings = preferenceStore.getBoolean("sync_app_settings", true).get(),
            sourceSettings = preferenceStore.getBoolean("sync_source_settings", true).get(),
            privateSettings = preferenceStore.getBoolean("sync_private_settings", true).get(),
            // AM (CUSTOM_INFORMATION) -->
            customInfo = preferenceStore.getBoolean("sync_custom_information", true).get(),
            // <-- AM (CUSTOM_INFORMATION)
        )
    }

    fun setSyncSettings(syncSettings: SyncSettings) {
        preferenceStore.getBoolean("sync_anime_lib_entries", true).set(syncSettings.animelibEntries)
        preferenceStore.getBoolean("sync_anime_categories", true).set(syncSettings.animeCategories)
        preferenceStore.getBoolean("sync_episodes", true).set(syncSettings.episodes)
        preferenceStore.getBoolean("sync_anime_tracking", true).set(syncSettings.animeTracking)
        preferenceStore.getBoolean("sync_app_settings", true).set(syncSettings.appSettings)
        preferenceStore.getBoolean("sync_source_settings", true).set(syncSettings.sourceSettings)
        preferenceStore.getBoolean("sync_private_settings", true).set(syncSettings.privateSettings)
        // AM (CUSTOM_INFORMATION) -->
        preferenceStore.getBoolean("sync_custom_information", true).set(syncSettings.customInfo)
        // <-- AM (CUSTOM_INFORMATION)
    }

    fun getSyncTriggerOptions(): SyncTriggerOptions {
        return SyncTriggerOptions(
            syncOnAppStart = preferenceStore.getBoolean("sync_on_app_start", false).get(),
            syncOnAppResume = preferenceStore.getBoolean("sync_on_app_resume", false).get(),
            syncOnEpisodeSeen = preferenceStore.getBoolean("sync_on_episode_seen", false).get(),
            syncOnEpisodeOpen = preferenceStore.getBoolean("sync_on_episode_open", false).get(),
        )
    }

    fun setSyncTriggerOptions(syncTriggerOptions: SyncTriggerOptions) {
        preferenceStore.getBoolean("sync_on_app_start", false)
            .set(syncTriggerOptions.syncOnAppStart)
        preferenceStore.getBoolean("sync_on_app_resume", false)
            .set(syncTriggerOptions.syncOnAppResume)

        // Anime
        preferenceStore.getBoolean("sync_on_episode_seen", false)
            .set(syncTriggerOptions.syncOnEpisodeSeen)
        preferenceStore.getBoolean("sync_on_episode_open", false)
            .set(syncTriggerOptions.syncOnEpisodeOpen)
    }
}
// <-- AM (SYNC)
