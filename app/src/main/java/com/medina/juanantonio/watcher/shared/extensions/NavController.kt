package com.medina.juanantonio.watcher.shared.extensions

import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * This is used to prevent navigating to directions from other screens.
 */
fun NavController.safeNavigate(directions: NavDirections) {
    currentDestination?.let { destination ->
        destination.getAction(directions.actionId)?.let { action ->
            val destinationId = action.destinationId
            findDestination(destinationId)?.let {
                navigate(directions)
            }
        }
    }
}
