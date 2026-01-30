package pl.michalgellert.archidektclient.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.michalgellert.archidektclient.data.auth.AuthManager

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Try auto-login if credentials are stored
        if (authManager.hasStoredCredentials()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = authManager.autoLogin()
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authManager.login(
                email = _uiState.value.email,
                password = _uiState.value.password,
                saveCredentials = true
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)
