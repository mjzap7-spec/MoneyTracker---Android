package com.example.moneytracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository =
        AuthRepository()

    private val _uiState =
        MutableStateFlow(
            AuthUiState()
        )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                AuthUiState(
                    isLoading = true
                )

            try {

                repository.login(
                    email,
                    password
                )

                _uiState.value =
                    AuthUiState(
                        isSuccess = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            e.message
                                ?: "Login failed"
                    )
            }
        }
    }

    fun register(
        email: String,
        password: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                AuthUiState(
                    isLoading = true
                )

            try {

                repository.register(
                    email,
                    password
                )

                _uiState.value =
                    AuthUiState(
                        isSuccess = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            e.message
                                ?: "Registration failed"
                    )
            }
        }
    }

    fun logout() {
        repository.logout()
    }

    fun resetState() {

        _uiState.value =
            AuthUiState()
    }
}