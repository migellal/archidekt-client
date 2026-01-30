package pl.michalgellert.archidektclient.data.repository

import android.util.Log
import pl.michalgellert.archidektclient.data.api.ApiClient
import pl.michalgellert.archidektclient.data.auth.AuthManager
import pl.michalgellert.archidektclient.data.model.CardModification
import pl.michalgellert.archidektclient.data.model.DeckData
import pl.michalgellert.archidektclient.data.model.DeckSummary
import pl.michalgellert.archidektclient.data.model.FolderResponse
import pl.michalgellert.archidektclient.data.model.ModifyCardsRequest
import pl.michalgellert.archidektclient.data.model.ModifyCardsResponse
import pl.michalgellert.archidektclient.data.model.SearchResultCard

class ArchidektRepository private constructor(
    private val authManager: AuthManager
) {
    private val api = ApiClient.archidektApi
    private val deckDetailsService = ApiClient.deckDetailsService

    // Cache for deck data to avoid re-fetching (which generates new card IDs)
    private var cachedDeckData: DeckData? = null
    private var cachedDeckId: Int? = null

    companion object {
        @Volatile
        private var INSTANCE: ArchidektRepository? = null

        fun getInstance(authManager: AuthManager): ArchidektRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ArchidektRepository(authManager).also { INSTANCE = it }
            }
        }

        private const val TAG = "ArchidektRepository"
    }

    fun getRootFolderId(): Int? = authManager.getRootFolderId()

    suspend fun getMyDecks(): Result<List<DeckSummary>> {
        return authManager.withAuth { auth ->
            try {
                val response = api.getMyDecks(auth)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.results)
                } else {
                    Result.failure(Exception("Failed to get decks: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getFolder(folderId: Int): Result<FolderResponse> {
        return authManager.withAuth { auth ->
            var lastErrorCode: Int? = null

            // Try /api/folders/{id}/
            try {
                Log.d(TAG, "Trying /api/folders/$folderId/")
                val response1 = api.getFolder(auth, folderId)
                Log.d(TAG, "Response from /api/folders/: ${response1.code()}")
                if (response1.isSuccessful && response1.body() != null) {
                    return@withAuth Result.success(response1.body()!!)
                }
                lastErrorCode = response1.code()
            } catch (e: Exception) {
                Log.e(TAG, "Exception for /api/folders/: ${e.message}")
            }

            // Try /api/decks/folders/{id}/
            try {
                Log.d(TAG, "Trying /api/decks/folders/$folderId/")
                val response2 = api.getDeckFolder(auth, folderId)
                Log.d(TAG, "Response from /api/decks/folders/: ${response2.code()}")
                if (response2.isSuccessful && response2.body() != null) {
                    return@withAuth Result.success(response2.body()!!)
                }
                lastErrorCode = response2.code()
            } catch (e: Exception) {
                Log.e(TAG, "Exception for /api/decks/folders/: ${e.message}")
            }

            // Try /api/users/folders/{id}/
            try {
                Log.d(TAG, "Trying /api/users/folders/$folderId/")
                val response3 = api.getUserFolder(auth, folderId)
                Log.d(TAG, "Response from /api/users/folders/: ${response3.code()}")
                if (response3.isSuccessful && response3.body() != null) {
                    return@withAuth Result.success(response3.body()!!)
                }
                lastErrorCode = response3.code()
            } catch (e: Exception) {
                Log.e(TAG, "Exception for /api/users/folders/: ${e.message}")
            }

            // If we got 401, include it in the error message so withAuth can retry
            val errorMsg = if (lastErrorCode == 401) {
                "401 Unauthorized - No working folder endpoint found"
            } else {
                "No working folder endpoint found (last error: $lastErrorCode)"
            }
            Result.failure(Exception(errorMsg))
        }
    }

    suspend fun getRootFolder(): Result<FolderResponse> {
        val rootId = authManager.getRootFolderId()
            ?: return Result.failure(Exception("No root folder ID"))
        return getFolder(rootId)
    }

    suspend fun getDeckData(deckId: Int, forceRefresh: Boolean = false): Result<DeckData> {
        // Return cached data if available and not forcing refresh
        if (!forceRefresh && cachedDeckId == deckId && cachedDeckData != null) {
            Log.d(TAG, "Returning cached deck data for deckId=$deckId")
            return Result.success(cachedDeckData!!)
        }

        val token = authManager.getAccessToken()
            ?: return Result.failure(Exception("Not authenticated"))

        var result = deckDetailsService.getDeckDetails(deckId, token)

        // If failed, try refreshing token and retry
        if (result.isFailure) {
            val refreshResult = authManager.refreshAccessToken()
            if (refreshResult.isSuccess) {
                val newToken = authManager.getAccessToken()!!
                result = deckDetailsService.getDeckDetails(deckId, newToken)
            }
        }

        // Cache successful result
        if (result.isSuccess) {
            cachedDeckData = result.getOrNull()
            cachedDeckId = deckId
            Log.d(TAG, "Cached deck data for deckId=$deckId")
        }

        return result
    }

    fun clearDeckCache() {
        cachedDeckData = null
        cachedDeckId = null
    }

    fun isCacheValid(deckId: Int): Boolean {
        return cachedDeckId == deckId && cachedDeckData != null
    }

    suspend fun getDeckDetails(deckId: Int): Result<Map<String, Any>> {
        return authManager.withAuth { auth ->
            try {
                val response = api.getDeckDetails(auth, deckId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to get deck details: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun modifyCards(
        deckId: Int,
        modifications: List<CardModification>
    ): Result<ModifyCardsResponse> {
        return authManager.withAuth { auth ->
            try {
                val response = api.modifyCards(
                    auth = auth,
                    deckId = deckId,
                    request = ModifyCardsRequest(modifications)
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to modify cards: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun changeCardCategory(
        deckId: Int,
        cardId: String,
        deckRelationId: String,
        newCategories: List<String>,
        currentLabel: String? = null
    ): Result<ModifyCardsResponse> {
        val modification = CardModification.modify(
            cardId = cardId,
            deckRelationId = deckRelationId,
            categories = newCategories,
            label = currentLabel
        )
        return modifyCards(deckId, listOf(modification))
    }

    suspend fun changeCardTag(
        deckId: Int,
        cardId: String,
        deckRelationId: String,
        categories: List<String>,
        tagName: String,
        tagColor: String
    ): Result<ModifyCardsResponse> {
        val label = if (tagName.isBlank()) ",$tagColor" else "$tagName,$tagColor"
        val modification = CardModification.modify(
            cardId = cardId,
            deckRelationId = deckRelationId,
            categories = categories,
            label = label
        )
        return modifyCards(deckId, listOf(modification))
    }

    suspend fun searchCards(query: String): Result<List<SearchResultCard>> {
        if (query.isBlank()) return Result.success(emptyList())

        return authManager.withAuth { auth ->
            try {
                val response = api.searchCards(auth, query)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.results)
                } else {
                    Result.failure(Exception("Search failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addCardToDeck(
        deckId: Int,
        cardId: Int,
        category: String,
        quantity: Int = 1,
        label: String = ",#656565"
    ): Result<ModifyCardsResponse> {
        val modification = CardModification.add(
            cardId = cardId.toString(),
            categories = listOf(category),
            quantity = quantity,
            label = label
        )
        return modifyCards(deckId, listOf(modification))
    }
}
