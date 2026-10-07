package com.example.moneytracker

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.moneytracker.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        val splashScreen = installSplashScreen()
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            com.example.moneytracker.ui.settings.SettingsFragment.themeMode(
                com.example.moneytracker.data.repository.AppPreferences(this).theme()))

        super.onCreate(
            savedInstanceState
        )
        splashScreen.setOnExitAnimationListener { splash ->
            splash.view.animate().alpha(0f).setDuration(180L)
                .withEndAction { splash.remove() }.start()
        }

        setContentView(
            R.layout.activity_main
        )

        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId) {
                navController.navigate(item.itemId, null, navOptions {
                    launchSingleTop = true
                    restoreState = true
                    anim {
                        enter = R.anim.tab_enter
                        exit = R.anim.tab_exit
                        popEnter = R.anim.tab_enter
                        popExit = R.anim.tab_exit
                    }
                    popUpTo(R.id.dashboardFragment) { saveState = true }
                })
            }
            true
        }
        bottomNavigation.setOnItemReselectedListener { }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val mainScreen = destination.id in setOf(R.id.dashboardFragment, R.id.historyFragment, R.id.insightsFragment, R.id.settingsFragment)
            bottomNavigation.visibility = if (mainScreen) View.VISIBLE else View.GONE
            if (mainScreen) bottomNavigation.menu.findItem(destination.id).isChecked = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (savedInstanceState == null) {
            checkAuthentication()
        }
    }

    fun selectMainTab(destinationId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = destinationId
    }

    private fun checkAuthentication() {

        val user =
            FirebaseAuth
                .getInstance()
                .currentUser

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.nav_host_fragment
                ) as NavHostFragment

        val navController =
            navHostFragment.navController

        if (user != null) {

            navController.navigate(
                R.id.action_loginFragment_to_dashboardFragment
            )
        }
    }
}
