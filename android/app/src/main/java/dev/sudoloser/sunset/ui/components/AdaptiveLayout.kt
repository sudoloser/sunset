package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class ScreenWidthClass { Compact, Medium, Expanded }

@Composable
fun rememberScreenWidthClass(): ScreenWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 840 -> ScreenWidthClass.Expanded
        widthDp >= 600 -> ScreenWidthClass.Medium
        else -> ScreenWidthClass.Compact
    }
}

@Composable
fun NavigationSuite(
    tabs: List<Pair<String, @Composable () -> Unit>>,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val screenClass = rememberScreenWidthClass()
    val useRail = screenClass != ScreenWidthClass.Compact

    if (useRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                Spacer(Modifier.height(48.dp))
                tabs.forEach { (label, icon) ->
                    NavigationRailItem(
                        selected = activeTab == label,
                        onClick = { onTabSelected(label) },
                        icon = { icon() },
                        label = {
                            if (screenClass == ScreenWidthClass.Expanded) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }
            }
            Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                content(padding)
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = activeTab == label,
                            onClick = { onTabSelected(label) },
                            icon = { icon() },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            content(padding)
        }
    }
}
