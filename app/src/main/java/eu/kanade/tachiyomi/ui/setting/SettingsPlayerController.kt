package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.summaryRes
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsPlayerController : SettingsController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_player

        listPreference {
            key = Keys.progressPreference
            titleRes = R.string.pref_progress_mark_as_seen

            entriesRes = arrayOf(
                R.string.pref_progress_seen_100,
                R.string.pref_progress_seen_95,
                R.string.pref_progress_seen_90,
                R.string.pref_progress_seen_85,
                R.string.pref_progress_seen_80,
                R.string.pref_progress_seen_75,
                R.string.pref_progress_seen_70
            )
            entryValues = arrayOf(
                "1.00F",
                "0.95F",
                "0.90F",
                "0.85F",
                "0.80F",
                "0.75F",
                "0.70F"
            )
            defaultValue = "0.85F"

            summary = "%s"
        }

        listPreference {
            key = Keys.unseenProgressPreference
            titleRes = R.string.pref_progress_mark_as_unseen

            entriesRes = arrayOf(
                R.string.pref_progress_unseen_0s,
                R.string.pref_progress_unseen_5s,
                R.string.pref_progress_unseen_10s,
                R.string.pref_progress_unseen_15s,
                R.string.pref_progress_unseen_20s,
                R.string.pref_progress_unseen_25s,
                R.string.pref_progress_unseen_30s
            )
            entryValues = arrayOf(
                "0",
                "5000",
                "10000",
                "15000",
                "20000",
                "25000",
                "30000"
            )
            defaultValue = "0"

            summary = "%s"
        }

        listPreference {
            key = Keys.skipLengthPreference
            titleRes = R.string.pref_skip_length

            entriesRes = arrayOf(
                R.string.pref_skip_30,
                R.string.pref_skip_20,
                R.string.pref_skip_10,
                R.string.pref_skip_5
            )
            entryValues = arrayOf(
                "30",
                "20",
                "10",
                "5"
            )
            defaultValue = "10"

            summary = "%s"
        }

        switchPreference {
            key = Keys.alwaysUseExternalPlayer
            titleRes = R.string.pref_always_use_external_player
            defaultValue = false
        }

        listPreference {
            key = Keys.externalPlayerPreference
            titleRes = R.string.pref_external_player_preference

            val pm = context.packageManager
            val installedPackages = pm.getInstalledPackages(0)
            val supportedPlayers = installedPackages.filter {
                when (it.packageName) {
                    "is.xyz.mpv" -> true
                    "com.mxtech.videoplayer" -> true
                    "com.mxtech.videoplayer.ad" -> true
                    "com.mxtech.videoplayer.pro" -> true
                    "org.videolan.vlc" -> true
                    else -> false
                }
            }
            val packageNames = supportedPlayers.map { it.packageName }
            val packageNamesReadable = supportedPlayers
                .map { pm.getApplicationLabel(it.applicationInfo).toString() }

            entries = arrayOf("None") + packageNamesReadable.toTypedArray()
            entryValues = arrayOf("") + packageNames.toTypedArray()
            defaultValue = ""

            summary = "%s"
        }

        switchPreference {
            key = Keys.pipPlayerPreference
            titleRes = R.string.pref_pip_player
            summaryRes = R.string.pref_pip_player_summary
            defaultValue = true
        }
    }
}
