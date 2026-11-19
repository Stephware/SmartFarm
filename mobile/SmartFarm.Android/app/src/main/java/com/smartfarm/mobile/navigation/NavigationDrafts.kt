package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.smartfarm.mobile.ui.components.ExitGuard

/** Includes stopped entries: a global page must not hide a draft after recreation. */
internal class NavigationDrafts {
    val guards = mutableMapOf<String, ExitGuard>()
    val routes = mutableMapOf<String, String>()
    fun remove(id: String) { guards.remove(id); routes.remove(id) }
    fun clearLeavingFarms() {
        guards.keys.toList().filter { routes[it] != "farms" }.forEach(::remove)
    }
    fun clear() { guards.clear(); routes.clear() }
}

@Composable
internal fun rememberNavigationDrafts(): NavigationDrafts = rememberSaveable(saver = mapSaver(
    save = { drafts -> drafts.guards.mapValues { (id, guard) ->
        listOf(drafts.routes[id].orEmpty(), guard.snapshot())
    } },
    restore = { saved -> NavigationDrafts().apply {
        saved.forEach { (id, value) ->
            val parts = value as List<*>
            routes[id] = parts[0] as String
            @Suppress("UNCHECKED_CAST")
            guards[id] = ExitGuard(parts[1] as Map<String, Any?>)
        }
    } }
)) { NavigationDrafts() }
