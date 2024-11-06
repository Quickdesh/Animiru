// AM (CONNECTION) -->
package eu.kanade.domain.connection.service

import eu.kanade.tachiyomi.data.connection.Connection
import tachiyomi.core.common.preference.PreferenceStore

class ConnectionPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun connectionUsername(connection: Connection) = preferenceStore.getString(
        connectionUsername(connection.id),
        "",
    )

    fun connectionPassword(connection: Connection) = preferenceStore.getString(
        connectionPassword(connection.id),
        "",
    )

    fun setConnectionCredentials(connection: Connection, username: String, password: String) {
        connectionUsername(connection).set(username)
        connectionPassword(connection).set(password)
    }

    fun connectionToken(connection: Connection) = preferenceStore.getString(connectionToken(connection.id), "")

    // AM (DISCORD_RPC) -->
    fun enableDiscordRPC() = preferenceStore.getBoolean("pref_enable_discord_rpc", false)

    fun discordRPCStatus() = preferenceStore.getInt("pref_discord_rpc_status", 1)

    fun discordRPCIncognito() = preferenceStore.getBoolean("pref_discord_rpc_incognito", false)

    fun discordRPCIncognitoCategories() = preferenceStore.getStringSet("discord_rpc_incognito_categories", emptySet())
    // <-- AM (DISCORD_RPC)

    companion object {

        fun connectionUsername(connectionId: Long) = "pref_anime_connections_username_$connectionId"

        private fun connectionPassword(connectionId: Long) = "pref_anime_connections_password_$connectionId"

        private fun connectionToken(connectionId: Long) = "connection_token_$connectionId"
    }
}
// <-- AM (CONNECTION)
