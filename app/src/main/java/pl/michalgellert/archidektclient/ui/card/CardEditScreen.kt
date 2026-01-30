package pl.michalgellert.archidektclient.ui.card

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import pl.michalgellert.archidektclient.data.model.CardData
import pl.michalgellert.archidektclient.data.model.CategoryInfo
import pl.michalgellert.archidektclient.data.model.ColorLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditScreen(
    deckId: Int,
    cardId: String,
    viewModel: CardEditViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deckId, cardId) {
        viewModel.loadCard(deckId, cardId)
    }

    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Card updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            Toast.makeText(context, "Card removed from deck", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.addCopySuccess) {
        if (uiState.addCopySuccess) {
            Toast.makeText(context, "Card copy added to deck", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.deckName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.hasChanges) {
                        TextButton(
                            onClick = { viewModel.saveChanges(onSaveSuccess) },
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.card != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Card Image and Info
                    CardHeader(card = uiState.card!!)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Tag Selection
                    TagSection(
                        availableTags = uiState.availableTags,
                        selectedTag = uiState.selectedTag,
                        onTagSelect = viewModel::selectTag
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Category Selection
                    CategorySection(
                        availableCategories = uiState.availableCategories,
                        selectedCategories = uiState.selectedCategories,
                        onEditClick = { showCategoryDialog = true },
                        onMoveUp = viewModel::moveCategoryUp,
                        onMoveDown = viewModel::moveCategoryDown
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Quantity
                    QuantitySection(
                        quantity = uiState.quantity,
                        onIncrement = viewModel::incrementQuantity,
                        onDecrement = viewModel::decrementQuantity
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Oracle Text
                    uiState.card?.let { card ->
                        if (card.text != null) {
                            OracleTextSection(card = card)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Action Buttons
                    ActionButtonsSection(
                        onDeleteClick = { showDeleteDialog = true },
                        onAddCopyClick = {
                            viewModel.addCardCopy {
                                onSaveSuccess()
                            }
                        },
                        isLoading = uiState.isDeleting || uiState.isAddingCopy
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card") },
            text = {
                Text("Are you sure you want to remove \"${uiState.card?.displayCardName}\" from this deck?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCard {
                            onSaveSuccess()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category Selection Dialog
    if (showCategoryDialog) {
        CategorySelectionDialog(
            availableCategories = uiState.availableCategories,
            selectedCategories = uiState.selectedCategories,
            onCategoryToggle = viewModel::toggleCategory,
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
private fun CardHeader(card: CardData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card Images - show both sides for double-faced cards
        if (card.isDoubleFaced && card.backImageUrl != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                // Front face
                CardImage(
                    imageUrl = card.fullImageUrl,
                    contentDescription = card.frontFaceName,
                    modifier = Modifier.width(140.dp)
                )
                // Back face
                CardImage(
                    imageUrl = card.backImageUrl,
                    contentDescription = card.backFaceName ?: "Back",
                    modifier = Modifier.width(140.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            // Single-faced card
            CardImage(
                imageUrl = card.fullImageUrl,
                contentDescription = card.name,
                modifier = Modifier.width(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Name
        Text(
            text = card.displayCardName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Mana Cost
        if (card.manaCostSymbols.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ManaCostRow(manaCost = card.manaCostSymbols)
        }

        // Type Line
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = card.typeLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CardImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.aspectRatio(0.72f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(200)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        )
    }
}

@Composable
private fun ManaCostRow(manaCost: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            .size(24.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagSection(
    availableTags: List<ColorLabel>,
    selectedTag: ColorLabel?,
    onTagSelect: (ColorLabel?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Tag",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Default Tag option (no tag)
            val isDefaultSelected = selectedTag == null || selectedTag.name.isBlank()
            FilterChip(
                selected = isDefaultSelected,
                onClick = { onTagSelect(null) },
                label = { Text("Default Tag") },
                leadingIcon = if (isDefaultSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                } else null
            )

            // Available tags
            availableTags.filter { it.name.isNotBlank() }.forEach { tag ->
                val tagColor = parseColor(tag.color)
                val isSelected = selectedTag?.name == tag.name

                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelect(tag) },
                    label = { Text(tag.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(tagColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tagColor.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
private fun CategorySection(
    availableCategories: List<CategoryInfo>,
    selectedCategories: List<String>,
    onEditClick: () -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onEditClick) {
                Text("Edit")
            }
        }

        if (selectedCategories.isNotEmpty()) {
            Text(
                text = "First category determines card placement",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Ordered list of selected categories
            selectedCategories.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position indicator
                    Surface(
                        color = if (index == 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == 0)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Category name
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    // Move up button
                    IconButton(
                        onClick = { onMoveUp(category) },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (index > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Move down button
                    IconButton(
                        onClick = { onMoveDown(category) },
                        enabled = index < selectedCategories.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = if (index < selectedCategories.size - 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No categories selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun QuantitySection(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quantity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDecrement,
                enabled = quantity > 1,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Decrease")
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedButton(
                onClick = onIncrement,
                enabled = quantity < 99,
                modifier = Modifier.size(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun OracleTextSection(card: CardData) {
    val backText = card.backFaceText
    val frontText = card.frontFaceText

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Oracle Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Check if card is double-faced with separate texts
        if (card.isDoubleFaced && backText != null) {
            // Front face
            Text(
                text = card.frontFaceName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = frontText ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back face
            Text(
                text = card.backFaceName ?: "Back",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = backText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Single-faced card or combined text
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = card.text ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onDeleteClick: () -> Unit,
    onAddCopyClick: () -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Delete Button
            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }

            // Add Copy Button
            Button(
                onClick = onAddCopyClick,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Copy")
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionDialog(
    availableCategories: List<CategoryInfo>,
    selectedCategories: List<String>,
    onCategoryToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Categories") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "New categories will be added at the end. Use arrows to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                availableCategories.forEach { category ->
                    val isSelected = category.name in selectedCategories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryToggle(category.name) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onCategoryToggle(category.name) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = category.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
