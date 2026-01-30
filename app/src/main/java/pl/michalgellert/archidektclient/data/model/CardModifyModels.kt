package pl.michalgellert.archidektclient.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class ModifyCardsRequest(
    val cards: List<CardModification>
)

data class CardModification(
    val action: String,
    @SerializedName("cardid")
    val cardId: String,
    val customCardId: String? = null,
    val categories: List<String>,
    val patchId: String = generatePatchId(),
    val modifications: CardModifications,
    val deckRelationId: String? = null  // Nullable - not included for "add" action
) {
    companion object {
        fun generatePatchId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

        fun modify(
            cardId: String,
            deckRelationId: String,
            categories: List<String>,
            quantity: Int = 1,
            modifier: String = "Normal",
            label: String? = null
        ) = CardModification(
            action = "modify",
            cardId = cardId,
            categories = categories,
            deckRelationId = deckRelationId,
            modifications = CardModifications(
                quantity = quantity,
                modifier = modifier,
                label = label ?: ",#656565"
            )
        )

        fun add(
            cardId: String,
            categories: List<String>,
            quantity: Int = 1,
            label: String = ",#656565"
        ) = CardModification(
            action = "add",
            cardId = cardId,
            categories = categories,
            deckRelationId = null,  // Not included in add request
            modifications = CardModifications(
                quantity = quantity,
                label = label
            )
        )

        fun remove(
            cardId: String,
            deckRelationId: String,
            categories: List<String> = emptyList()
        ) = CardModification(
            action = "remove",
            cardId = cardId,
            categories = categories,
            deckRelationId = deckRelationId,
            modifications = CardModifications()
        )
    }
}

data class CardModifications(
    val quantity: Int = 1,
    val modifier: String = "Normal",
    val customCmc: Int? = null,
    val companion: Boolean = false,
    val flippedDefault: Boolean = false,
    val label: String = ",#656565"
)

data class ModifyCardsResponse(
    val add: List<Any> = emptyList(),
    val createdCategories: List<String> = emptyList()
)

// Tag helper
data class CardTag(
    val name: String,
    val color: String
) {
    fun toLabel(): String = if (name.isBlank()) ",$color" else "$name,$color"

    companion object {
        fun fromLabel(label: String?): CardTag? {
            if (label.isNullOrBlank()) return null
            val parts = label.split(",", limit = 2)
            return CardTag(
                name = parts.getOrNull(0) ?: "",
                color = parts.getOrNull(1) ?: "#656565"
            )
        }

        // Predefiniowane kolory tagów (z Archidekt)
        val PRESET_COLORS = listOf(
            "#f47373", // czerwony
            "#f4a973", // pomarańczowy
            "#f4d373", // żółty
            "#73f473", // zielony
            "#73d3f4", // jasnoniebieski
            "#7373f4", // niebieski
            "#d373f4", // fioletowy
            "#f473d3", // różowy
            "#656565", // szary (domyślny)
            "#ffffff"  // biały
        )
    }
}
