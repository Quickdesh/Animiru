// AM (NAVIGATION_PILL) -->
package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.recents.RecentsTab
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs

@Composable
fun NavigationPill(
    tabs: List<Tab>,
    currentTabIndex: Int,
    setCurrentTabIndex: (Int) -> Unit,
    labelFade: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val tabNavigator = LocalTabNavigator.current
    val configuration = LocalConfiguration.current

    val pillItemWidth = (configuration.screenWidthDp / tabs.size).dp
    val pillItemHeight = 48.dp

    val tabMap = tabs.associateBy { it.options.index.toInt() }
    var oldIndex by remember { mutableIntStateOf(currentTabIndex) }

    val updateTab: (Int) -> Unit = {
        if (tabMap[it] != null) {
            tabNavigator.current = tabMap[it]!!
            setCurrentTabIndex(it)
        }
    }

    val navigationOffsetX: Dp by animateDpAsState(
        targetValue = pillItemWidth * (currentTabIndex - getOffsetX(tabs.size)),
        animationSpec = tween(labelFade * 2),
    )

    BackHandler(
        enabled = tabNavigator.current != AnimeLibraryTab,
        onBack = { updateTab(0) },
    )

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        var flickOffsetX by remember { mutableFloatStateOf(0f) }

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
                            val newIndex = when {
                                (flickOffsetX < 0F) -> oldIndex - 1
                                (flickOffsetX > 0F) -> oldIndex + 1
                                else -> oldIndex
                            }

                            flickOffsetX = 0F

                            updateTab(minOf(maxOf(newIndex, 0), tabs.size - 1))
                        },
                    )
                },
            tonalElevation = 1.4.dp,
        ) {
            val topStartCornerSize = remember { Animatable(0f) }
            val topEndCornerSize = remember { Animatable(28f) }
            val bottomStartCornerSize = remember { Animatable(0f) }
            val bottomEndCornerSize = remember { Animatable(28f) }

            NavigationPillItemBackground(
                pillItemWidth = pillItemWidth,
                pillItemHeight = pillItemHeight,
                pillOffsetX = navigationOffsetX,
                topStartCornerSize = topStartCornerSize.value.dp,
                topEndCornerSize = topEndCornerSize.value.dp,
                bottomStartCornerSize = bottomStartCornerSize.value.dp,
                bottomEndCornerSize = bottomEndCornerSize.value.dp,
            )
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    tabs.fastForEach {
                        NavigationPillItem(it, updateTab, pillItemWidth, pillItemHeight)
                    }
                }

                val alpha = remember { Animatable(-1f) }
                val cornerAnimationSpec: AnimationSpec<Float> = tween(durationMillis = labelFade * 2)

                LaunchedEffect(currentTabIndex) {
                    scope.launchUI {
                        if (alpha.value == -1f) return@launchUI

                        if (oldIndex < currentTabIndex) {
                            alpha.animateTo(0.5f, animationSpec = tween(durationMillis = labelFade))
                        } else {
                            alpha.animateTo(-0.5f, animationSpec = tween(durationMillis = labelFade))
                        }
                    }

                    when (currentTabIndex) {
                        0 -> {
                            scope.launch { topStartCornerSize.animateTo(0f, animationSpec = cornerAnimationSpec) }
                            scope.launch { topEndCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomStartCornerSize.animateTo(0f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomEndCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                        }
                        tabs.size - 1 -> {
                            scope.launch { topStartCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { topEndCornerSize.animateTo(0f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomStartCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomEndCornerSize.animateTo(0f, animationSpec = cornerAnimationSpec) }
                        }
                        else -> {
                            scope.launch { topStartCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { topEndCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomStartCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                            scope.launch { bottomEndCornerSize.animateTo(28f, animationSpec = cornerAnimationSpec) }
                        }
                    }
                }

                LaunchedEffect(alpha.value) {
                    scope.launchUI {
                        when (alpha.value) {
                            -1f -> alpha.snapTo(0f)

                            -0.5f -> {
                                if (oldIndex > currentTabIndex) {
                                    alpha.snapTo(0.5f)
                                    oldIndex = currentTabIndex
                                } else {
                                    alpha.animateTo(0f, animationSpec = tween(durationMillis = labelFade))
                                }
                            }

                            0.5f -> {
                                if (oldIndex < currentTabIndex) {
                                    alpha.snapTo(-0.5f)
                                    oldIndex = currentTabIndex
                                } else {
                                    alpha.animateTo(0f, animationSpec = tween(durationMillis = labelFade))
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(1 - abs(alpha.value * 2)),
                ) {
                    Text(
                        text = tabs[oldIndex].options.title,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = navigationOffsetX),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationPillItemBackground(
    pillItemWidth: Dp,
    pillItemHeight: Dp,
    pillOffsetX: Dp,
    topStartCornerSize: Dp,
    topEndCornerSize: Dp,
    bottomStartCornerSize: Dp,
    bottomEndCornerSize: Dp,
) {
    Surface(
        modifier = Modifier
            .offset(x = pillOffsetX)
            .requiredWidthIn(max = pillItemWidth),

        shape = MaterialTheme.shapes.extraLarge.copy(
            topStart = CornerSize(topStartCornerSize),
            topEnd = CornerSize(topEndCornerSize),
            bottomStart = CornerSize(bottomStartCornerSize),
            bottomEnd = CornerSize(bottomEndCornerSize),
        ),
    ) {
        Box(
            modifier = Modifier
                .size(width = pillItemWidth, height = pillItemHeight)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )
    }
}

@Composable
private fun NavigationPillItem(
    tab: Tab,
    updateTab: (Int) -> Unit,
    pillItemWidth: Dp,
    pillItemHeight: Dp,
) {
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

    // AM (TAB_HOLD) -->
    val onLongClick: () -> Unit = {
        if (selected) {
            scope.launch { tab.onReselectHold(navigator) }
        }
    }
    // <-- AM (TAB_HOLD)

    Box(
        modifier = Modifier
            .size(width = pillItemWidth, height = pillItemHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            // AM (TAB_HOLD) -->
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = true,
                role = Role.Tab,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics {
                this.selected = selected
            },
        // <-- AM (TAB_HOLD)
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
                // AM (RECENTS) -->
                RecentsTab::class.isInstance(tab) -> {
                    // <-- AM (RECENTS)
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<LibraryPreferences>()
                        pref.newAnimeUpdatesCount().changes()
                            .collectLatest { value = if (pref.newShowUpdatesCount().get()) it else 0 }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                MR.plurals.notification_chapters_generic,
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
                        pref.animeExtensionUpdatesCount().changes().collectLatest { value = it }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                MR.plurals.update_check_notification_ext_updates,
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
            tint = LocalContentColor.current,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun getOffsetX(numOfTabs: Int): Float = (0.5f * numOfTabs) - 0.5f

// <-- AM (NAVIGATION_PILL)
