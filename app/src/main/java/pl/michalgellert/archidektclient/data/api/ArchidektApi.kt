package pl.michalgellert.archidektclient.data.api

import pl.michalgellert.archidektclient.data.model.DecksResponse
import pl.michalgellert.archidektclient.data.model.FolderResponse
import pl.michalgellert.archidektclient.data.model.LoginRequest
import pl.michalgellert.archidektclient.data.model.LoginResponse
import pl.michalgellert.archidektclient.data.model.ModifyCardsRequest
import pl.michalgellert.archidektclient.data.model.ModifyCardsResponse
import pl.michalgellert.archidektclient.data.model.TokenRefreshRequest
import pl.michalgellert.archidektclient.data.model.TokenRefreshResponse
import retrofit2.Response
import pl.michalgellert.archidektclient.data.model.CardSearchResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchidektApi {

    companion object {
        const val BASE_URL = "https://archidekt.com/"
    }

    // Auth
    @POST("api/rest-auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/rest-auth/token/refresh/")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>

    // Decks
    @GET("api/decks/curated/self-recent/")
    suspend fun getMyDecks(
        @Header("Authorization") auth: String
    ): Response<DecksResponse>

    @GET("api/decks/{deckId}/")
    suspend fun getDeckDetails(
        @Header("Authorization") auth: String,
        @Path("deckId") deckId: Int
    ): Response<Map<String, Any>>

    // Card modifications
    @PATCH("api/decks/{deckId}/modifyCards/v2/")
    suspend fun modifyCards(
        @Header("Authorization") auth: String,
        @Path("deckId") deckId: Int,
        @Body request: ModifyCardsRequest
    ): Response<ModifyCardsResponse>

    // Color tags
    @GET("api/decks/colorTags/")
    suspend fun getColorTags(
        @Header("Authorization") auth: String
    ): Response<List<Map<String, Any>>>

    // Folders - różne warianty endpointów
    @GET("api/folders/{folderId}/")
    suspend fun getFolder(
        @Header("Authorization") auth: String,
        @Path("folderId") folderId: Int
    ): Response<FolderResponse>

    @GET("api/decks/folders/{folderId}/")
    suspend fun getDeckFolder(
        @Header("Authorization") auth: String,
        @Path("folderId") folderId: Int
    ): Response<FolderResponse>

    @GET("api/users/folders/{folderId}/")
    suspend fun getUserFolder(
        @Header("Authorization") auth: String,
        @Path("folderId") folderId: Int
    ): Response<FolderResponse>

    // Card search
    @GET("api/cards/v2/")
    suspend fun searchCards(
        @Header("Authorization") auth: String,
        @Query("nameSearch") query: String,
        @Query("includeTokens") includeTokens: Boolean = true,
        @Query("includeDigital") includeDigital: Boolean = true,
        @Query("includeEmblems") includeEmblems: Boolean = true,
        @Query("unique") unique: Boolean = true
    ): Response<CardSearchResponse>
}
