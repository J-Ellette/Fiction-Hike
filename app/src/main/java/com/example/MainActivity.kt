package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.FictionHikeDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.FictionHikeRepository
import com.example.data.remote.OfflineTileManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.ExploreMapScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.TrekCreatorScreen
import com.example.ui.screens.TreksCatalogScreen
import com.example.ui.theme.FictionHikeTheme
import com.example.viewmodel.FictionHikeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val MAP = "map"
    const val TREKS = "treks"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"
    const val CREATE_TREK = "create_trek"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsStore = remember { SettingsDataStore(context.applicationContext) }
            val userSettings by settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.local.UserSettings()
            )

            FictionHikeTheme(darkTheme = userSettings.darkModeEnabled) {
                AppNavigation(settingsStore = settingsStore)
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    settingsStore: SettingsDataStore,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Trail Explorer"
        key(uid) {
            AuthenticatedFictionHikeApp(
                currentUserId = uid,
                userDisplayName = displayName,
                settingsStore = settingsStore
            )
        }
    }
}

@Composable
private fun AuthenticatedFictionHikeApp(
    currentUserId: String,
    userDisplayName: String,
    settingsStore: SettingsDataStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: FictionHikeViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = FictionHikeRepository(db)
                val roomDb = FictionHikeDatabase.getInstance(app)
                val offlineManager = OfflineTileManager(roomDb.offlineMapDao())
                FictionHikeViewModel(
                    repository = repository,
                    offlineTileManager = offlineManager,
                    settingsDataStore = settingsStore,
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isCreatorScreen = currentDestination?.route == Routes.CREATE_TREK

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val tabs = remember {
        listOf(
            NavTab(Routes.MAP, "Overlay", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_map"),
            NavTab(Routes.TREKS, "Treks", Icons.Filled.AutoStories, Icons.Outlined.AutoStories, "nav_treks"),
            NavTab(Routes.COMMUNITY, "Community", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_community"),
            NavTab(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp && !isCreatorScreen

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (!useRail && !isCreatorScreen) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.MAP
                    ) {
                        composable(Routes.MAP) {
                            ExploreMapScreen(
                                viewModel = viewModel,
                                darkTheme = userSettings.darkModeEnabled,
                                onOpenTreksPicker = { navigateToTab(Routes.TREKS) }
                            )
                        }
                        composable(Routes.TREKS) {
                            TreksCatalogScreen(
                                viewModel = viewModel,
                                onSelectTrekToMap = { navigateToTab(Routes.MAP) },
                                onOpenCreateTrek = { navController.navigate(Routes.CREATE_TREK) }
                            )
                        }
                        composable(Routes.CREATE_TREK) {
                            TrekCreatorScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onTrekPublished = {
                                    navController.popBackStack()
                                    navigateToTab(Routes.MAP)
                                }
                            )
                        }
                        composable(Routes.COMMUNITY) {
                            CommunityScreen(
                                viewModel = viewModel,
                                currentUserId = currentUserId
                            )
                        }
                        composable(Routes.PROFILE) {
                            ProfileSettingsScreen(
                                viewModel = viewModel,
                                userDisplayName = userDisplayName,
                                onSignOutClicked = {
                                    signOutSafely(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun signOutSafely(
    credentialManager: CredentialManager,
    scope: CoroutineScope
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
            }
        }
    }
}
