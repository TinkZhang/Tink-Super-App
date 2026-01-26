package app.tinks.tink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import app.tinks.tink.haircut.HaircutScreen
import app.tinks.tink.merriam.MerriamScreen
import app.tinks.tink.navigation.MyNavKey
import app.tinks.tink.navigation.ScreenA
import app.tinks.tink.navigation.ScreenB
import app.tinks.tink.navigation.ScreenHair
import app.tinks.tink.navigation.ScreenLearntZi
import app.tinks.tink.navigation.ScreenLeeter
import app.tinks.tink.navigation.ScreenMerriam
import app.tinks.tink.navigation.ScreenWeight
import app.tinks.tink.navigation.ScreenZi
import app.tinks.tink.navigation.allTopDestinations
import app.tinks.tink.weight.WeightScreen
import app.tinks.tink.zi.ZiScreen
import app.tinks.tink.zi.zilist.LearntZiListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp(
    modifier: Modifier = Modifier
) {
    val backStack = remember { mutableStateListOf<MyNavKey>(ScreenA) }
    val currentKey = backStack.lastOrNull()

    val onNavigate: (MyNavKey) -> Unit = { destination ->
        if (currentKey != destination) {
            if (backStack.isNotEmpty()) {
                backStack.removeLast()
            }
            backStack.add(destination)
        }
    }

    fun navigateBack() {
        backStack.removeLast()
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(Modifier.width(280.dp)) {
                Text(
                    "Nav 3 Menu",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider()
                allTopDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        icon = { Icon(dest.icon, dest.label) },
                        selected = currentKey == dest,
                        onClick = {
                            onNavigate(dest)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        drawerState = drawerState,
        gesturesEnabled = true
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(currentKey?.label ?: "响应式应用") },
                    // 左上角添加菜单按钮，点击打开抽屉
                    navigationIcon = {
                        if (currentKey in allTopDestinations) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "打开抽屉")
                            }
                        } else {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回上级"
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) { key ->
                NavEntry(key) {
                    when (key) {
                        is ScreenA -> ZiScreen(hiltViewModel(), onNavigationEvent = {
                            backStack.add(ScreenLearntZi)
                        })

                        is ScreenB -> WeightScreen(hiltViewModel())
                        is ScreenWeight -> WeightScreen(hiltViewModel())
                        is ScreenHair -> HaircutScreen(hiltViewModel())
                        is ScreenLeeter -> HaircutScreen(hiltViewModel())
                        is ScreenZi -> ZiScreen(hiltViewModel(), onNavigationEvent = {
                            backStack.add(ScreenLearntZi)
                        })
                        is ScreenLearntZi -> LearntZiListScreen(hiltViewModel())
                        is ScreenMerriam -> MerriamScreen(hiltViewModel())
                    }
                }
            }
        }
    }
}