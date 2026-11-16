package com.neverexpire.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNav(vm: InventoryViewModel = viewModel()) {
    val nav = rememberNavController()

    fun goTab(t: Tab) = nav.navigate(t.route) {
        popUpTo(Tab.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    NavHost(nav, startDestination = Tab.HOME.route) {
        composable(Tab.HOME.route) {
            HomeScreen(vm, onOpen = { nav.navigate("detail/$it") }, onAdd = { nav.navigate("edit") }, onTab = ::goTab)
        }
        composable(Tab.SCAN.route) {
            ScanScreen(vm, onDone = { nav.navigate("edit?scan=true") }, onTab = ::goTab)
        }
        composable(Tab.SETTINGS.route) { SettingsScreen(vm, onTab = ::goTab) }
        composable(
            "edit?id={id}&scan={scan}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("scan") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
            val scan = entry.arguments?.getBoolean("scan") ?: false
            EditScreen(
                vm, productId = id, fromScan = scan,
                onBack = { nav.popBackStack() },
                onSaved = { saved -> openDetailAfterSave(nav, saved, id != null) }
            )
        }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            DetailScreen(vm, id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("edit?id=$id") })
        }
    }
}

private fun openDetailAfterSave(nav: NavHostController, id: Long, editing: Boolean) {
    if (editing) nav.popBackStack()
    else nav.navigate("detail/$id") { popUpTo(Tab.HOME.route) }
}
