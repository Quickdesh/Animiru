package eu.kanade.tachiyomi.ui.browse.migration.search

import android.app.Dialog
import android.os.Bundle
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.anime.AnimeController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchController
import eu.kanade.tachiyomi.ui.browse.animesource.globalsearch.GlobalAnimeSearchPresenter
import eu.kanade.tachiyomi.ui.browse.migration.AnimeMigrationFlags
import uy.kohesive.injekt.injectLazy

class AnimeSearchController(
    private var anime: Anime? = null
) : GlobalAnimeSearchController(anime?.originalTitle) {

    private var newAnime: Anime? = null

    override fun createPresenter(): GlobalAnimeSearchPresenter {
        return AnimeSearchPresenter(
            initialQuery,
            anime!!
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(::anime.name, anime)
        outState.putSerializable(::newAnime.name, newAnime)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        anime = savedInstanceState.getSerializable(::anime.name) as? Anime
        newAnime = savedInstanceState.getSerializable(::newAnime.name) as? Anime
    }

    fun migrateAnime(anime: Anime? = null, newAnime: Anime?) {
        anime ?: return
        newAnime ?: return

        (presenter as? AnimeSearchPresenter)?.migrateAnime(anime, newAnime, true)
    }

    fun copyAnime(anime: Anime? = null, newAnime: Anime?) {
        anime ?: return
        newAnime ?: return

        (presenter as? AnimeSearchPresenter)?.migrateAnime(anime, newAnime, false)
    }

    override fun onAnimeClick(anime: Anime) {
        newAnime = anime
        val dialog =
            MigrationDialog(this.anime, newAnime, this)
        dialog.targetController = this
        dialog.showDialog(router)
    }

    override fun onAnimeLongClick(anime: Anime) {
        // Call parent's default click listener
        super.onAnimeClick(anime)
    }

    fun renderIsReplacingAnime(isReplacingAnime: Boolean, newAnime: Anime?) {
        binding.progress.isVisible = isReplacingAnime
        if (!isReplacingAnime) {
            router.popController(this)
            if (newAnime != null) {
                val newAnimeController = RouterTransaction.with(AnimeController(newAnime))
                if (router.backstack.last().controller is AnimeController) {
                    // Replace old AnimeController
                    router.replaceTopController(newAnimeController)
                } else {
                    // Push AnimeController on top of MigrationController
                    router.pushController(newAnimeController)
                }
            }
        }
    }

    class MigrationDialog(private val anime: Anime? = null, private val newAnime: Anime? = null, private val callingController: Controller? = null) : DialogController() {

        private val preferences: PreferencesHelper by injectLazy()

        @Suppress("DEPRECATION")
        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val prefValue = preferences.migrateFlags().get()
            val enabledFlagsPositions = AnimeMigrationFlags.getEnabledFlagsPositions(prefValue)
            val items = AnimeMigrationFlags.titles
                .map { resources?.getString(it) }
                .toTypedArray()
            val selected = items
                .mapIndexed { i, _ -> enabledFlagsPositions.contains(i) }
                .toBooleanArray()

            return MaterialAlertDialogBuilder(activity!!)
                .setTitle(R.string.migration_dialog_what_to_include)
                .setMultiChoiceItems(items, selected) { _, which, checked ->
                    selected[which] = checked
                }
                .setPositiveButton(R.string.migrate) { _, _ ->
                    // Save current settings for the next time
                    val selectedIndices = mutableListOf<Int>()
                    selected.forEachIndexed { i, b -> if (b) selectedIndices.add(i) }
                    val newValue = AnimeMigrationFlags.getFlagsFromPositions(selectedIndices.toTypedArray())
                    preferences.migrateFlags().set(newValue)

                    if (callingController != null) {
                        if (callingController.javaClass == AnimeSourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? AnimeSearchController)?.migrateAnime(anime, newAnime)
                }
                .setNegativeButton(R.string.copy) { _, _, ->
                    if (callingController != null) {
                        if (callingController.javaClass == AnimeSourceSearchController::class.java) {
                            router.popController(callingController)
                        }
                    }
                    (targetController as? AnimeSearchController)?.copyAnime(anime, newAnime)
                }
                .setNeutralButton(android.R.string.cancel, null)
                .create()
        }
    }

    override fun onTitleClick(source: AnimeCatalogueSource) {
        presenter.preferences.lastUsedAnimeSource().set(source.id)

        router.pushController(AnimeSourceSearchController(anime, source, presenter.query).withFadeTransaction())
    }
}
