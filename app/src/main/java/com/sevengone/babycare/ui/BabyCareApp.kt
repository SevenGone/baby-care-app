package com.sevengone.babycare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class RootTab(val label: String) {
    Home("首页"),
    Record("记录"),
    Trend("统计"),
    Settings("设置")
}

@Composable
fun BabyCareApp(viewModel: BabyCareViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rootView = LocalView.current
    var currentTab by rememberSaveable { mutableStateOf(RootTab.Home) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BabyCareBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { paddingValues ->
        DecorativeBackground {
            when (currentTab) {
                RootTab.Home -> HomeScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues,
                    onExportClick = {
                        scope.launch {
                            val result = ExportUtils.exportCurrentView(context, rootView)
                            snackbarHostState.showSnackbar(
                                result.fold(
                                    onSuccess = { "已导出到相册：$it" },
                                    onFailure = { "导出失败：${it.message ?: "未知错误"}" }
                                )
                            )
                        }
                    }
                )

                RootTab.Record -> RecordScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues,
                    onSaved = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )

                RootTab.Trend -> TrendScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues
                )

                RootTab.Settings -> SettingsScreen(
                    viewModel = viewModel,
                    contentPadding = paddingValues
                )
            }
        }
    }
}

@Composable
private fun DecorativeBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1200f
                )
            )
    ) {
        content()
    }
}

@Composable
private fun BabyCareBottomBar(
    currentTab: RootTab,
    onTabSelected: (RootTab) -> Unit
) {
    val items = listOf(
        RootTab.Home to Icons.Rounded.Home,
        RootTab.Record to Icons.Rounded.MedicalServices,
        RootTab.Trend to Icons.Rounded.AutoGraph,
        RootTab.Settings to Icons.Rounded.Settings
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.38f))
            .padding(5.dp)
    ) {
        NavigationBar(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            tonalElevation = 0.dp
        ) {
            items.forEach { (tab, icon) ->
                NavigationBarItem(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = { Text(tab.label) }
                )
            }
        }
    }
}
