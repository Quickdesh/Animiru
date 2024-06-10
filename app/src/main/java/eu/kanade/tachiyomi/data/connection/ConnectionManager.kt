// AM (CONNECTION) -->
package eu.kanade.tachiyomi.data.connection

import eu.kanade.tachiyomi.data.connection.discord.Discord
import eu.kanade.tachiyomi.data.connection.syncmiru.GoogleDrive
import eu.kanade.tachiyomi.data.connection.syncmiru.SyncMiru
import eu.kanade.tachiyomi.data.connection.syncmiru.SyncYomi

class ConnectionManager {

    companion object {
        // AM (DISCORD) -->
        const val DISCORD = 201L
        // <-- AM (DISCORD)

        // AM (SYNC) -->
        const val SYNCMIRU = 202L

        // AM (SYNC_DRIVE) -->
        const val GOOGLEDRIVE = 203L
        // <-- AM (SYNC_DRIVE)

        // AM (SYNC_YOMI) -->
        const val SYNCYOMI = 204L
        // <-- AM (SYNC_YOMI)

        // <-- AM (SYNC)
    }

    // AM (DISCORD) -->
    val discord = Discord(DISCORD)
    // <-- AM (DISCORD)

    // AM (SYNC) -->
    val syncmiru = SyncMiru(SYNCMIRU)

    // AM (SYNC_DRIVE) -->
    val googleDrive = GoogleDrive(GOOGLEDRIVE)
    // <-- AM (SYNC_DRIVE)

    // AM (SYNC_YOMI) -->
    val syncyomi = SyncYomi(SYNCYOMI)
    // <-- AM (SYNC_YOMI)

    // <-- AM (SYNC)

    val services: List<BaseConnection> = listOf(discord, syncmiru)
}
// <-- AM (CONNECTION)
