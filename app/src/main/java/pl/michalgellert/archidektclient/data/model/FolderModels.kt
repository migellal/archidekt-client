package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName

data class FolderResponse(
    val id: Int,
    val name: String,
    val parentFolder: ParentFolder? = null,
    val private: Boolean = false,
    val owner: DeckOwner? = null,
    val subfolders: List<Subfolder> = emptyList(),
    val decks: List<DeckSummary> = emptyList(),
    val count: Int = 0,
    val next: String? = null
)

data class ParentFolder(
    val id: Int,
    val name: String
)

data class Subfolder(
    val id: Int,
    val name: String,
    val createdAt: String? = null,
    val private: Boolean = false,
    val isRoot: Boolean = false
)

// For organizing decks by folder in UI
data class FolderWithDecks(
    val folder: Subfolder,
    val decks: List<DeckSummary>
)

// Helper to group decks by folder
fun List<DeckSummary>.groupByFolder(
    subfolders: List<Subfolder>,
    rootFolderId: Int,
    rootFolderName: String = "Home"
): List<FolderWithDecks> {
    val folderMap = subfolders.associateBy { it.id }.toMutableMap()
    // Add root folder with special flag
    folderMap[rootFolderId] = Subfolder(rootFolderId, rootFolderName, isRoot = true)

    val grouped = this.groupBy { it.parentFolderId ?: rootFolderId }

    return folderMap.values
        .mapNotNull { folder ->
            val folderDecks = grouped[folder.id]
            if (folderDecks != null && folderDecks.isNotEmpty()) {
                FolderWithDecks(folder, folderDecks.sortedByDescending { it.updatedAt })
            } else if (folder.id == rootFolderId) {
                // Always show root folder even if empty
                FolderWithDecks(folder, emptyList())
            } else {
                null
            }
        }
        .sortedWith(compareBy(
            { !it.folder.isRoot }, // Root first (isRoot=true -> false comes first)
            { it.folder.name.lowercase() }
        ))
}
