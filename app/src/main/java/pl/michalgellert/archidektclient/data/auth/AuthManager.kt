package pl.michalgellert.archidektclient.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.michalgellert.archidektclient.data.api.ApiClient
import pl.michalgellert.archidektclient.data.model.LoginRequest
import pl.michalgellert.archidektclient.data.model.TokenRefreshRequest
import pl.michalgellert.archidektclient.data.model.User

class AuthManager private constructor(context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "archidekt_auth"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROOT_FOLDER_ID = "root_folder_id"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var accessToken: String? = null
    private var refreshToken: String? = null

    init {
        loadStoredCredentials()
    }

    private fun loadStoredCredentials() {
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userId = prefs.getInt(KEY_USER_ID, -1)
        val username = prefs.getString(KEY_USERNAME, null)

        if (accessToken != null && userId != -1 && username != null) {
            _currentUser.value = User(id = userId, username = username)
            _authState.value = AuthState.Authenticated
        } else if (hasStoredCredentials()) {
            _authState.value = AuthState.NeedsLogin
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun hasStoredCredentials(): Boolean {
        val email = prefs.getString(KEY_EMAIL, null)
        val password = prefs.getString(KEY_PASSWORD, null)
        return !email.isNullOrBlank() && !password.isNullOrBlank()
    }

    fun getAuthHeader(): String? = accessToken?.let { "JWT $it" }

    fun getAccessToken(): String? = accessToken

    fun getRootFolderId(): Int? {
        val id = prefs.getInt(KEY_ROOT_FOLDER_ID, -1)
        return if (id != -1) id else null
    }

    suspend fun login(email: String, password: String, saveCredentials: Boolean = true): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            val response = ApiClient.archidektApi.login(LoginRequest(email, password))

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                accessToken = loginResponse.accessToken
                refreshToken = loginResponse.refreshToken
                _currentUser.value = loginResponse.user

                prefs.edit().apply {
                    if (saveCredentials) {
                        putString(KEY_EMAIL, email)
                        putString(KEY_PASSWORD, password)
                    }
                    putString(KEY_ACCESS_TOKEN, loginResponse.accessToken)
                    putString(KEY_REFRESH_TOKEN, loginResponse.refreshToken)
                    putInt(KEY_USER_ID, loginResponse.user.id)
                    putString(KEY_USERNAME, loginResponse.user.username)
                    loginResponse.user.rootFolder?.let { putInt(KEY_ROOT_FOLDER_ID, it) }
                    apply()
                }

                _authState.value = AuthState.Authenticated
                Log.d(TAG, "Login successful for user: ${loginResponse.user.username}")
                Result.success(loginResponse.user)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Login failed: ${response.code()} - $errorBody")
                _authState.value = AuthState.NotAuthenticated
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            _authState.value = AuthState.NotAuthenticated
            Result.failure(e)
        }
    }

    suspend fun autoLogin(): Result<User> {
        val email = prefs.getString(KEY_EMAIL, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        return if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
            login(email, password, saveCredentials = false)
        } else {
            Result.failure(Exception("No stored credentials"))
        }
    }

    suspend fun refreshAccessToken(): Result<String> {
        val currentRefreshToken = refreshToken ?: return Result.failure(Exception("No refresh token"))

        return try {
            val response = ApiClient.archidektApi.refreshToken(
                TokenRefreshRequest(currentRefreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val newAccessToken = response.body()!!.getToken()
                accessToken = newAccessToken

                prefs.edit().putString(KEY_ACCESS_TOKEN, newAccessToken).apply()

                Log.d(TAG, "Token refreshed successfully")
                Result.success(newAccessToken)
            } else {
                Log.e(TAG, "Token refresh failed: ${response.code()}")
                // Token refresh failed, try auto-login
                val autoLoginResult = autoLogin()
                if (autoLoginResult.isSuccess) {
                    Result.success(accessToken!!)
                } else {
                    _authState.value = AuthState.NeedsLogin
                    Result.failure(Exception("Token refresh failed"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            // Try auto-login on error
            val autoLoginResult = autoLogin()
            if (autoLoginResult.isSuccess) {
                Result.success(accessToken!!)
            } else {
                _authState.value = AuthState.NeedsLogin
                Result.failure(e)
            }
        }
    }

    suspend fun <T> withAuth(block: suspend (authHeader: String) -> Result<T>): Result<T> {
        val auth = getAuthHeader() ?: return Result.failure(Exception("Not authenticated"))

        val result = block(auth)

        // If request failed with 401, try to refresh token and retry
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            if (exception?.message?.contains("401") == true) {
                val refreshResult = refreshAccessToken()
                if (refreshResult.isSuccess) {
                    val newAuth = getAuthHeader()!!
                    return block(newAuth)
                }
            }
        }

        return result
    }

    fun logout() {
        accessToken = null
        refreshToken = null
        _currentUser.value = null

        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            // Zachowaj email/password dla auto-login
            apply()
        }

        _authState.value = AuthState.NotAuthenticated
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        accessToken = null
        refreshToken = null
        _currentUser.value = null
        _authState.value = AuthState.NotAuthenticated
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data object NeedsLogin : AuthState()
    data object Authenticated : AuthState()
}
