package pl.michalgellert.archidektclient.ui.deck

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import pl.michalgellert.archidektclient.R
import pl.michalgellert.archidektclient.data.model.CardData
import pl.michalgellert.archidektclient.data.model.CardGroup
import pl.michalgellert.archidektclient.data.model.ColorLabel
import pl.michalgellert.archidektclient.data.model.DeckFormat
import pl.michalgellert.archidektclient.data.model.SearchResultCard
import pl.michalgellert.archidektclient.ui.components.AppOverflowMenu
import pl.michalgellert.archidektclient.ui.theme.contrastingTextColor
import pl.michalgellert.archidektclient.ui.theme.parseColorSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailsScreen(
    deckId: Int,
    deckName: String,
    viewModel: DeckDetailsViewModel = viewModel(),
    onBackClick: () -> Unit,
    onCardClick: (CardData) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddCardSheet by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load deck on first composition
    LaunchedEffect(deckId) {
        viewModel.loadDeck(deckId)
    }

    // Reload when screen becomes visible again (e.g., after returning from card edit)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDeck(deckId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show toast on card added
    LaunchedEffect(uiState.addCardSuccess) {
        if (uiState.addCardSuccess) {
            Toast.makeText(context, "Card added to deck", Toast.LENGTH_SHORT).show()
            viewModel.clearAddCardSuccess()
        }
    }

    // Show toast on error
    LaunchedEffect(uiState.searchError) {
        uiState.searchError?.let { error ->
            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            viewModel.clearSearchError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search cards...") },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = uiState.deckName.ifBlank { deckName },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showSearchBar) {
                            showSearchBar = false
                            viewModel.setSearchQuery("")
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (showSearchBar) "Close search" else "Back"
                        )
                    }
                },
                actions = {
                    if (!showSearchBar) {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    AppOverflowMenu()
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCardSheet = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add card")
            }
        }
    ) { padding ->

    // Add Card Bottom Sheet
    if (showAddCardSheet) {
        AddCardBottomSheet(
            sheetState = sheetState,
            searchQuery = uiState.cardSearchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            isAddingCard = uiState.isAddingCard,
            availableCategories = viewModel.getAvailableCategories(),
            onSearchQueryChange = { viewModel.setCardSearchQuery(it) },
            onAddCard = { card, category ->
                viewModel.addCardToDeck(card, category) {
                    // Card added successfully
                }
            },
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showAddCardSheet = false
                    viewModel.clearSearch()
                }
            }
        )
    }
        when {
            uiState.isLoading && uiState.cardGroups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.cardGroups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to retry",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { viewModel.loadDeck(deckId) }
                        )
                    }
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Header with deck info
                    item {
                        DeckHeader(
                            featuredImage = uiState.featuredImage,
                            deckName = uiState.deckName,
                            deckFormat = uiState.deckFormat,
                            mainDeckCount = uiState.mainDeckCount,
                            sideboardCount = uiState.sideboardCount
                        )
                    }

                    // Tag summary (clickable filters) + View controls in one compact section
                    if (uiState.tagSummary.isNotEmpty()) {
                        item {
                            TagSummary(
                                tagSummary = uiState.tagSummary,
                                colorLabels = uiState.colorLabels,
                                selectedTags = uiState.selectedTags,
                                onTagClick = viewModel::toggleTag
                            )
                        }
                    }

                    // View controls
                    item {
                        ViewControls(
                            viewMode = uiState.viewMode,
                            groupingMode = uiState.groupingMode,
                            onViewModeChange = viewModel::setViewMode,
                            onGroupingModeChange = viewModel::setGroupingMode
                        )
                    }

                    // Main deck card groups
                    uiState.cardGroups.forEach { group ->
                        val isExpanded = group.name in uiState.expandedGroups

                        item(key = "header_${group.name}") {
                            GroupHeader(
                                group = group,
                                isExpanded = isExpanded,
                                onClick = { viewModel.toggleGroupExpansion(group.name) }
                            )
                        }

                        if (isExpanded) {
                            if (uiState.viewMode == ViewMode.GRID) {
                                item(key = "grid_${group.name}") {
                                    CardGrid(
                                        cards = group.cards,
                                        onCardClick = onCardClick
                                    )
                                }
                            } else {
                                items(
                                    items = group.cards,
                                    key = { "card_${it.id}" }
                                ) { card ->
                                    CardListItem(
                                        card = card,
                                        onClick = { onCardClick(card) }
                                    )
                                }
                            }
                        }
                    }

                    // Sideboard/Maybeboard section
                    if (uiState.sideboardGroups.isNotEmpty()) {
                        item {
                            SideboardDivider()
                        }

                        uiState.sideboardGroups.forEach { group ->
                            val isExpanded = group.name in uiState.expandedGroups

                            item(key = "sideboard_header_${group.name}") {
                                SideboardGroupHeader(
                                    group = group,
                                    isExpanded = isExpanded,
                                    onClick = { viewModel.toggleGroupExpansion(group.name) }
                                )
                            }

                            if (isExpanded) {
                                if (uiState.viewMode == ViewMode.GRID) {
                                    item(key = "sideboard_grid_${group.name}") {
                                        CardGrid(
                                            cards = group.cards,
                                            onCardClick = onCardClick
                                        )
                                    }
                                } else {
                                    items(
                                        items = group.cards,
                                        key = { "sideboard_card_${it.id}" }
                                    ) { card ->
                                        CardListItem(
                                            card = card,
                                            onClick = { onCardClick(card) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckHeader(
    featuredImage: String?,
    deckName: String,
    deckFormat: Int,
    mainDeckCount: Int,
    sideboardCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Featured image (compact)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            featuredImage?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deckName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DeckFormat.getName(deckFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (sideboardCount > 0) {
                        "$mainDeckCount (+$sideboardCount)"
                    } else {
                        "$mainDeckCount cards"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSummary(
    tagSummary: Map<String, Int>,
    colorLabels: List<ColorLabel>,
    selectedTags: Set<String>,
    onTagClick: (String) -> Unit
) {
    val colorMap = colorLabels.associateBy { it.name }

    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tagSummary.entries.toList()) { (tagName, count) ->
            val colorLabel = colorMap[tagName]
            val tagColor = colorLabel?.color?.let { parseColorSafe(it) }
                ?: MaterialTheme.colorScheme.primary
            val textColor = tagColor.contrastingTextColor()

            val isSelected = tagName in selectedTags || selectedTags.isEmpty()

            FilterChip(
                selected = isSelected,
                onClick = { onTagClick(tagName) },
                label = {
                    Text(
                        text = "$tagName $count",
                        color = if (isSelected) textColor else tagColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = tagColor,
                    selectedLabelColor = textColor
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = tagColor.copy(alpha = 0.5f),
                    selectedBorderColor = tagColor
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewControls(
    viewMode: ViewMode,
    groupingMode: GroupingMode,
    onViewModeChange: (ViewMode) -> Unit,
    onGroupingModeChange: (GroupingMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // View mode toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.height(32.dp)
        ) {
            SegmentedButton(
                selected = viewMode == ViewMode.GRID,
                onClick = { onViewModeChange(ViewMode.GRID) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Grid", style = MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = viewMode == ViewMode.LIST,
                onClick = { onViewModeChange(ViewMode.LIST) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("List", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Grouping mode toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.height(32.dp)
        ) {
            SegmentedButton(
                selected = groupingMode == GroupingMode.TYPE,
                onClick = { onGroupingModeChange(GroupingMode.TYPE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Type", style = MaterialTheme.typography.labelSmall)
            }
            SegmentedButton(
                selected = groupingMode == GroupingMode.TAG,
                onClick = { onGroupingModeChange(GroupingMode.TAG) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Tag", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: CardGroup,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded)
                    Icons.Default.KeyboardArrowDown
                else
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = group.cardCount.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SideboardDivider() {
    Column(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Not in deck",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SideboardGroupHeader(
    group: CardGroup,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded)
                    Icons.Default.KeyboardArrowDown
                else
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = group.cardCount.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
private fun CardGrid(
    cards: List<CardData>,
    onCardClick: (CardData) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards, key = { it.id }) { card ->
            CardGridItem(
                card = card,
                onClick = { onCardClick(card) }
            )
        }
    }
}

@Composable
private fun CardGridItem(
    card: CardData,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(card.smallImageUrl ?: card.fullImageUrl)
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = card.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    },
                    error = {
                        // Fallback: show first letter of card name
                        Text(
                            text = card.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Quantity badge
                if (card.qty > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${card.qty}x",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Tag color indicator
                card.colorLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(parseColorSafe(label.color))
                    )
                }
            }

            // Card name
            Text(
                text = card.displayCardName,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CardListItem(
    card: CardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity
            Text(
                text = "${card.qty}x",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )

            // Tag color indicator
            card.colorLabel?.let { label ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parseColorSafe(label.color))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.displayCardName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.typeLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mana cost
            ManaCost(manaCost = card.manaCostSymbols)
        }
    }
}

@Composable
private fun ManaCost(manaCost: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        manaCost.forEach { symbol ->
            ManaSymbol(symbol = symbol)
        }
    }
}

@Composable
private fun ManaSymbol(symbol: String) {
    val color = when (symbol.uppercase()) {
        "W" -> Color(0xFFF9FAF4)
        "U" -> Color(0xFF0E68AB)
        "B" -> Color(0xFF150B00)
        "R" -> Color(0xFFD3202A)
        "G" -> Color(0xFF00733E)
        "C" -> Color(0xFFCBC8C4)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (symbol.uppercase()) {
        "W" -> Color.Black
        "B" -> Color.White
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    searchQuery: String,
    searchResults: List<SearchResultCard>,
    isSearching: Boolean,
    isAddingCard: Boolean,
    availableCategories: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onAddCard: (SearchResultCard, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<SearchResultCard?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Add Card",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    selectedCard = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a card...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            selectedCard = null
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search results or selected card
            if (selectedCard != null) {
                // Show selected card with category picker
                SelectedCardView(
                    card = selectedCard!!,
                    selectedCategory = selectedCategory,
                    categoryExpanded = categoryExpanded,
                    availableCategories = availableCategories,
                    isAddingCard = isAddingCard,
                    onCategoryExpandedChange = { categoryExpanded = it },
                    onCategorySelect = {
                        selectedCategory = it
                        categoryExpanded = false
                    },
                    onAddClick = {
                        if (selectedCategory.isNotEmpty()) {
                            onAddCard(selectedCard!!, selectedCategory)
                            selectedCard = null
                            selectedCategory = ""
                        }
                    },
                    onBackClick = { selectedCard = null }
                )
            } else {
                // Show search results
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    searchResults.isEmpty() && searchQuery.length >= 2 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cards found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type at least 2 characters to search",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            items(searchResults) { card ->
                                SearchResultItem(
                                    card = card,
                                    onClick = {
                                        selectedCard = card
                                        selectedCategory = card.defaultCategory
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedCardView(
    card: SearchResultCard,
    selectedCategory: String,
    categoryExpanded: Boolean,
    availableCategories: List<String>,
    isAddingCard: Boolean,
    onCategoryExpandedChange: (Boolean) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Column {
        // Back button
        Row(
            modifier = Modifier
                .clickable(onClick = onBackClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to search",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to search",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card image
            Card(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.72f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(card.smallImageUrl)
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = card.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.typeLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${card.setName} (${card.setCode.uppercase()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                card.cheapestPrice?.let { price ->
                    Text(
                        text = "$${String.format("%.2f", price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category selector
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = onCategoryExpandedChange
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { onCategoryExpandedChange(false) }
            ) {
                availableCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = { onCategorySelect(category) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add button
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCategory.isNotEmpty() && !isAddingCard
        ) {
            if (isAddingCard) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Deck")
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    card: SearchResultCard,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Small card image
            Card(
                modifier = Modifier
                    .width(50.dp)
                    .aspectRatio(0.72f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(card.smallImageUrl)
                        .crossfade(200)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = card.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.typeLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = card.setName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Price
            card.cheapestPrice?.let { price ->
                Text(
                    text = "$${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
