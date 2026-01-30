package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName

data class DecksResponse(
    val results: List<DeckSummary>
)

data class DeckSummary(
    val id: Int,
    val name: String,
    val updatedAt: String,
    val deckFormat: Int,
    val edhBracket: Int? = null,
    val featured: String? = null,
    val customFeatured: String? = null,
    val private: Boolean = false,
    val unlisted: Boolean = false,
    val viewCount: Int = 0,
    val theorycrafted: Boolean = false,
    val hasDescription: Boolean = false,
    val parentFolderId: Int? = null,
    val owner: DeckOwner,
    val colors: DeckColors,
    val tags: List<String> = emptyList()
) {
    val featuredImage: String?
        get() = customFeatured?.takeIf { it.isNotBlank() } ?: featured?.takeIf { it.isNotBlank() }
}

data class DeckOwner(
    val id: Int,
    val username: String,
    val avatar: String? = null,
    val pledgeLevel: Int? = null
)

data class DeckColors(
    @SerializedName("W") val white: Int = 0,
    @SerializedName("U") val blue: Int = 0,
    @SerializedName("B") val black: Int = 0,
    @SerializedName("R") val red: Int = 0,
    @SerializedName("G") val green: Int = 0
) {
    fun toColorList(): List<String> {
        val colors = mutableListOf<String>()
        if (white > 0) colors.add("W")
        if (blue > 0) colors.add("U")
        if (black > 0) colors.add("B")
        if (red > 0) colors.add("R")
        if (green > 0) colors.add("G")
        return colors
    }
}

data class DeckDetails(
    val id: Int,
    val name: String,
    val deckFormat: Int,
    val description: String? = null,
    val featured: String? = null,
    val owner: DeckOwner,
    val cards: List<DeckCard> = emptyList(),
    val categories: List<Category> = emptyList()
)

data class DeckCard(
    val id: String,
    val card: Card,
    val quantity: Int,
    val categories: List<String>,
    val label: String? = null,
    val modifier: String = "Normal",
    val companion: Boolean = false
) {
    val tagName: String?
        get() = label?.substringBefore(",")?.takeIf { it.isNotBlank() }

    val tagColor: String?
        get() = label?.substringAfter(",")?.takeIf { it.isNotBlank() }
}

data class Card(
    val id: Int,
    val uid: String,
    val oracleCard: OracleCard,
    val edition: CardEdition,
    val prices: CardPrices? = null
)

data class OracleCard(
    val id: Int,
    val name: String,
    val manaCost: String? = null,
    val cmc: Double = 0.0,
    val types: List<String> = emptyList(),
    val text: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val colors: List<String> = emptyList(),
    val colorIdentity: List<String> = emptyList()
)

data class CardEdition(
    @SerializedName("editioncode")
    val code: String,
    @SerializedName("editionname")
    val name: String
)

data class CardPrices(
    val tcg: Double? = null,
    val ck: Double? = null,
    val mp: Double? = null
)

data class Category(
    val name: String,
    val includedInDeck: Boolean = true,
    val includedInPrice: Boolean = true,
    val isPremier: Boolean = false
)

// Format constants
object DeckFormat {
    const val STANDARD = 1
    const val MODERN = 2
    const val COMMANDER = 3
    const val LEGACY = 4
    const val VINTAGE = 5
    const val PAUPER = 6
    const val PIONEER = 7
    const val BRAWL = 8

    fun getName(format: Int): String = when (format) {
        STANDARD -> "Standard"
        MODERN -> "Modern"
        COMMANDER -> "Commander"
        LEGACY -> "Legacy"
        VINTAGE -> "Vintage"
        PAUPER -> "Pauper"
        PIONEER -> "Pioneer"
        BRAWL -> "Brawl"
        else -> "Unknown"
    }
}
