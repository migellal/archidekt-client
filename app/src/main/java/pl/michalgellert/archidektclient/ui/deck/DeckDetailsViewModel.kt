package pl.michalgellert.archidektclient.ui.deck

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import pl.michalgellert.archidektclient.data.auth.AuthManager
import pl.michalgellert.archidektclient.data.model.CardData
import pl.michalgellert.archidektclient.data.model.CardGroup
import pl.michalgellert.archidektclient.data.model.CardModification
import pl.michalgellert.archidektclient.data.model.ColorLabel
import pl.michalgellert.archidektclient.data.model.DeckData
import pl.michalgellert.archidektclient.data.model.SearchResultCard
import pl.michalgellert.archidektclient.data.model.groupByType
import pl.michalgellert.archidektclient.data.model.groupByTag
import pl.michalgellert.archidektclient.data.model.getTagSummary
import pl.michalgellert.archidektclient.data.repository.ArchidektRepository

class DeckDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)
    private val repository = ArchidektRepository.getInstance(authManager)

    private val _uiState = MutableStateFlow(DeckDetailsUiState())
    val uiState: StateFlow<DeckDetailsUiState> = _uiState.asStateFlow()

    private var deckData: DeckData? = null
    private var searchJob: Job? = null

    fun loadDeck(deckId: Int, forceReload: Boolean = false) {
        if (_uiState.value.isLoading) return
        // Always reload if local data exists but repository cache was cleared (e.g., after card edit)
        val cacheWasCleared = deckData != null && !repository.isCacheValid(deckId)
        if (!forceReload && !cacheWasCleared && deckData != null && deckData?.id == deckId) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getDeckData(deckId, forceRefresh = forceReload)

            if (result.isSuccess) {
                val deck = result.getOrNull()!!
                deckData = deck

                Log.d("DeckDetailsVM", "Loaded deck: ${deck.name}, total cards in map: ${deck.cardMap.size}")
                Log.d("DeckDetailsVM", "Main deck: ${deck.mainDeckCount} cards (${deck.mainDeckCards.size} unique)")
                Log.d("DeckDetailsVM", "Sideboard/Maybeboard: ${deck.sideboardCount} cards (${deck.sideboardCards.size} unique)")

                // Debug: check first card's image URLs
                deck.cardMap.values.firstOrNull()?.let { card ->
                    Log.d("DeckDetailsVM", "Sample card: ${card.name}")
                    Log.d("DeckDetailsVM", "  uid: ${card.uid}")
                    Log.d("DeckDetailsVM", "  smallImageUrl: ${card.smallImageUrl}")
                    Log.d("DeckDetailsVM", "  fullImageUrl: ${card.fullImageUrl}")
                }

                updateCardGroups()
            } else {
                Log.e("DeckDetailsVM", "Failed to load deck", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load deck"
                    )
                }
            }
        }
    }

    private fun updateCardGroups() {
        val deck = deckData ?: return
        val state = _uiState.value

        // Get all tag names for filtering
        val allTags = deck.cardMap.getTagSummary().keys

        // Filter main deck cards based on search and selected tags
        val filteredMainDeck = filterCards(deck.mainDeckCards, state.searchQuery, state.selectedTags, allTags)
        val filteredSideboard = filterCards(deck.sideboardCards, state.searchQuery, state.selectedTags, allTags)

        // Group main deck cards
        val mainDeckGroups = when (state.groupingMode) {
            GroupingMode.TYPE -> filteredMainDeck.groupByType()
            GroupingMode.TAG -> filteredMainDeck.groupByTag(deck.colorLabels)
        }

        // Group sideboard cards using the same grouping mode as main deck
        val sideboardGroups = when (state.groupingMode) {
            GroupingMode.TYPE -> filteredSideboard.groupByType()
            GroupingMode.TAG -> filteredSideboard.groupByTag(deck.colorLabels)
        }

        val tagSummary = deck.mainDeckCards.getTagSummary()

        // Expand all groups by default on first load
        val allGroupNames = if (state.cardGroups.isEmpty()) {
            (mainDeckGroups + sideboardGroups).map { it.name }.toSet()
        } else {
            state.expandedGroups
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                deckName = deck.name,
                deckFormat = deck.format,
                mainDeckCount = deck.mainDeckCount,
                sideboardCount = deck.sideboardCount,
                featuredImage = deck.featuredImage,
                colorLabels = deck.colorLabels,
                tagSummary = tagSummary,
                cardGroups = mainDeckGroups,
                sideboardGroups = sideboardGroups,
                allCards = deck.cardMap.values.toList(),
                expandedGroups = allGroupNames
            )
        }
    }

    private fun filterCards(
        cards: Map<String, CardData>,
        query: String,
        selectedTags: Set<String>,
        allTags: Set<String>
    ): Map<String, CardData> {
        var result = cards

        // Filter by search query
        if (query.isNotBlank()) {
            result = result.filterValues { card ->
                card.name.contains(query, ignoreCase = true) ||
                        card.types.any { it.contains(query, ignoreCase = true) } ||
                        card.text?.contains(query, ignoreCase = true) == true
            }
        }

        // Filter by selected tags (if not all or none selected)
        if (selectedTags.isNotEmpty() && selectedTags != allTags) {
            result = result.filterValues { card ->
                val cardTag = card.colorLabel?.name ?: ""
                cardTag in selectedTags
            }
        }

        return result
    }

    fun setGroupingMode(mode: GroupingMode) {
        if (_uiState.value.groupingMode == mode) return

        _uiState.update { it.copy(groupingMode = mode) }
        updateCardGroups()

        // Expand all groups when changing grouping mode
        val allGroupNames = (_uiState.value.cardGroups + _uiState.value.sideboardGroups).map { it.name }.toSet()
        _uiState.update { it.copy(expandedGroups = allGroupNames) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateCardGroups()

        // Expand all groups after search
        val allGroupNames = (_uiState.value.cardGroups + _uiState.value.sideboardGroups).map { it.name }.toSet()
        _uiState.update { it.copy(expandedGroups = allGroupNames) }
    }

    fun toggleTag(tagName: String) {
        val currentSelected = _uiState.value.selectedTags.toMutableSet()
        val allTags = _uiState.value.tagSummary.keys

        if (tagName in currentSelected) {
            currentSelected.remove(tagName)
        } else {
            currentSelected.add(tagName)
        }

        // If all tags selected or none selected, clear selection (show all)
        val newSelected = if (currentSelected.isEmpty() || currentSelected == allTags) {
            emptySet()
        } else {
            currentSelected
        }

        _uiState.update { it.copy(selectedTags = newSelected) }
        updateCardGroups()
    }

    fun toggleGroupExpansion(groupName: String) {
        _uiState.update { state ->
            val newExpanded = state.expandedGroups.toMutableSet()
            if (groupName in newExpanded) {
                newExpanded.remove(groupName)
            } else {
                newExpanded.add(groupName)
            }
            state.copy(expandedGroups = newExpanded)
        }
    }

    fun expandAllGroups() {
        val allGroupNames = (_uiState.value.cardGroups + _uiState.value.sideboardGroups).map { it.name }.toSet()
        _uiState.update { it.copy(expandedGroups = allGroupNames) }
    }

    fun collapseAllGroups() {
        _uiState.update { it.copy(expandedGroups = emptySet()) }
    }

    fun refresh() {
        deckData?.let { deck ->
            // Clear cache and reload
            repository.clearDeckCache()
            loadDeck(deck.id, forceReload = true)
        }
    }

    // Search functionality
    fun setCardSearchQuery(query: String) {
        _uiState.update { it.copy(cardSearchQuery = query) }

        // Debounce search
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // Wait 300ms before searching
                searchCards(query)
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    private suspend fun searchCards(query: String) {
        _uiState.update { it.copy(isSearching = true) }

        val result = repository.searchCards(query)

        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = result.getOrDefault(emptyList())
                )
            }
        } else {
            Log.e("DeckDetailsVM", "Search failed", result.exceptionOrNull())
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchError = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                cardSearchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null
            )
        }
    }

    fun addCardToDeck(
        card: SearchResultCard,
        category: String,
        quantity: Int = 1,
        onSuccess: () -> Unit
    ) {
        val deck = deckData ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingCard = true) }

            val result = repository.addCardToDeck(
                deckId = deck.id,
                cardId = card.id,
                category = category,
                quantity = quantity
            )

            if (result.isSuccess) {
                Log.d("DeckDetailsVM", "Card added successfully: ${card.name}")
                repository.clearDeckCache()
                _uiState.update { it.copy(isAddingCard = false, addCardSuccess = true) }
                onSuccess()
                // Reload deck to show new card
                loadDeck(deck.id, forceReload = true)
            } else {
                Log.e("DeckDetailsVM", "Failed to add card", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isAddingCard = false,
                        searchError = result.exceptionOrNull()?.message ?: "Failed to add card"
                    )
                }
            }
        }
    }

    fun clearAddCardSuccess() {
        _uiState.update { it.copy(addCardSuccess = false) }
    }

    fun clearSearchError() {
        _uiState.update { it.copy(searchError = null) }
    }

    fun getAvailableCategories(): List<String> {
        return deckData?.categories?.keys?.sorted() ?: emptyList()
    }
}

data class DeckDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deckName: String = "",
    val deckFormat: Int = 0,
    val mainDeckCount: Int = 0,
    val sideboardCount: Int = 0,
    val featuredImage: String? = null,
    val colorLabels: List<ColorLabel> = emptyList(),
    val tagSummary: Map<String, Int> = emptyMap(),
    val cardGroups: List<CardGroup> = emptyList(),
    val sideboardGroups: List<CardGroup> = emptyList(),
    val allCards: List<CardData> = emptyList(),
    val groupingMode: GroupingMode = GroupingMode.TYPE,
    val viewMode: ViewMode = ViewMode.GRID,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val expandedGroups: Set<String> = emptySet(),
    // Card search/add
    val cardSearchQuery: String = "",
    val searchResults: List<SearchResultCard> = emptyList(),
    val isSearching: Boolean = false,
    val isAddingCard: Boolean = false,
    val searchError: String? = null,
    val addCardSuccess: Boolean = false
) {
    val isAllExpanded: Boolean
        get() = expandedGroups.size == (cardGroups.size + sideboardGroups.size) &&
                (cardGroups.isNotEmpty() || sideboardGroups.isNotEmpty())
}

enum class GroupingMode {
    TYPE, TAG
}

enum class ViewMode {
    GRID, LIST
}
