package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.library.anime.model.AnimeLibrarySort

@Execution(ExecutionMode.CONCURRENT)
class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        LibraryDisplayMode.values.size shouldBe 4
        AnimeLibrarySort.types.size shouldBe 11
        AnimeLibrarySort.directions.size shouldBe 2
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val animecurrent = AnimeLibrarySort(
            AnimeLibrarySort.Type.LastSeen,
            AnimeLibrarySort.Direction.Ascending,
        )
        val newanime = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )
        val animeflag = animecurrent + newanime

        animeflag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator`() {
        val animesort = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )

        animesort.flag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentanimeSort = AnimeLibrarySort(
            AnimeLibrarySort.Type.UnseenCount,
            AnimeLibrarySort.Direction.Descending,
        )
        currentanimeSort.flag shouldBe 0b00001100

        val animesort = AnimeLibrarySort(
            AnimeLibrarySort.Type.DateAdded,
            AnimeLibrarySort.Direction.Ascending,
        )
        val animeflag = animesort.flag + animesort

        animeflag shouldBe 0b01011100
        animeflag shouldNotBe currentanimeSort.flag
    }

    @Test
    fun `Test default flags`() {
        val animesort = AnimeLibrarySort.default
        val animeflag = animesort.type + animesort.direction

        animeflag shouldBe 0b01000000
    }
}
