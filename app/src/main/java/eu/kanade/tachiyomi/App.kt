package eu.kanade.tachiyomi

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.util.DebugLogger
import eu.kanade.domain.DomainModule
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.connection.SyncPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.tachiyomi.crash.CrashActivity
import eu.kanade.tachiyomi.crash.GlobalExceptionHandler
import eu.kanade.tachiyomi.data.coil.AnimeCoverFetcher
import eu.kanade.tachiyomi.data.coil.AnimeCoverKeyer
import eu.kanade.tachiyomi.data.coil.AnimeKeyer
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.data.connection.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connection.syncmiru.SyncDataJob
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.di.AppModule
import eu.kanade.tachiyomi.di.PreferenceModule
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.LogcatLogger
import org.conscrypt.Conscrypt
import tachiyomi.core.i18n.stringResource
import tachiyomi.core.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.widget.entries.anime.AnimeWidgetManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.security.Security

class App : Application(), DefaultLifecycleObserver, ImageLoaderFactory {

    private val basePreferences: BasePreferences by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()

    // AM (SYNC) -->
    private val syncPreferences: SyncPreferences by injectLazy()
    // <-- AM (SYNC)

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        super<Application>.onCreate()

        // AM (REMOVE_ACRA_FIREBASE) -->
        // setupAcra()
        // <-- AM (REMOVE_ACRA_FIREBASE)
        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)

        // TLS 1.3 support for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // Avoid potential crashes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        Injekt.importModule(PreferenceModule(this))
        Injekt.importModule(AppModule(this))
        Injekt.importModule(DomainModule())

        setupNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Show notification to disable Incognito Mode when it's enabled
        basePreferences.incognitoMode().changes()
            .onEach { enabled ->
                if (enabled) {
                    disableIncognitoReceiver.register()
                    notify(
                        Notifications.ID_INCOGNITO_MODE,
                        Notifications.CHANNEL_INCOGNITO_MODE,
                    ) {
                        setContentTitle(stringResource(MR.strings.pref_incognito_mode))
                        setContentText(stringResource(MR.strings.notification_incognito_text))
                        setSmallIcon(R.drawable.ic_glasses_24dp)
                        setOngoing(true)

                        val pendingIntent = PendingIntent.getBroadcast(
                            this@App,
                            0,
                            Intent(ACTION_DISABLE_INCOGNITO_MODE),
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        setContentIntent(pendingIntent)
                    }
                } else {
                    disableIncognitoReceiver.unregister()
                    cancelNotification(Notifications.ID_INCOGNITO_MODE)
                }
            }
            .launchIn(ProcessLifecycleOwner.get().lifecycleScope)

        setAppCompatDelegateThemeMode(Injekt.get<UiPreferences>().themeMode().get())

        // Updates widget update
        with(AnimeWidgetManager(Injekt.get(), Injekt.get())) {
            init(ProcessLifecycleOwner.get().lifecycleScope)
        }

        // AM (SYNC) -->
        startSyncJob(syncPreferences.getSyncTriggerOptions().syncOnAppStart)
        // <-- AM (SYNC)

        if (!LogcatLogger.isInstalled && networkPreferences.verboseLogging().get()) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).apply {
            val callFactoryInit = { Injekt.get<NetworkHelper>().client }
            val diskCacheInit = { CoilDiskCache.get(this@App) }
            components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(TachiyomiImageDecoder.Factory())
                add(AnimeCoverFetcher.AnimeFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                add(AnimeCoverFetcher.AnimeCoverFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                add(AnimeKeyer())
                add(AnimeCoverKeyer())
            }
            callFactory(callFactoryInit)
            diskCache(diskCacheInit)
            diskCache(diskCacheInit)
            crossfade((300 * this@App.animatorDurationScale).toInt())
            allowRgb565(DeviceUtil.isLowRamDevice(this@App))
            if (networkPreferences.verboseLogging().get()) logger(DebugLogger())

            // Coil spawns a new thread for every image load by default
            fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
            decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
            transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
        }.build()
    }

    override fun onStart(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStart()
        // AM (DISCORD_RPC) -->
        DiscordRPCService.start(applicationContext)
        // <-- AM (DISCORD_RPC)
        // AM (SYNC) -->
        startSyncJob(syncPreferences.getSyncTriggerOptions().syncOnAppResume)
        // <-- AM (SYNC)
    }

    override fun onStop(owner: LifecycleOwner) {
        SecureActivityDelegate.onApplicationStopped()
        // AM (DISCORD_RPC) -->
        DiscordRPCService.stop(applicationContext, 10000L)
        // <-- AM (DISCORD_RPC)
        // AM (SYNC) -->
        startSyncJob(syncPreferences.getSyncTriggerOptions().syncOnAppStart)
        // <-- AM (SYNC)
    }

    override fun getPackageName(): String {
        // This causes freezes in Android 6/7 for some reason
        try {
            // Override the value passed as X-Requested-With in WebView requests
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val chromiumElement = stackTrace.find {
                it.className.equals(
                    "org.chromium.base.BuildInfo",
                    ignoreCase = true,
                )
            }
            if (chromiumElement?.methodName.equals("getAll", ignoreCase = true)) {
                return WebViewUtil.SPOOF_PACKAGE_NAME
            }
        } catch (_: Exception) {
        }
        return super.getPackageName()
    }

    // AM (REMOVE_ACRA_FIREBASE) -->
    // private fun setupAcra() {
    //     if (BuildConfig.ACRA_URI.isNotEmpty() && isPreviewBuildType || isReleaseBuildType) {
    //         initAcra {
    //             buildConfigClass = BuildConfig::class.java
    //             excludeMatchingSharedPreferencesKeys = listOf(
    //                 Preference.privateKey(".*"), ".*username.*", ".*password.*", ".*token.*",
    //             )
    //
    //             reportFormat = StringFormat.JSON
    //             httpSender {
    //                 uri = BuildConfig.ACRA_URI
    //                 basicAuthLogin = BuildConfig.ACRA_LOGIN
    //                 basicAuthPassword = BuildConfig.ACRA_PASSWORD
    //                 httpMethod = HttpSender.Method.POST
    //             }
    //
    //             scheduler {
    //                 requiresBatteryNotLow = true
    //                 requiresDeviceIdle = true
    //                 requiresNetworkType = JobInfo.NETWORK_TYPE_UNMETERED
    //             }
    //         }
    //     }
    // }
    // <-- AM (REMOVE_ACRA_FIREBASE)

    private fun setupNotificationChannels() {
        try {
            Notifications.createChannels(this)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to modify notification channels" }
        }
    }

    private inner class DisableIncognitoReceiver : BroadcastReceiver() {
        private var registered = false

        override fun onReceive(context: Context, intent: Intent) {
            basePreferences.incognitoMode().set(false)
        }

        fun register() {
            if (!registered) {
                ContextCompat.registerReceiver(
                    this@App,
                    this,
                    IntentFilter(ACTION_DISABLE_INCOGNITO_MODE),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                unregisterReceiver(this)
                registered = false
            }
        }
    }

    // AM (SYNC) -->
    private fun startSyncJob(syncTriggerOption: Boolean) {
        if (syncPreferences.isSyncEnabled() && syncTriggerOption) {
            SyncDataJob.startNow(this@App)
        }
    }
    // <-- AM (SYNC)
}

private const val ACTION_DISABLE_INCOGNITO_MODE = "tachi.action.DISABLE_INCOGNITO_MODE"

/**
 * Direct copy of Coil's internal SingletonDiskCache so that [AnimeCoverFetcher] can access it.
 */
private object CoilDiskCache {

    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
        return instance ?: run {
            val safeCacheDir = context.cacheDir.apply { mkdirs() }
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(safeCacheDir.resolve(FOLDER_NAME))
                .build()
                .also { instance = it }
        }
    }
}
