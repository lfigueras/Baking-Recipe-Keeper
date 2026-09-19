package com.lovely.bakingrecipes.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lovely.bakingrecipes.data.PastryDatabase
import com.lovely.bakingrecipes.repository.PastryRepository
import com.lovely.bakingrecipes.repository.ShoppingListRepository
import com.lovely.bakingrecipes.ui.screens.add.AddPastryScreen
import com.lovely.bakingrecipes.ui.screens.baking.StartBakingScreen
import com.lovely.bakingrecipes.ui.screens.detail.PastryDetailScreen
import com.lovely.bakingrecipes.ui.screens.home.HomeScreen
import com.lovely.bakingrecipes.ui.screens.ingredients.IngredientsListScreen
import com.lovely.bakingrecipes.ui.screens.media.MediaAlbumScreen
import com.lovely.bakingrecipes.ui.screens.more.MoreScreen
import com.lovely.bakingrecipes.ui.screens.overview.OverviewScreen
import com.lovely.bakingrecipes.ui.screens.pastrylist.PastryListScreen
import com.lovely.bakingrecipes.ui.screens.settings.SettingsScreen
import com.lovely.bakingrecipes.ui.screens.shopping.ShoppingListScreen
import com.lovely.bakingrecipes.ui.theme.ThemeMode
import com.lovely.bakingrecipes.util.Analytics
import com.lovely.bakingrecipes.viewmodel.AddPastryViewModel
import com.lovely.bakingrecipes.viewmodel.AddPastryViewModelFactory
import com.lovely.bakingrecipes.viewmodel.BackupViewModel
import com.lovely.bakingrecipes.viewmodel.DashboardTarget
import com.lovely.bakingrecipes.viewmodel.GenericViewModelFactory
import com.lovely.bakingrecipes.viewmodel.HomeViewModel
import com.lovely.bakingrecipes.viewmodel.HomeViewModelFactory
import com.lovely.bakingrecipes.viewmodel.IngredientsListViewModel
import com.lovely.bakingrecipes.viewmodel.OverviewViewModel
import com.lovely.bakingrecipes.viewmodel.PastryDetailViewModel
import com.lovely.bakingrecipes.viewmodel.PastryDetailViewModelFactory
import com.lovely.bakingrecipes.viewmodel.PastryListViewModel
import com.lovely.bakingrecipes.viewmodel.ShoppingListViewModel
import kotlinx.coroutines.launch

// Maps a tapped dashboard tile to its destination route.
private fun targetRoute(target: DashboardTarget): String = when (target) {
    DashboardTarget.Ingredients -> "ingredients"
    DashboardTarget.AllPastries -> "pastry_list/__all__"
    is DashboardTarget.Category -> "pastry_list/${Uri.encode(target.name)}"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab("home", "Home", Icons.Filled.Home),
    BottomTab("favorites", "Favorites", Icons.Filled.Favorite),
    BottomTab("categories", "Categories", Icons.Filled.GridView),
    BottomTab("more", "More", Icons.Filled.MoreHoriz)
)

@Composable
fun AppNavigation(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {

    val navController = rememberNavController()

    val context = LocalContext.current
    val application = context.applicationContext as Application

    val database = PastryDatabase.getDatabase(context)

    val repository = PastryRepository(
        pastryDao = database.pastryDao()
    )
    val shoppingRepository = ShoppingListRepository(
        shoppingListDao = database.shoppingListDao()
    )
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Report each visited destination as a screen_view.
    LaunchedEffect(currentRoute) {
        currentRoute?.let { Analytics.screenView(it) }
    }
    // Hide the bar only on immersive/form flows; show it everywhere else for quick tab access.
    val hiddenBottomBarRoutes = setOf(
        "add_pastry",
        "edit_pastry/{pastryId}",
        "start_baking/{pastryId}"
    )
    val showBottomBar = currentRoute != null && currentRoute !in hiddenBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // Pop to the tab root without restoring shared sub-pages (e.g. Settings).
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->

        // Only consume the bottom inset (nav bar); each screen's TopAppBar handles the top inset.
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(bottom = scaffoldPadding.calculateBottomPadding())
        ) {

        composable("home") { backStackEntry ->
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(repository)
            )
            val uiState by homeViewModel.uiState.collectAsState()
            val addedMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("added_pastry_message", null)
                .collectAsState()
            val deletedMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("deleted_recipe_name", null)
                .collectAsState()
            HomeScreen(
                uiState = uiState,
                onSearchChange = homeViewModel::onSearchChange,
                onCategorySelected = homeViewModel::onCategorySelected,
                onAddPastryClick = {
                    navController.navigate("add_pastry")
                },
                onPastryClick = { pastryId ->
                    navController.navigate("detail/$pastryId")
                },
                onTileClick = { target ->
                    navController.navigate(targetRoute(target))
                },
                onSeeAllClick = {
                    navController.navigate("overview")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                addedMessage = addedMessage,
                onAddedMessageShown = {
                    backStackEntry.savedStateHandle["added_pastry_message"] = null
                },
                deletedMessage = deletedMessage,
                onDeletedMessageShown = {
                    backStackEntry.savedStateHandle["deleted_recipe_name"] = null
                }
            )
        }

        composable("favorites") {
            val listViewModel: PastryListViewModel = viewModel(
                factory = GenericViewModelFactory {
                    PastryListViewModel(repository, category = null, favoritesOnly = true)
                }
            )
            val pastries by listViewModel.pastries.collectAsState()
            val listLoading by listViewModel.isLoading.collectAsState()
            PastryListScreen(
                title = listViewModel.title,
                pastries = pastries,
                isLoading = listLoading,
                onBackClick = null,
                onPastryClick = { id -> navController.navigate("detail/$id") }
            )
        }

        composable("categories") {
            val overviewViewModel: OverviewViewModel = viewModel(
                factory = GenericViewModelFactory { OverviewViewModel(repository) }
            )
            val tiles by overviewViewModel.tiles.collectAsState()
            OverviewScreen(
                tiles = tiles,
                onBackClick = null,
                title = "Categories",
                onTileClick = { target -> navController.navigate(targetRoute(target)) }
            )
        }

        composable("more") {
            MoreScreen(
                onShoppingListClick = { navController.navigate("shopping_list") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("shopping_list") {
            val shoppingViewModel: ShoppingListViewModel = viewModel(
                factory = GenericViewModelFactory { ShoppingListViewModel(shoppingRepository, repository) }
            )
            val items by shoppingViewModel.items.collectAsState()
            ShoppingListScreen(
                items = items,
                onBackClick = { navController.popBackStack() },
                onToggleChecked = shoppingViewModel::setChecked,
                onDelete = shoppingViewModel::delete,
                onClearChecked = shoppingViewModel::clearChecked,
                onClearAll = shoppingViewModel::clearAll,
                onAddManual = shoppingViewModel::addManual,
                onAddAllRecipes = shoppingViewModel::addAllRecipeIngredients
            )
        }

        composable("add_pastry") {
            val viewModel: AddPastryViewModel = viewModel(
                factory = AddPastryViewModelFactory(application, repository, null)
            )
            AddPastryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveComplete = { _, name, _ ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("added_pastry_message", "$name added")
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable(
            route = "edit_pastry/{pastryId}",
            arguments = listOf(navArgument("pastryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val pastryId = backStackEntry.arguments?.getInt("pastryId") ?: return@composable
            val viewModel: AddPastryViewModel = viewModel(
                factory = AddPastryViewModelFactory(application, repository, pastryId)
            )
            AddPastryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveComplete = { _, name, _ ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("edited_pastry_name", name)
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable(
            route = "detail/{pastryId}",
            arguments = listOf(navArgument("pastryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val pastryId = backStackEntry.arguments?.getInt("pastryId") ?: return@composable
            val detailViewModel: PastryDetailViewModel = viewModel(
                factory = PastryDetailViewModelFactory(application, repository, pastryId)
            )
            val pastry by detailViewModel.pastry.collectAsState()
            val editedName by backStackEntry.savedStateHandle
                .getStateFlow<String?>("edited_pastry_name", null)
                .collectAsState()
            val duplicatedMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("duplicated_message", null)
                .collectAsState()
            val addedToListMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("added_to_list_message", null)
                .collectAsState()
            PastryDetailScreen(
                pastry = pastry,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditClick = { id ->
                    navController.navigate("edit_pastry/$id")
                },
                onDeleteConfirmed = { toDelete ->
                    detailViewModel.deletePastry(toDelete)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("deleted_recipe_name", toDelete.name)
                    navController.popBackStack()
                },
                onToggleFavorite = { detailViewModel.toggleFavorite() },
                onDuplicate = {
                    detailViewModel.duplicate { newId ->
                        navController.navigate("detail/$newId") {
                            popUpTo("detail/$pastryId") { inclusive = true }
                        }
                    }
                },
                onAddToShoppingList = { scale ->
                    val current = pastry
                    if (current != null) {
                        scope.launch { shoppingRepository.addFromRecipe(current, scale) }
                        Analytics.shoppingListAdd("recipe")
                        backStackEntry.savedStateHandle["added_to_list_message"] =
                            "Added to shopping list"
                    }
                },
                onStartBaking = { id -> navController.navigate("start_baking/$id") },
                onSeeAllMedia = { id -> navController.navigate("media/$id") },
                editedMessage = editedName,
                onEditedMessageShown = {
                    backStackEntry.savedStateHandle["edited_pastry_name"] = null
                },
                duplicatedMessage = duplicatedMessage,
                onDuplicatedMessageShown = {
                    backStackEntry.savedStateHandle["duplicated_message"] = null
                },
                addedToListMessage = addedToListMessage,
                onAddedToListMessageShown = {
                    backStackEntry.savedStateHandle["added_to_list_message"] = null
                }
            )
        }

        composable(
            route = "start_baking/{pastryId}",
            arguments = listOf(navArgument("pastryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val pastryId = backStackEntry.arguments?.getInt("pastryId") ?: return@composable
            val detailViewModel: PastryDetailViewModel = viewModel(
                factory = PastryDetailViewModelFactory(application, repository, pastryId)
            )
            val pastry by detailViewModel.pastry.collectAsState()
            StartBakingScreen(
                pastry = pastry,
                onExit = { navController.popBackStack() }
            )
        }

        composable(
            route = "media/{pastryId}",
            arguments = listOf(navArgument("pastryId") { type = NavType.IntType })
        ) { backStackEntry ->
            val pastryId = backStackEntry.arguments?.getInt("pastryId") ?: return@composable
            val detailViewModel: PastryDetailViewModel = viewModel(
                factory = PastryDetailViewModelFactory(application, repository, pastryId)
            )
            val pastry by detailViewModel.pastry.collectAsState()
            MediaAlbumScreen(
                media = pastry?.media.orEmpty(),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("overview") {
            val overviewViewModel: OverviewViewModel = viewModel(
                factory = GenericViewModelFactory { OverviewViewModel(repository) }
            )
            val tiles by overviewViewModel.tiles.collectAsState()
            OverviewScreen(
                tiles = tiles,
                onBackClick = {
                    navController.popBackStack()
                },
                onTileClick = { target ->
                    navController.navigate(targetRoute(target))
                }
            )
        }

        composable("settings") {
            val backupViewModel: BackupViewModel = viewModel(
                factory = GenericViewModelFactory { BackupViewModel(application, repository) }
            )
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onBackClick = {
                    navController.popBackStack()
                },
                onExport = { uri, onResult -> backupViewModel.exportTo(uri, onResult) },
                onImport = { uri, onResult -> backupViewModel.importFrom(uri, onResult) }
            )
        }

        composable("ingredients") { backStackEntry ->
            val ingredientsViewModel: IngredientsListViewModel = viewModel(
                factory = GenericViewModelFactory { IngredientsListViewModel(repository) }
            )
            val items by ingredientsViewModel.groups.collectAsState()
            val ingredientSearch by ingredientsViewModel.searchQuery.collectAsState()
            val ingredientsLoading by ingredientsViewModel.isLoading.collectAsState()
            val ingredientsDeletedMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("deleted_recipe_name", null)
                .collectAsState()
            IngredientsListScreen(
                groups = items,
                isLoading = ingredientsLoading,
                searchQuery = ingredientSearch,
                onSearchChange = ingredientsViewModel::onSearchChange,
                onBackClick = {
                    navController.popBackStack()
                },
                onPastryClick = { id ->
                    navController.navigate("detail/$id")
                },
                deletedMessage = ingredientsDeletedMessage,
                onDeletedMessageShown = {
                    backStackEntry.savedStateHandle["deleted_recipe_name"] = null
                }
            )
        }

        composable(
            route = "pastry_list/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString("category")
            val category = if (raw == null || raw == "__all__") null else raw
            val listViewModel: PastryListViewModel = viewModel(
                factory = GenericViewModelFactory { PastryListViewModel(repository, category) }
            )
            val pastries by listViewModel.pastries.collectAsState()
            val listLoading by listViewModel.isLoading.collectAsState()
            val listDeletedMessage by backStackEntry.savedStateHandle
                .getStateFlow<String?>("deleted_recipe_name", null)
                .collectAsState()
            PastryListScreen(
                title = listViewModel.title,
                pastries = pastries,
                isLoading = listLoading,
                onBackClick = {
                    navController.popBackStack()
                },
                onPastryClick = { id ->
                    navController.navigate("detail/$id")
                },
                deletedMessage = listDeletedMessage,
                onDeletedMessageShown = {
                    backStackEntry.savedStateHandle["deleted_recipe_name"] = null
                }
            )
        }
    }
    }
}