package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.App
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.domain.library.service.LibraryPreferences

class MergeSortTypeDirectionMigration : Migration {
    override val version = 82f

    // Merge Sort Type and Direction into one class
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<App>() ?: return false
        val libraryPreferences = migrationContext.get<LibraryPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        prefs.edit {
            val animesort = prefs.getString(
                libraryPreferences.animeSortingMode().key(),
                null,
            ) ?: return@edit
            val direction = prefs.getString("library_sorting_ascending", "ASCENDING")!!
            putString(libraryPreferences.animeSortingMode().key(), "$animesort,$direction")
            remove("library_sorting_ascending")
        }

        return true
    }
}
