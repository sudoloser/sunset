package dev.sudoloser.sunset.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            NavigationRail(
                containerColor = Color(0xFF121212),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Spacer(Modifier.height(48.dp))
                tabs.forEach { (label, icon) ->
                    NavigationRailItem(
                        selected = activeTab == label,
                        onClick = { onTabSelected(label) },
                        icon = { icon() },
                        label = {
                            if (screenClass == ScreenWidthClass.Expanded) {
                                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = Color.White,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Black
            ) { padding ->
                content(padding)
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    tabs.forEach { (label, icon) ->
                        val isSelected = activeTab == label
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { onTabSelected(label) },
                            icon = { icon() },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = Color.White,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            containerColor = Color.Black
        ) { padding ->
            content(padding)
        }
    }
}
