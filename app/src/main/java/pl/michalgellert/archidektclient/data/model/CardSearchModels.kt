package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName

data class CardSearchResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<SearchResultCard>
)

data class SearchResultCard(
    val id: Int,
    val artist: String?,
    val uid: String?,
    val displayName: String?,
    val releasedAt: String?,
    val edition: EditionInfo?,
    val flavor: String?,
    val scryfallImageHash: String?,
    val oracleCard: SearchOracleCard?,
    val prices: SearchCardPrices?,
    val rarity: String?
) {
    val name: String
        get() = oracleCard?.name ?: displayName ?: "Unknown"

    val manaCost: String
        get() = oracleCard?.manaCost ?: ""

    val types: List<String>
        get() = oracleCard?.types ?: emptyList()

    val text: String?
        get() = oracleCard?.text

    val typeLine: String
        get() {
            val superTypes = oracleCard?.superTypes ?: emptyList()
            val cardTypes = oracleCard?.types ?: emptyList()
            val subTypes = oracleCard?.subTypes ?: emptyList()

            val mainPart = (superTypes + cardTypes).joinToString(" ")
            return if (subTypes.isNotEmpty()) {
                "$mainPart — ${subTypes.joinToString(" ")}"
            } else {
                mainPart
            }
        }

    val defaultCategory: String
        get() = oracleCard?.defaultCategory ?: types.firstOrNull() ?: "Other"

    // Small card image (146x204)
    val smallImageUrl: String?
        get() {
            if (uid == null) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/small/front/$dir1/$dir2/$uid.jpg"
        }

    // Normal card image (488x680)
    val normalImageUrl: String?
        get() {
            if (uid == null) return null
            val dir1 = uid[0]
            val dir2 = uid[1]
            return "https://cards.scryfall.io/normal/front/$dir1/$dir2/$uid.jpg"
        }

    val setName: String
        get() = edition?.editionName ?: ""

    val setCode: String
        get() = edition?.editionCode ?: ""

    val cheapestPrice: Double?
        get() = listOfNotNull(prices?.tcg, prices?.ck, prices?.cm, prices?.mp).minOrNull()
}

data class EditionInfo(
    @SerializedName("editioncode")
    val editionCode: String?,
    @SerializedName("editionname")
    val editionName: String?,
    @SerializedName("editiondate")
    val editionDate: String?,
    @SerializedName("editiontype")
    val editionType: String?
)

data class SearchOracleCard(
    val id: Int,
    val cmc: Double?,
    val colorIdentity: List<String>?,
    val colors: List<String>?,
    val layout: String?,
    val manaCost: String?,
    val name: String?,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val text: String?,
    val types: List<String>?,
    val subTypes: List<String>?,
    val superTypes: List<String>?,
    val keywords: List<String>?,
    val defaultCategory: String?,
    val salt: Double?,
    val edhrecRank: Int?
)

data class SearchCardPrices(
    val ck: Double?,
    val ckfoil: Double?,
    val tcg: Double?,
    val tcgfoil: Double?,
    val cm: Double?,
    val cmfoil: Double?,
    val mp: Double?,
    val mpfoil: Double?
)
