package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val DISABLED_ALPHA = .38f
const val SECONDARY_ALPHA = .78f

class Padding {

    // AM (BROWSE) -->
    val superLarge = 72.dp
    // <-- AM (BROWSE)

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val small = 8.dp

    val extraSmall = 4.dp

    // AM (BROWSE) -->
    val none = 0.dp
    // <-- AM (BROWSE)
}

val MaterialTheme.padding: Padding
    get() = Padding()
