package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    val user: User,
    val token: String? = null // alternative token field
)

data class User(
    val id: Int,
    val username: String,
    val email: String? = null,
    val avatar: String? = null,
    @SerializedName("pledgeLevel")
    val pledgeLevel: Int? = null,
    val rootFolder: Int? = null
)

data class TokenRefreshRequest(
    val refresh: String
)

data class TokenRefreshResponse(
    @SerializedName("access_token")
    val accessToken: String? = null,
    val access: String? = null // fallback
) {
    fun getToken(): String = accessToken ?: access ?: ""
}
