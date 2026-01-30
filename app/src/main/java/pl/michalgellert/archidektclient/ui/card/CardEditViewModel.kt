package pl.michalgellert.archidektclient.ui.card

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.michalgellert.archidektclient.data.auth.AuthManager
import pl.michalgellert.archidektclient.data.model.CardData
import pl.michalgellert.archidektclient.data.model.CardModification
import pl.michalgellert.archidektclient.data.model.CategoryInfo
import pl.michalgellert.archidektclient.data.model.ColorLabel
import pl.michalgellert.archidektclient.data.model.DeckData
import pl.michalgellert.archidektclient.data.repository.ArchidektRepository

class CardEditViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)
    private val repository = ArchidektRepository.getInstance(authManager)

    private val _uiState = MutableStateFlow(CardEditUiState())
    val uiState: StateFlow<CardEditUiState> = _uiState.asStateFlow()

    private var deckData: DeckData? = null
    private var originalCard: CardData? = null

    fun loadCard(deckId: Int, cardId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.getDeckData(deckId)

            if (result.isSuccess) {
                val deck = result.getOrNull()!!
                deckData = deck

                Log.d("CardEditVM", "Looking for cardId: '$cardId'")
                Log.d("CardEditVM", "CardMap keys: ${deck.cardMap.keys.take(5)}")

                val card = deck.cardMap[cardId]
                if (card == null) {
                    Log.e("CardEditVM", "Card not found! cardId='$cardId', available keys=${deck.cardMap.keys}")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Card not found: $cardId")
                    }
                    return@launch
                }

                originalCard = card

                // Get all available categories
                val availableCategories = deck.categories.values
                    .sortedBy { it.name }

                Log.d("CardEditVM", "Loaded card: ${card.name}")
                Log.d("CardEditVM", "Categories: ${card.categories}")
                Log.d("CardEditVM", "Tag: ${card.colorLabel}")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        deckId = deckId,
                        deckName = deck.name,
                        card = card,
                        availableCategories = availableCategories,
                        availableTags = deck.colorLabels,
                        selectedCategories = card.categories,  // Preserve order from API
                        selectedTag = card.colorLabel,
                        quantity = card.qty
                    )
                }
            } else {
                Log.e("CardEditVM", "Failed to load deck", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load deck"
                    )
                }
            }
        }
    }

    fun toggleCategory(categoryName: String) {
        _uiState.update { state ->
            val newCategories = state.selectedCategories.toMutableList()
            if (categoryName in newCategories) {
                // Don't allow removing the last category
                if (newCategories.size > 1) {
                    newCategories.remove(categoryName)
                }
            } else {
                // Add new category at the end
                newCategories.add(categoryName)
            }
            state.copy(selectedCategories = newCategories, hasChanges = true)
        }
    }

    fun moveCategoryUp(categoryName: String) {
        _uiState.update { state ->
            val categories = state.selectedCategories.toMutableList()
            val index = categories.indexOf(categoryName)
            if (index > 0) {
                categories.removeAt(index)
                categories.add(index - 1, categoryName)
            }
            state.copy(selectedCategories = categories, hasChanges = true)
        }
    }

    fun moveCategoryDown(categoryName: String) {
        _uiState.update { state ->
            val categories = state.selectedCategories.toMutableList()
            val index = categories.indexOf(categoryName)
            if (index >= 0 && index < categories.size - 1) {
                categories.removeAt(index)
                categories.add(index + 1, categoryName)
            }
            state.copy(selectedCategories = categories, hasChanges = true)
        }
    }

    fun selectTag(tag: ColorLabel?) {
        _uiState.update { it.copy(selectedTag = tag, hasChanges = true) }
    }

    fun incrementQuantity() {
        _uiState.update { state ->
            if (state.quantity < 99) {
                state.copy(quantity = state.quantity + 1, hasChanges = true)
            } else state
        }
    }

    fun decrementQuantity() {
        _uiState.update { state ->
            if (state.quantity > 1) {
                state.copy(quantity = state.quantity - 1, hasChanges = true)
            } else state
        }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val state = _uiState.value
        val card = state.card ?: return
        val deckId = state.deckId

        if (!state.hasChanges) {
            onSuccess()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val label = state.selectedTag?.let { "${it.name},${it.color}" } ?: ",#656565"

            val modification = CardModification.modify(
                cardId = card.cardId ?: card.id,
                deckRelationId = card.deckRelationId ?: "",
                categories = state.selectedCategories.toList(),
                quantity = state.quantity,
                label = label
            )

            val result = repository.modifyCards(deckId, listOf(modification))

            if (result.isSuccess) {
                Log.d("CardEditVM", "Card modified successfully")
                // Clear deck cache so DeckDetailsScreen will refresh
                repository.clearDeckCache()
                _uiState.update { it.copy(isSaving = false, hasChanges = false, saveSuccess = true) }
                onSuccess()
            } else {
                Log.e("CardEditVM", "Failed to modify card", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save changes"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun deleteCard(onSuccess: () -> Unit) {
        val state = _uiState.value
        val card = state.card ?: return
        val deckId = state.deckId

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            val modification = CardModification.remove(
                cardId = card.cardId ?: card.id,
                deckRelationId = card.deckRelationId ?: ""
            )

            val result = repository.modifyCards(deckId, listOf(modification))

            if (result.isSuccess) {
                Log.d("CardEditVM", "Card deleted successfully")
                repository.clearDeckCache()
                _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                onSuccess()
            } else {
                Log.e("CardEditVM", "Failed to delete card", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete card"
                    )
                }
            }
        }
    }

    fun addCardCopy(onSuccess: () -> Unit) {
        val state = _uiState.value
        val card = state.card ?: return
        val deckId = state.deckId

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingCopy = true) }

            val label = state.selectedTag?.let { "${it.name},${it.color}" } ?: ",#656565"
            val cardIdToUse = card.cardId ?: card.id

            Log.d("CardEditVM", "Adding card copy:")
            Log.d("CardEditVM", "  card.id (map key): ${card.id}")
            Log.d("CardEditVM", "  card.cardId: ${card.cardId}")
            Log.d("CardEditVM", "  using cardId: $cardIdToUse")
            Log.d("CardEditVM", "  categories: ${state.selectedCategories}")
            Log.d("CardEditVM", "  label: $label")

            val modification = CardModification.add(
                cardId = cardIdToUse,
                categories = state.selectedCategories.toList(),
                label = label
            )

            val result = repository.modifyCards(deckId, listOf(modification))

            if (result.isSuccess) {
                Log.d("CardEditVM", "Card copy added successfully")
                repository.clearDeckCache()
                _uiState.update { it.copy(isAddingCopy = false, addCopySuccess = true) }
                onSuccess()
            } else {
                Log.e("CardEditVM", "Failed to add card copy", result.exceptionOrNull())
                _uiState.update {
                    it.copy(
                        isAddingCopy = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to add card copy"
                    )
                }
            }
        }
    }
}

data class CardEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isAddingCopy: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val addCopySuccess: Boolean = false,
    val deckId: Int = 0,
    val deckName: String = "",
    val card: CardData? = null,
    val availableCategories: List<CategoryInfo> = emptyList(),
    val availableTags: List<ColorLabel> = emptyList(),
    val selectedCategories: List<String> = emptyList(),
    val selectedTag: ColorLabel? = null,
    val quantity: Int = 1,
    val hasChanges: Boolean = false
)
