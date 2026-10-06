package com.example.moneytracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth =
        FirebaseAuth.getInstance()

    suspend fun register(
        email: String,
        password: String
    ) {
        auth.createUserWithEmailAndPassword(
            email,
            password
        ).await()
    }

    suspend fun login(
        email: String,
        password: String
    ) {
        auth.signInWithEmailAndPassword(
            email,
            password
        ).await()
    }

    fun logout() {
        auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}