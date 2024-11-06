// AM (DISCORD_RPC) -->
package eu.kanade.tachiyomi.data.connection.discord

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.ui.util.fastAny
import eu.kanade.domain.connection.service.ConnectionPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.ConnectionManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.player.viewer.PipState
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.category.anime.interactor.GetAnimeCategories
import tachiyomi.domain.category.model.Category.Companion.UNCATEGORIZED_ID
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.ceil
import kotlin.math.floor

class DiscordRPCService : Service() {

    private val connectionManager: ConnectionManager by injectLazy()

    override fun onCreate() {
        super.onCreate()
        val token = connectionPreferences.connectionToken(connectionManager.discord).get()
        val status = when (connectionPreferences.discordRPCStatus().get()) {
            -1 -> "dnd"
            0 -> "idle"
            else -> "online"
        }
        rpc = if (token.isNotBlank()) DiscordRPC(token, status) else null
        if (rpc != null) {
            with(DiscordRPCService) {
                discordScope.launchIO { setScreen(this@DiscordRPCService.applicationContext, lastUsedScreen) }
            }
            notification(this)
        } else {
            connectionPreferences.enableDiscordRPC().set(false)
        }
    }

    override fun onDestroy() {
        NotificationReceiver.dismissNotification(this, Notifications.ID_DISCORD_RPC)
        rpc?.closeRPC()
        rpc = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun notification(context: Context) {
        val builder = context.notificationBuilder(Notifications.CHANNEL_DISCORD_RPC) {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_discord_24dp)
            setContentText(context.stringResource(MR.strings.pref_discord_rpc))
            setAutoCancel(false)
            setOngoing(true)
            setUsesChronometer(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.ID_DISCORD_RPC,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(Notifications.ID_DISCORD_RPC, builder.build())
        }
    }

    companion object {

        private val connectionPreferences: ConnectionPreferences by injectLazy()

        internal var rpc: DiscordRPC? = null

        private val handler = Handler(Looper.getMainLooper())

        private val job = SupervisorJob()
        internal val discordScope = CoroutineScope(Dispatchers.IO + job)

        fun start(context: Context) {
            handler.removeCallbacksAndMessages(null)
            if (rpc == null && connectionPreferences.enableDiscordRPC().get()) {
                since = System.currentTimeMillis()
                context.startService(Intent(context, DiscordRPCService::class.java))
            }
        }

        fun stop(context: Context, delay: Long = 30000L) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed(
                { context.stopService(Intent(context, DiscordRPCService::class.java)) },
                delay,
            )
        }

        private var since = 0L

        internal var lastUsedScreen = DiscordScreen.APP
            set(value) {
                field = if (value == DiscordScreen.VIDEO || value == DiscordScreen.WEBVIEW) field else value
            }

        internal suspend fun setScreen(
            context: Context,
            discordScreen: DiscordScreen,
            playerData: PlayerData = PlayerData(),
        ) {
            handler.removeCallbacksAndMessages(null)
            if (PipState.mode == PipState.ON && discordScreen != DiscordScreen.VIDEO) return
            lastUsedScreen = discordScreen

            if (rpc == null) return

            val name = playerData.animeTitle ?: context.stringResource(MR.strings.app_name)

            val details = playerData.animeTitle ?: context.stringResource(discordScreen.details)

            val state = playerData.episodeNumber ?: context.stringResource(discordScreen.text)

            val imageUrl = playerData.thumbnailUrl ?: discordScreen.imageUrl

            rpc!!.updateRPC(
                activity = DiscordActivity(
                    name = name,
                    details = details,
                    state = state,
                    type = 3,
                    timestamps = DiscordActivity.Timestamps(start = since),
                    assets = DiscordActivity.Assets(
                        largeImage = "mp:$imageUrl",
                        smallImage = "mp:${DiscordScreen.APP.imageUrl}",
                        smallText = context.stringResource(DiscordScreen.APP.text),
                    ),
                ),
                since = since,
            )
        }

        internal suspend fun setPlayerActivity(context: Context, playerData: PlayerData = PlayerData()) {
            if (rpc == null || playerData.thumbnailUrl == null || playerData.animeId == null) return

            val animeCategoryIds = Injekt.get<GetAnimeCategories>()
                .await(playerData.animeId)
                .map { it.id.toString() }
                .run { ifEmpty { plus(UNCATEGORIZED_ID.toString()) } }

            val discordIncognitoMode = connectionPreferences.discordRPCIncognito().get()
            val incognitoCategories = connectionPreferences.discordRPCIncognitoCategories().get()

            val incognitoCategory = animeCategoryIds.fastAny {
                it in incognitoCategories
            }

            val discordIncognito = discordIncognitoMode || playerData.incognitoMode || incognitoCategory

            val animeTitle = playerData.animeTitle.takeUnless { discordIncognito }

            val episodeNumber = playerData.episodeNumber?.toFloatOrNull()?.let {
                when {
                    discordIncognito -> null
                    ceil(it) == floor(it) -> "Episode ${it.toInt()}"
                    else -> "Episode $it"
                }
            }

            withIOContext {
                val networkService: NetworkHelper by injectLazy()
                val client = networkService.client
                val response = if (!discordIncognito) {
                    try {
                        // Thanks to https://github.com/dead8309/Kizzy
                        client.newCall(
                            GET("https://kizzy-api.vercel.app/image?url=${playerData.thumbnailUrl}"),
                        ).execute()
                    } catch (e: Throwable) {
                        null
                    }
                } else {
                    null
                }

                val animeThumbnail = response?.body?.string()
                    ?.takeIf { !it.contains("external/Not Found") }
                    ?.substringAfter("\"id\": \"")?.substringBefore("\"}")
                    ?.split("external/")?.getOrNull(1)?.let { "external/$it" }

                // AM (DISCORD_RPC) -->
                with(DiscordRPCService) {
                    discordScope.launchIO {
                        setScreen(
                            context = context.applicationContext,
                            discordScreen = DiscordScreen.VIDEO,
                            playerData = PlayerData(
                                animeTitle = animeTitle,
                                episodeNumber = episodeNumber,
                                thumbnailUrl = animeThumbnail,
                            ),
                        )
                    }
                }
            }
        }
    }
}
// <-- AM (DISCORD_RPC)
