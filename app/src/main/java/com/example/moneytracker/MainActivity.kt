package com.example.moneytracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.moneytracker.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        if (savedInstanceState == null) {
            checkAuthentication()
        }
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
                R.id.dashboardFragment
            )
        }
    }
}