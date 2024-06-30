// AM (BROWSE) -->
package eu.kanade.tachiyomi.ui.browse.anime.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.anime.AnimeExtensionScreen
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeExtensionDetailsScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AnimeExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val state by screenModel.state.collectAsState()
        val onChangeSearchQuery = screenModel::search

        AnimeExtensionScreen(
            state = state,
            navigator = navigator,
            searchQuery = state.searchQuery,
            onLongClickItem = { extension ->
                when (extension) {
                    is AnimeExtension.Available -> screenModel.installExtension(extension)
                    else -> screenModel.uninstallExtension(extension)
                }
            },
            onChangeSearchQuery = onChangeSearchQuery,
            onClickItemCancel = screenModel::cancelInstallUpdateExtension,
            onClickUpdateAll = screenModel::updateAllExtensions,
            onInstallExtension = screenModel::installExtension,
            onOpenExtension = { navigator.push(AnimeExtensionDetailsScreen(it.pkgName)) },
            onTrustExtension = { screenModel.trustExtension(it) },
            onUninstallExtension = { screenModel.uninstallExtension(it) },
            onUpdateExtension = screenModel::updateExtension,
            onRefresh = screenModel::findAvailableExtensions,
        )

        LaunchedEffect(Unit) {
            Injekt.get<AnimeExtensionManager>().findAvailableExtensions()
        }
    }
}
// <-- AM (BROWSE)
