package pl.michalgellert.archidektclient.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import pl.michalgellert.archidektclient.data.model.DeckColors
import pl.michalgellert.archidektclient.data.model.DeckFormat
import pl.michalgellert.archidektclient.data.model.DeckSummary
import pl.michalgellert.archidektclient.data.model.Subfolder
import pl.michalgellert.archidektclient.ui.components.AppOverflowMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksListScreen(
    viewModel: DecksListViewModel = viewModel(),
    onDeckClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Decks") },
                actions = { AppOverflowMenu() }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.foldersWithDecks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.foldersWithDecks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Pull to refresh",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Log.d("DecksListScreen", "Rendering ${uiState.foldersWithDecks.size} folders, expandedIds=${uiState.expandedFolderIds}")
                    uiState.foldersWithDecks.forEachIndexed { index, f ->
                        Log.d("DecksListScreen", "  [$index] ${f.folder.name} (id=${f.folder.id}, isRoot=${f.folder.isRoot}, decks=${f.decks.size})")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        // Loaded folders with decks
                        uiState.foldersWithDecks.forEach { folderWithDecks ->
                            val isExpanded = folderWithDecks.folder.id in uiState.expandedFolderIds
                            Log.d("DecksListScreen", "Folder ${folderWithDecks.folder.name}: isExpanded=$isExpanded")

                            item(key = "folder_${folderWithDecks.folder.id}") {
                                FolderHeader(
                                    folder = folderWithDecks.folder,
                                    deckCount = folderWithDecks.decks.size,
                                    isExpanded = isExpanded,
                                    onClick = { viewModel.toggleFolder(folderWithDecks.folder.id) }
                                )
                            }

                            if (isExpanded) {
                                items(
                                    folderWithDecks.decks,
                                    key = { "deck_${it.id}" }
                                ) { deck ->
                                    DeckCard(
                                        deck = deck,
                                        onClick = {
                                            viewModel.clearDeckCache()
                                            onDeckClick(deck.id)
                                        },
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                                    )
                                }

                                item(key = "spacer_${folderWithDecks.folder.id}") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                        // Unloaded subfolders
                        val loadedFolderIds = uiState.foldersWithDecks.map { it.folder.id }.toSet()
                        val unloadedFolders = uiState.subfolders.filter { it.id !in loadedFolderIds }

                        if (unloadedFolders.isNotEmpty()) {
                            item(key = "subfolders_header") {
                                Text(
                                    text = "Other Folders",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(unloadedFolders, key = { "unloaded_${it.id}" }) { subfolder ->
                                SubfolderCard(
                                    subfolder = subfolder,
                                    onClick = { viewModel.loadSubfolder(subfolder.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubfolderCard(
    subfolder: Subfolder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subfolder.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Tap to load",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderHeader(
    folder: Subfolder,
    deckCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded)
                    Icons.Default.KeyboardArrowDown
                else
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = deckCount.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DeckCard(
    deck: DeckSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Featured image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                deck.featuredImage?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Deck info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = DeckFormat.getName(deck.deckFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ColorIdentityDots(deck.colors)
                }
            }
        }
    }
}

@Composable
private fun ColorIdentityDots(colors: DeckColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val colorList = colors.toColorList()
        if (colorList.isEmpty()) {
            ColorDot(Color(0xFF9E9E9E)) // Colorless
        } else {
            colorList.forEach { color ->
                ColorDot(getMtgColor(color))
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun getMtgColor(symbol: String): Color = when (symbol) {
    "W" -> Color(0xFFF9FAF4) // White
    "U" -> Color(0xFF0E68AB) // Blue
    "B" -> Color(0xFF150B00) // Black
    "R" -> Color(0xFFD3202A) // Red
    "G" -> Color(0xFF00733E) // Green
    else -> Color(0xFF9E9E9E) // Colorless
}
