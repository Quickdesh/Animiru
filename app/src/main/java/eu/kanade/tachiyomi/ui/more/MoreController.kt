package eu.kanade.tachiyomi.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.AnimeDownloadService
import eu.kanade.tachiyomi.ui.base.controller.NoAppBarElevationController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryController
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesController
import eu.kanade.tachiyomi.ui.setting.SettingsBackupController
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.util.preference.*
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy
import eu.kanade.tachiyomi.ui.animecategory.CategoryController as AnimeCategoryController

class MoreController :
    SettingsController(),
    RootController,
    NoAppBarElevationController {

    private val animedownloadManager: AnimeDownloadManager by injectLazy()
    private var isDownloading: Boolean = false
    private var isDownloadingAnime: Boolean = false
    private var downloadQueueSize: Int = 0
    private var downloadQueueSizeAnime: Int = 0

    private var untilDestroySubscriptions = CompositeSubscription()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.label_more

        val tintColor = context.getResourceColor(R.attr.colorAccent)

        add(MoreHeaderPreference(context))

        switchPreference {
            bindTo(preferences.downloadedOnly())
            titleRes = R.string.label_downloaded_only
            summaryRes = R.string.downloaded_only_summary
            iconRes = R.drawable.ic_cloud_off_24dp
            iconTint = tintColor
        }

        switchPreference {
            bindTo(preferences.incognitoMode())
            summaryRes = R.string.pref_incognito_mode_summary
            titleRes = R.string.pref_incognito_mode
            iconRes = R.drawable.ic_glasses_24dp
            iconTint = tintColor

            preferences.incognitoMode().asFlow()
                .onEach { isChecked = it }
                .launchIn(viewScope)
        }

        preferenceCategory {
            if (!preferences.showNavUpdates().get()) {
                preference {
                    titleRes = R.string.label_recent_updates
                    iconRes = R.drawable.ic_updates_outline_24dp
                    iconTint = tintColor
                    onClick {
                        router.pushController(AnimeUpdatesController().withFadeTransaction())
                    }
                }
            }
            if (!preferences.showNavHistory().get()) {
                preference {
                    titleRes = R.string.label_recent_history
                    iconRes = R.drawable.ic_history_24dp
                    iconTint = tintColor
                    onClick {
                        router.pushController(AnimeHistoryController().withFadeTransaction())
                    }
                }
            }
            preference {
                titleRes = R.string.label_download_queue

                if (animedownloadManager.queue.isNotEmpty()) {
                    initDownloadQueueSummary(this)
                }

                iconRes = R.drawable.ic_get_app_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(DownloadController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.anime_categories
                iconRes = R.drawable.ic_label_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(AnimeCategoryController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.label_backup
                iconRes = R.drawable.ic_settings_backup_restore_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(SettingsBackupController().withFadeTransaction())
                }
            }
        }

        preferenceCategory {
            preference {
                titleRes = R.string.label_settings
                iconRes = R.drawable.ic_settings_24dp
                iconTint = tintColor
                onClick {
                    router.pushController(SettingsMainController().withFadeTransaction())
                }
            }
            preference {
                iconRes = R.drawable.ic_info_24dp
                iconTint = tintColor
                titleRes = R.string.pref_category_about
                onClick {
                    router.pushController(AboutController().withFadeTransaction())
                }
            }
            preference {
                titleRes = R.string.label_help
                iconRes = R.drawable.ic_help_24dp
                iconTint = tintColor
                onClick {
                    activity?.openInBrowser(URL_HELP)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
    }

    private fun initDownloadQueueSummary(preference: Preference) {
        // Handle running/paused status change

        AnimeDownloadService.runningRelay
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy { isRunning ->
                isDownloadingAnime = isRunning
                updateDownloadQueueSummary(preference)
            }

        // Handle queue progress updating

        animedownloadManager.queue.getUpdatedObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeUntilDestroy {
                downloadQueueSizeAnime = it.size
                updateDownloadQueueSummary(preference)
            }
    }

    private fun updateDownloadQueueSummary(preference: Preference) {
        val pendingDownloadExists = downloadQueueSize + downloadQueueSizeAnime != 0
        val pauseMessage = resources?.getString(R.string.paused)
        val numberOfPendingDownloads = resources?.getQuantityString(R.plurals.download_queue_summary, downloadQueueSize + downloadQueueSizeAnime, downloadQueueSize + downloadQueueSizeAnime)

        preference.summary = when {
            !pendingDownloadExists -> null
            !isDownloading && !isDownloadingAnime && !pendingDownloadExists -> pauseMessage
            !isDownloading && !isDownloadingAnime && pendingDownloadExists -> "$pauseMessage • $numberOfPendingDownloads"
            else -> numberOfPendingDownloads
        }
    }

    private fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }

    companion object {
        const val URL_HELP = "https://aniyomi.jmir.xyz/help/"
    }
}
