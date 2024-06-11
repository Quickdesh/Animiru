// AM (NAVPILL) -->
package eu.kanade.tachiyomi.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private val configuration: Configuration @Composable get() = LocalConfiguration.current
private val pillItemWidth: Dp @Composable get() = (configuration.screenWidthDp / 5).dp
private val pillItemHeight: Dp @Composable get() = 48.dp

@Composable
fun NavigationPill(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
) {
    val tabMap = tabs.associateBy { it.options.index.toInt() }

    val tabNavigator = LocalTabNavigator.current
    val currTabIndex = tabNavigator.current.options.index.toInt()
    var currentIndex by remember { mutableStateOf(currTabIndex) }

    val updateTab: (Int) -> Unit = {
        if (tabMap[it] != null) {
            tabNavigator.current = tabMap[it]!!
            currentIndex = it
        }
    }

    BackHandler(
        enabled = tabNavigator.current != AnimeLibraryTab,
        onBack = { updateTab(0) },
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        var flickOffsetX by remember { mutableStateOf(0f) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            flickOffsetX += dragAmount.x
                        },
                        onDragEnd = {
                            currentIndex = when {
                                (flickOffsetX < 0F) -> {
                                    currentIndex - 1
                                }

                                (flickOffsetX > 0F) -> {
                                    currentIndex + 1
                                }

                                else -> currentIndex
                            }
                            flickOffsetX = 0F

                            updateTab(minOf(maxOf(currentIndex, 0), 4))
                        },
                    )
                },
            shape = MaterialTheme.shapes.extraLarge.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
            tonalElevation = 1.3.dp,
        ) {
            NavigationBarItemBackground(currentIndex)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    tabs.fastForEach {
                        NavigationBarItem(it, updateTab)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    tabs.fastForEach {
                        Text(
                            text = it.options.title,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = pillItemWidth * (it.options.index.toInt() - 2)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationBarItemBackground(
    currentIndex: Int,
) {
    val offset: Dp by animateDpAsState(
        targetValue = pillItemWidth * (currentIndex - 2),
        animationSpec = tween(500),
    )

    Surface(
        modifier = Modifier.offset(x = offset).requiredWidthIn(max = pillItemWidth),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier = Modifier
                .size(width = pillItemWidth, height = pillItemHeight)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
    }
}

@Composable
private fun NavigationBarItem(tab: Tab, updateTab: (Int) -> Unit) {
    val tabNavigator = LocalTabNavigator.current
    val navigator = LocalNavigator.currentOrThrow

    val scope = rememberCoroutineScope()
    val selected = tabNavigator.current::class == tab::class
    val tabIndex = tab.options.index.toInt()
    val onClick: () -> Unit = {
        if (!selected) {
            updateTab(tabIndex)
        } else {
            scope.launch { tab.onReselect(navigator) }
        }
    }

    Box(
        modifier = Modifier
            .size(width = pillItemWidth, height = pillItemHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        NavigationIconItem(tab)
    }
}

@Composable
private fun NavigationIconItem(tab: Tab) {
    BadgedBox(
        badge = {
            when {
                UpdatesTab::class.isInstance(tab) -> {
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<LibraryPreferences>()
                        pref.newAnimeUpdatesCount().changes()
                            .collectLatest { value = if (pref.newShowUpdatesCount().get()) it else 0 }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                id = R.plurals.notification_episodes_generic,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
                BrowseTab::class.isInstance(tab) -> {
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<SourcePreferences>()
                        pref.animeExtensionUpdatesCount().changes()
                            .collectLatest { value = it }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                id = R.plurals.update_check_notification_ext_updates,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Icon(
            painter = tab.options.icon!!,
            contentDescription = tab.options.title,
            modifier = Modifier.size(28.dp),
        )
    }
}

// <-- AM (NAVPILL)
