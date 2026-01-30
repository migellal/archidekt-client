package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from /_next/data endpoint for deck details
 */
data class DeckDetailsResponse(
    val pageProps: PageProps
)

data class PageProps(
    val redux: ReduxState
)

data class ReduxState(
    val deck: DeckData
)

data class DeckData(
    val id: Int,
    val name: String,
    val description: String? = null,
    val format: Int,
    val edhBracket: Int? = null,
    val private: Boolean = false,
    val unlisted: Boolean = false,
    val owner: String,
    @SerializedName("ownerid")
    val ownerId: Int,
    val ownerAvatar: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val viewCount: Int = 0,
    val parentFolder: Int? = null,
    val categories: Map<String, CategoryInfo> = emptyMap(),
    val colorLabels: List<ColorLabel> = emptyList(),
    val cardMap: Map<String, CardData> = emptyMap()
) {
    companion object {
        // Categories that are not part of the main deck
        private val EXCLUDED_CATEGORIES = setOf("Sideboard", "Maybeboard")
    }

    // Cards that are part of the main deck (not in Sideboard/Maybeboard)
    val mainDeckCards: Map<String, CardData>
        get() = cardMap.filterValues { card ->
            card.categories.none { it in EXCLUDED_CATEGORIES }
        }

    // Cards in sideboard/maybeboard
    val sideboardCards: Map<String, CardData>
        get() = cardMap.filterValues { card ->
            card.categories.any { it in EXCLUDED_CATEGORIES }
        }

    // Count of main deck cards
    val mainDeckCount: Int
        get() = mainDeckCards.values.sumOf { it.qty }

    // Count of sideboard/maybeboard cards
    val sideboardCount: Int
        get() = sideboardCards.values.sumOf { it.qty }

    // Total cards including sideboard (for display in parentheses)
    val totalCards: Int
        get() = cardMap.values.sumOf { it.qty }

    val featuredImage: String?
        get() = mainDeckCards.values.firstOrNull { "Commander" in it.categories }?.imageUrl
            ?: mainDeckCards.values.firstOrNull()?.imageUrl
}

data class CategoryInfo(
    val id: Int,
    val name: String,
    val isPremier: Boolean = false,
    val includedInDeck: Boolean = true,
    val includedInPrice: Boolean = true
)

data class ColorLabel(
    val name: String,
    val color: String
) {
    val displayName: String
        get() = name.ifBlank { "Default Tag" }
}

data class CardData(
    val id: String,
    val name: String,
    val displayName: String? = null,
    val cmc: Double = 0.0,
    val castingCost: List<Any> = emptyList(),  // Can be String or List<String> for hybrid mana
    val colorIdentity: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val text: String? = null,
    val flavor: String? = null,
    val set: String? = null,
    val setCode: String? = null,
    val releasedAt: String? = null,
    val deckRelationId: String? = null,
    val cardId: String? = null,
    val oracleCardId: Int? = null,
    val uid: String? = null,
    val artist: String? = null,
    val superTypes: List<String> = emptyList(),
    val subTypes: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val power: String? = null,
    val toughness: String? = null,
    val loyalty: String? = null,
    val rarity: String? = null,
    val layout: String? = null,
    val qty: Int = 1,
    val modifier: String = "Normal",
    val categories: List<String> = emptyList(),
    val typeCategory: String? = null,
    val colorLabel: ColorLabel? = null,
    val prices: CardPricesData? = null,
    val scryfallImageHash: String? = null,
    val salt: Double? = null,
    val edhrecRank: Int? = null,
    val companion: Boolean = false
) {
    val displayCardName: String
        get() = displayName ?: name

    val manaCostFormatted: String
        get() = castingCost.joinToString("") { symbol ->
            when (symbol) {
                is String -> "{$symbol}"
                is List<*> -> "{${symbol.joinToString("/")}}"  // Hybrid mana like W/U
                else -> "{$symbol}"
            }
        }

    // Flat list of mana symbols for display
    val manaCostSymbols: List<String>
        get() = castingCost.flatMap { symbol ->
            when (symbol) {
                is String -> listOf(symbol)
                is List<*> -> symbol.filterIsInstance<String>()
                else -> listOf(symbol.toString())
            }
        }

    val typeLine: String
        get() {
            val parts = mutableListOf<String>()
            if (superTypes.isNotEmpty()) parts.add(superTypes.joinToString(" "))
            if (types.isNotEmpty()) parts.add(types.joinToString(" "))
            val mainType = parts.joinToString(" ")
            return if (subTypes.isNotEmpty()) {
                "$mainType — ${subTypes.joinToString(" ")}"
            } else {
                mainType
            }
        }

    // Art crop from Archidekt CDN (for featured images)
    val imageUrl: String?
        get() {
            if (scryfallImageHash == null || setCode == null || uid == null) return null
            return "https://storage.googleapis.com/archidekt-card-images/$setCode/${uid}_art_crop.jpg"
        }

    // Small card image (146x204) - best for grid view, fast loading
    // Scryfall URL format: /front/{first_char}/{second_char}/{uuid}.jpg
    val smallImageUrl: String?
        get() {
            if (uid == null) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/small/front/$dir1/$dir2/$uid.jpg"
        }

    // Normal card image (488x680) - for detail view
    val fullImageUrl: String?
        get() {
            if (uid == null) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/normal/front/$dir1/$dir2/$uid.jpg"
        }

    // Back side image for double-faced cards
    val backImageUrl: String?
        get() {
            if (uid == null || !isDoubleFaced) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/normal/back/$dir1/$dir2/$uid.jpg"
        }

    // Small back side image for double-faced cards
    val smallBackImageUrl: String?
        get() {
            if (uid == null || !isDoubleFaced) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/small/back/$dir1/$dir2/$uid.jpg"
        }

    // Check if card is double-faced
    val isDoubleFaced: Boolean
        get() = layout in listOf("transform", "modal_dfc", "double_faced_token", "reversible_card", "flip", "art_series")

    // Get front face name (before //)
    val frontFaceName: String
        get() = if (name.contains(" // ")) name.substringBefore(" // ") else name

    // Get back face name (after //)
    val backFaceName: String?
        get() = if (name.contains(" // ")) name.substringAfter(" // ") else null

    // Get front face oracle text (before \n-----\n or full text)
    val frontFaceText: String?
        get() = text?.let {
            if (it.contains("\n-----\n")) it.substringBefore("\n-----\n") else it
        }

    // Get back face oracle text (after \n-----\n)
    val backFaceText: String?
        get() = text?.let {
            if (it.contains("\n-----\n")) it.substringAfter("\n-----\n") else null
        }
}

data class CardPricesData(
    val ck: Double? = null,
    val ckFoil: Double? = null,
    val tcg: Double? = null,
    val tcgFoil: Double? = null,
    val mtgo: Double? = null,
    val mtgoFoil: Double? = null,
    val cm: Double? = null,
    val cmFoil: Double? = null,
    val scg: Double? = null,
    val scgFoil: Double? = null,
    val mp: Double? = null,
    val mpFoil: Double? = null
) {
    val cheapest: Double?
        get() = listOfNotNull(ck, tcg, cm, scg, mp).minOrNull()
}

// UI models for grouping
data class CardGroup(
    val name: String,
    val cards: List<CardData>,
    val isExpanded: Boolean = true
) {
    val cardCount: Int get() = cards.sumOf { it.qty }
}

// Extension functions for grouping
fun Map<String, CardData>.groupByType(): List<CardGroup> {
    // Define the preferred order for card types
    val typeOrder = listOf(
        "Commander", "Creature", "Planeswalker", "Instant", "Sorcery",
        "Artifact", "Enchantment", "Land", "Battle"
    )

    return values
        .flatMap { card ->
            // Use types list for grouping, or "Other" if empty
            val cardTypes = card.types.ifEmpty { listOf("Other") }
            cardTypes.map { type -> type to card }
        }
        .groupBy({ it.first }, { it.second })
        .map { (type, cards) ->
            CardGroup(
                name = type,
                cards = cards.sortedBy { it.name }
            )
        }
        .sortedBy { group ->
            val index = typeOrder.indexOf(group.name)
            if (index >= 0) index else typeOrder.size // Unknown types at the end
        }
}

fun Map<String, CardData>.groupByCategory(): List<CardGroup> {
    return values
        .flatMap { card -> card.categories.map { category -> category to card } }
        .groupBy({ it.first }, { it.second })
        .map { (category, cards) ->
            CardGroup(
                name = category,
                cards = cards.sortedBy { it.name }
            )
        }
        .sortedBy { it.name }
}

fun Map<String, CardData>.groupByTag(colorLabels: List<ColorLabel>): List<CardGroup> {
    val tagGroups = values
        .groupBy { card -> card.colorLabel?.name ?: "" }
        .map { (tagName, cards) ->
            CardGroup(
                name = tagName.ifBlank { "Default Tag" },
                cards = cards.sortedBy { it.name }
            )
        }
        .sortedBy { if (it.name == "Default Tag") "zzz" else it.name }

    return tagGroups
}

fun Map<String, CardData>.getTagSummary(): Map<String, Int> {
    return values
        .groupBy { card -> card.colorLabel?.name ?: "" }
        .mapValues { (_, cards) -> cards.sumOf { it.qty } }
        .filterKeys { it.isNotBlank() }
}
