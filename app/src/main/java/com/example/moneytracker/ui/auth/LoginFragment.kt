package com.example.moneytracker.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import android.util.Patterns
import com.example.moneytracker.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel:
            AuthViewModel by viewModels()

    private var _binding:
            FragmentLoginBinding? = null

    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentLoginBinding.inflate(
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

        binding.emailEditText.doAfterTextChanged { binding.emailInputLayout.error = null }
        binding.passwordEditText.doAfterTextChanged { binding.passwordInputLayout.error = null }
        binding.loginButton.setOnClickListener {

            val email =
                binding.emailEditText
                    .text
                    .toString()
                    .trim()

            val password =
                binding.passwordEditText
                    .text
                    .toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                binding.emailInputLayout.error = getString(R.string.invalid_email)

                return@setOnClickListener
            }

            if (password.isBlank()) {

                binding.passwordInputLayout.error = "Enter your password"

                return@setOnClickListener
            }

            viewModel.login(
                email,
                password
            )
        }

        binding.registerButton.setOnClickListener {

            findNavController().navigate(
                R.id.action_loginFragment_to_registerFragment
            )
        }

        observeUiState()
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    binding.loginButton.isEnabled = !state.isLoading
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

                    } else {

                        binding.errorTextView.visibility =
                            View.GONE
                    }

                    if (state.isSuccess) {

                        viewModel.resetState()

                        findNavController().navigate(
                            R.id.action_loginFragment_to_dashboardFragment
                        )
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