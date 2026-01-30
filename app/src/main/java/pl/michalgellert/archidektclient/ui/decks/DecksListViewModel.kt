package pl.michalgellert.archidektclient.ui.decks

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
import pl.michalgellert.archidektclient.data.model.DeckSummary
import pl.michalgellert.archidektclient.data.model.FolderWithDecks
import pl.michalgellert.archidektclient.data.model.Subfolder
import pl.michalgellert.archidektclient.data.repository.ArchidektRepository

class DecksListViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)
    private val repository = ArchidektRepository.getInstance(authManager)

    private val _uiState = MutableStateFlow(DecksListUiState())
    val uiState: StateFlow<DecksListUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    fun refresh() {
        loadDecks()
    }

    fun toggleFolder(folderId: Int) {
        _uiState.update { state ->
            val newExpanded = state.expandedFolderIds.toMutableSet()
            if (folderId in newExpanded) {
                newExpanded.remove(folderId)
            } else {
                newExpanded.add(folderId)
            }
            state.copy(expandedFolderIds = newExpanded)
        }
    }

    fun isFolderExpanded(folderId: Int): Boolean {
        return folderId in _uiState.value.expandedFolderIds
    }

    private fun loadDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val rootFolderId = repository.getRootFolderId()
            Log.d("DecksListVM", "rootFolderId: $rootFolderId")

            if (rootFolderId != null) {
                val folderResult = repository.getRootFolder()
                Log.d("DecksListVM", "folderResult: ${folderResult.isSuccess}, error: ${folderResult.exceptionOrNull()?.message}")

                if (folderResult.isSuccess) {
                    val folder = folderResult.getOrNull()!!
                    Log.d("DecksListVM", "Loaded root folder: id=${folder.id}, name=${folder.name}, decks=${folder.decks.size}, subfolders=${folder.subfolders.size}")

                    // Create folder with decks for root
                    val rootFolderWithDecks = FolderWithDecks(
                        folder = Subfolder(folder.id, folder.name, isRoot = true),
                        decks = folder.decks
                    )

                    // Expand root by default
                    val expandedIds = setOf(folder.id)
                    Log.d("DecksListVM", "Setting expandedFolderIds to: $expandedIds")

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            foldersWithDecks = listOf(rootFolderWithDecks),
                            subfolders = folder.subfolders,
                            rootFolderId = folder.id,
                            expandedFolderIds = expandedIds
                        )
                    }
                    Log.d("DecksListVM", "State updated, expandedFolderIds now: ${_uiState.value.expandedFolderIds}")
                    return@launch
                }
            }

            // Fallback to simple deck list
            val result = repository.getMyDecks()
            if (result.isSuccess) {
                val decks = result.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        foldersWithDecks = listOf(
                            FolderWithDecks(
                                folder = Subfolder(0, "My Decks", isRoot = true),
                                decks = decks
                            )
                        ),
                        expandedFolderIds = setOf(0)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load decks"
                    )
                }
            }
        }
    }

    fun loadSubfolder(folderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.getFolder(folderId)
            Log.d("DecksListVM", "loadSubfolder $folderId result: ${result.isSuccess}")

            if (result.isSuccess) {
                val folder = result.getOrNull()!!
                val rootId = _uiState.value.rootFolderId
                val isRootFolder = rootId != null && folder.id == rootId

                Log.d("DecksListVM", "Loading folder: id=${folder.id}, name=${folder.name}, rootId=$rootId, isRoot=$isRootFolder")

                val newFolderWithDecks = FolderWithDecks(
                    folder = Subfolder(folder.id, folder.name, isRoot = isRootFolder),
                    decks = folder.decks
                )

                // Add to list and sort
                val currentFolders = _uiState.value.foldersWithDecks.toMutableList()
                val existingIndex = currentFolders.indexOfFirst { it.folder.id == folderId }

                if (existingIndex >= 0) {
                    currentFolders[existingIndex] = newFolderWithDecks
                } else {
                    currentFolders.add(newFolderWithDecks)
                }

                // Sort: root first (using isRoot flag), then alphabetically
                val sortedFolders = currentFolders.sortedWith(
                    compareBy<FolderWithDecks> { !it.folder.isRoot }  // isRoot=true comes first
                        .thenBy { it.folder.name.lowercase() }
                )

                Log.d("DecksListVM", "Before sort: ${currentFolders.map { "${it.folder.name}(isRoot=${it.folder.isRoot})" }}")
                Log.d("DecksListVM", "After sort: ${sortedFolders.map { "${it.folder.name}(isRoot=${it.folder.isRoot})" }}")

                // Merge new subfolders
                val allSubfolders = (_uiState.value.subfolders + folder.subfolders)
                    .distinctBy { it.id }

                // Expand newly loaded folder
                val currentExpandedIds = _uiState.value.expandedFolderIds
                val newExpandedIds = currentExpandedIds + folderId

                Log.d("DecksListVM", "Expanded IDs: before=$currentExpandedIds, after=$newExpandedIds")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        foldersWithDecks = sortedFolders,
                        subfolders = allSubfolders,
                        expandedFolderIds = newExpandedIds
                    )
                }

                Log.d("DecksListVM", "State updated: foldersWithDecks=${_uiState.value.foldersWithDecks.size}, expandedIds=${_uiState.value.expandedFolderIds}")
            } else {
                Log.e("DecksListVM", "Failed to load subfolder: ${result.exceptionOrNull()?.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearDeckCache() {
        repository.clearDeckCache()
    }
}

data class DecksListUiState(
    val isLoading: Boolean = false,
    val foldersWithDecks: List<FolderWithDecks> = emptyList(),
    val subfolders: List<Subfolder> = emptyList(),
    val rootFolderId: Int? = null,
    val error: String? = null,
    val expandedFolderIds: Set<Int> = emptySet()
) {
    val allDecks: List<DeckSummary>
        get() = foldersWithDecks.flatMap { it.decks }
}
