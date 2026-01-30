# Archidekt Mobile Client - Project Brief

## Project Goal
Create an Android mobile client for Archidekt (Magic: The Gathering deck builder) with the following features:
1. Login with automatic token refresh
2. Browse your decks with folders
3. View deck details with cards
4. Edit cards: change tags, categories, and quantity
5. Delete cards from deck
6. Add cards via search

---

## Implementation Status

### Phase 1: Reverse Engineering - COMPLETED
- WebView with HTTP interceptor to capture requests
- Captured login flow, JWT token structure
- Documented API endpoints

### Phase 2: API Client - COMPLETED
- Authorization module (login, auto-refresh tokens, EncryptedSharedPreferences)
- Archidekt API client (Retrofit)
- Data repository (singleton with cache)

### Phase 3: UI - Browsing - COMPLETED
- Login screen
- Deck list with folders (expandable/collapsible)
- Deck details view with cards
  - Header with featured image
  - Tag summary with card counts
  - Tag filtering (multi-select)
  - Card search within deck
  - View toggle (Grid/List)
  - Grouping by type (Creature, Artifact...) or tag
  - Separate Sideboard/Maybeboard section
  - Pull-to-refresh

### Phase 4: UI - Card Editing - COMPLETED
- Full-screen card edit view
- Tag change (single-select with FilterChips)
- Category change (multi-select with checkboxes, reorderable)
- Quantity change (+/- buttons)
- Card image and Oracle text preview
- Double-faced card support (both sides)
- Delete card from deck
- Add card copy

### Phase 5: Card Search & Add - COMPLETED
- FAB button to open search
- Bottom sheet with search UI
- Debounced search via Archidekt API
- Card preview with image and details
- Category selector for new cards
- Add card to deck

---

## Architecture

```
app/
├── data/
│   ├── api/
│   │   ├── ArchidektApi.kt        # Retrofit interface
│   │   ├── ApiClient.kt           # OkHttp + Retrofit setup
│   │   └── DeckDetailsService.kt  # Scraping __NEXT_DATA__ from HTML
│   ├── auth/
│   │   └── AuthManager.kt         # Singleton - JWT tokens, login, refresh
│   ├── model/
│   │   ├── AuthModels.kt          # Login request/response
│   │   ├── DeckModels.kt          # DeckSummary, DeckColors, etc.
│   │   ├── DeckDetailsModels.kt   # DeckData, CardData, ColorLabel
│   │   ├── FolderModels.kt        # FolderResponse, Subfolder
│   │   ├── CardModifyModels.kt    # CardModification, ModifyCardsRequest
│   │   └── CardSearchModels.kt    # SearchResultCard, CardSearchResponse
│   └── repository/
│       └── ArchidektRepository.kt # Singleton with deck cache
├── ui/
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── decks/
│   │   ├── DecksListScreen.kt
│   │   └── DecksListViewModel.kt
│   ├── deck/
│   │   ├── DeckDetailsScreen.kt   # Deck view with cards + search bottom sheet
│   │   └── DeckDetailsViewModel.kt
│   ├── card/
│   │   ├── CardEditScreen.kt      # Card editing (full screen)
│   │   └── CardEditViewModel.kt
│   └── theme/
│       └── Theme.kt
└── MainActivity.kt                 # Navigation (Compose Navigation)
```

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **HTTP:** Retrofit + OkHttp
- **JSON:** Gson
- **Images:** Coil 3 (with SubcomposeAsyncImage)
- **Architecture:** MVVM
- **Navigation:** Compose Navigation
- **Security:** EncryptedSharedPreferences (credentials)

---

## Key Solutions

### 1. Fetching Deck Details
Archidekt doesn't have a simple API for full deck data. Solution:
- Fetch HTML page `/decks/{deckId}`
- Extract `<script id="__NEXT_DATA__">`
- Parse JSON from `pageProps.redux.deck`
- Implementation: `DeckDetailsService.kt`

### 2. Singleton Repository with Cache
**Problem:** Archidekt generates NEW card IDs on every fetch. When CardEditScreen fetched the deck separately, card IDs didn't match.

**Solution:** `ArchidektRepository` is a singleton with cache:
```kotlin
class ArchidektRepository private constructor(...) {
    private var cachedDeckData: DeckData? = null
    private var cachedDeckId: Int? = null

    companion object {
        @Volatile
        private var INSTANCE: ArchidektRepository? = null

        fun getInstance(authManager: AuthManager): ArchidektRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ArchidektRepository(authManager).also { INSTANCE = it }
            }
        }
    }

    suspend fun getDeckData(deckId: Int, forceRefresh: Boolean = false): Result<DeckData> {
        if (!forceRefresh && cachedDeckId == deckId && cachedDeckData != null) {
            return Result.success(cachedDeckData!!)
        }
        // ... fetch and cache
    }
}
```

### 3. Authorization
- JWT tokens with auto-refresh
- Login credentials saved in EncryptedSharedPreferences
- Automatic re-login when token expires
- `AuthManager.withAuth { auth -> ... }` wrapper for requests

### 4. Card Images (Scryfall)
URL format:
```
https://cards.scryfall.io/{size}/front/{a}/{b}/{uuid}.jpg
```
Where:
- `{size}` = `small` (146x204) or `normal` (488x680)
- `{a}` = first character of UUID
- `{b}` = second character of UUID

Example: UUID `b738612f-...` → `/front/b/7/b738612f-....jpg`

For double-faced cards, back side uses `/back/` instead of `/front/`.

### 5. Card Grouping
- **By type:** `groupByType()` - groups by `card.types` (Creature, Artifact, Enchantment...)
- **By tag:** `groupByTag()` - groups by `card.colorLabel.name`
- Sideboard/Maybeboard excluded from main deck via `EXCLUDED_CATEGORIES`

### 6. Navigation with cardId
CardId may contain special characters, so we use URL encoding:
```kotlin
// Navigate to edit
val encodedCardId = URLEncoder.encode(card.id, "UTF-8")
navController.navigate("cardEdit/$deckId/$encodedCardId")

// Read in composable
val cardId = URLDecoder.decode(encodedCardId, "UTF-8")
```

### 7. Category Ordering
- Categories are stored as a `List<String>` (not Set) to preserve order
- First category determines where the card appears in the deck view
- Users can reorder categories with up/down buttons

### 8. Double-Faced Cards
- Detected by `layout` field: `transform`, `modal_dfc`, `double_faced_token`, etc.
- Oracle text contains both sides separated by `\n-----\n`
- Back image URL uses `/back/` path instead of `/front/`

### 9. Debounced Search
- Search triggers after 300ms of no typing
- Previous search job is cancelled when new input arrives
- Implemented with coroutine Job cancellation

---

## Potential Future Improvements

1. **Offline mode** - cache decks between sessions
2. **Card duplicates handling** - same `cardId` with different `deckRelationId`
3. **Deck creation** - create new decks from the app
4. **Deck statistics** - mana curve, card type distribution
5. **Price tracking** - show deck value from different sources

---

## Useful Links

- Archidekt: https://archidekt.com
- Scryfall API docs: https://scryfall.com/docs/api
- API Documentation: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
