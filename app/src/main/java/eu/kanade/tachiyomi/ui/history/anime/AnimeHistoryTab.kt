package eu.kanade.tachiyomi.ui.history.anime

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.history.HistoryDeleteAllDialog
import eu.kanade.presentation.history.HistoryDeleteDialog
import eu.kanade.presentation.history.anime.AnimeHistoryScreen
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recents.openEpisode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.core.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

val resumeLastEpisodeSeenEvent = Channel<Unit>()

// AM (TAB_HOLD) -->
val snackbarHostState = SnackbarHostState()
// <-- AM (TAB_HOLD)

@Composable
fun Screen.animeHistoryTab(
    context: Context,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { AnimeHistoryScreenModel() }
    val state by screenModel.state.collectAsState()
    val searchQuery by screenModel.query.collectAsState()

    return TabContent(
        // AM (RECENTS) -->
        titleRes = MR.strings.label_recent_manga,
        // <-- AM (RECENTS)
        searchEnabled = true,
        content = { contentPadding, _ ->
            AnimeHistoryScreen(
                state = state,
                searchQuery = searchQuery,
                snackbarHostState = snackbarHostState,
                onClickCover = { navigator.push(AnimeScreen(it)) },
                onClickResume = screenModel::getNextEpisodeForAnime,
                onDialogChange = screenModel::setDialog,
            )

            val onDismissRequest = { screenModel.setDialog(null) }
            when (val dialog = state.dialog) {
                is AnimeHistoryScreenModel.Dialog.Delete -> {
                    HistoryDeleteDialog(
                        onDismissRequest = onDismissRequest,
                        onDelete = { all ->
                            if (all) {
                                screenModel.removeAllFromHistory(dialog.history.animeId)
                            } else {
                                screenModel.removeFromHistory(dialog.history)
                            }
                        },
                        isManga = false,
                    )
                }
                is AnimeHistoryScreenModel.Dialog.DeleteAll -> {
                    HistoryDeleteAllDialog(
                        onDismissRequest = onDismissRequest,
                        onDelete = screenModel::removeAllHistory,
                    )
                }
                null -> {}
            }

            LaunchedEffect(state.list) {
                if (state.list != null) {
                    (context as? MainActivity)?.ready = true
                }
            }

            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { e ->
                    when (e) {
                        AnimeHistoryScreenModel.Event.InternalError ->
                            snackbarHostState.showSnackbar(context.stringResource(MR.strings.internal_error))
                        AnimeHistoryScreenModel.Event.HistoryCleared ->
                            snackbarHostState.showSnackbar(context.stringResource(MR.strings.clear_history_completed))
                        is AnimeHistoryScreenModel.Event.OpenEpisode -> openEpisode(context, e.episode)
                    }
                }
            }
        },
        actions =
        persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.pref_clear_history),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { screenModel.setDialog(AnimeHistoryScreenModel.Dialog.DeleteAll) },
            ),
        ),
        navigateUp = null,
    )
}
