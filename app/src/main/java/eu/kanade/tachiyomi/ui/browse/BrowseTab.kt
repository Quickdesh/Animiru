package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.source.anime.model.installedExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.anime.AnimeSourceOptionsDialog
import eu.kanade.presentation.browse.anime.AnimeSourcesScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService
import eu.kanade.tachiyomi.data.connections.discord.DiscordScreen
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.AnimeSourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

data class BrowseTab(
    private var toExtensions: Boolean,
) : Tab() {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(AnimeExtensionsScreen())
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        // AM (BROWSE) -->
        val snackbarHostState = SnackbarHostState()
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeSourcesScreenModel() }
        val state by screenModel.state.collectAsState()
        val sourcePreferences: SourcePreferences by injectLazy()
        val repos by sourcePreferences.animeExtensionRepos().collectAsState()

        AnimeSourcesScreen(
            state = state,
            navigator = navigator,
            onClickItem = { source, listing ->
                navigator.push(BrowseAnimeSourceScreen(source.id, listing.query))
            },
            onClickPin = screenModel::togglePin,
            onLongClickItem = screenModel::showSourceDialog,
            sourcePreferences = sourcePreferences,
        )

        state.dialog?.let { dialog ->
            val source = dialog.source
            AnimeSourceOptionsDialog(
                source = source,
                onClickPin = {
                    screenModel.togglePin(source)
                    screenModel.closeDialog()
                },
                onClickDisable = {
                    screenModel.toggleSource(source)
                    screenModel.closeDialog()
                },
                onClickUpdate = {
                    screenModel.updateExtension(source.installedExtension)
                    screenModel.closeDialog()
                },
                onClickUninstall = {
                    screenModel.uninstallExtension(source.installedExtension)
                    screenModel.closeDialog()
                },
                onDismiss = screenModel::closeDialog,
            )
        }
        // <-- AM (BROWSE)

        LaunchedEffect(Unit) {
            if (toExtensions) {
                toExtensions = false
                navigator.push(AnimeExtensionsScreen())
            }
            (context as? MainActivity)?.ready = true
        }

        // AM (BROWSE) -->
        val internalErrString = stringResource(MR.strings.internal_error)
        LaunchedEffect(Unit) {
            // AM (DISCORD) -->
            DiscordRPCService.setScreen(context, DiscordScreen.BROWSE)
            // <-- AM (DISCORD)
            Injekt.get<AnimeExtensionManager>().findAvailableExtensions()
            screenModel.events.collectLatest { event ->
                when (event) {
                    AnimeSourcesScreenModel.Event.FailedFetchingSources -> {
                        launch { snackbarHostState.showSnackbar(internalErrString) }
                    }
                }
            }
        }
        // <-- AM (BROWSE)
    }
}
