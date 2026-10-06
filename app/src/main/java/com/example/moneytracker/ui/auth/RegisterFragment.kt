package com.example.moneytracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val viewModel:
            AuthViewModel by viewModels()

    private var _binding:
            FragmentRegisterBinding? = null

    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentRegisterBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        setupRegisterButton()
        setupLoginButton()
        observeUiState()
    }

    private fun setupRegisterButton() {

        binding.registerButton.setOnClickListener {

            val email =
                binding.emailEditText
                    .text
                    .toString()
                    .trim()

            val password =
                binding.passwordEditText
                    .text
                    .toString()

            val confirmPassword =
                binding.confirmPasswordEditText
                    .text
                    .toString()

            if (email.isBlank()) {

                showError(
                    "Enter your email"
                )

                return@setOnClickListener
            }

            if (password.isBlank()) {

                showError(
                    "Enter your password"
                )

                return@setOnClickListener
            }

            if (password != confirmPassword) {

                showError(
                    "Passwords do not match"
                )

                return@setOnClickListener
            }

            viewModel.register(
                email,
                password
            )
        }
    }

    private fun setupLoginButton() {

        binding.loginButton.setOnClickListener {

            // Register was opened from Login,
            // so simply go back to Login.
            findNavController().popBackStack()
        }
    }

    private fun showError(
        message: String
    ) {

        binding.errorTextView.text =
            message

        binding.errorTextView.visibility =
            View.VISIBLE
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    binding.progressBar.visibility =
                        if (state.isLoading) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    if (state.errorMessage != null) {

                        binding.errorTextView.text =
                            state.errorMessage

                        binding.errorTextView.visibility =
                            View.VISIBLE

                    } else if (!state.isLoading) {

                        binding.errorTextView.visibility =
                            View.GONE
                    }

                    if (state.isSuccess) {

                        viewModel.resetState()

                        // Registration succeeded.
                        // Go back to Login.
                        findNavController()
                            .popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
