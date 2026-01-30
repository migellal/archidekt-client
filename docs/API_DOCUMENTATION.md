# Archidekt API Documentation

Documentation based on reverse engineering (January 2026).

---

## Authorization

### Token System
- **Type:** JWT (JSON Web Token)
- **Header:** `Authorization: JWT <access_token>`
- **Access token expiry:** ~1 hour
- **Refresh token expiry:** ~40 days

### Session Cookies (Web)
```
tbJwt       - access token
tbRefresh   - refresh token
tbUser      - username
tbId        - user ID
tbTier      - subscription tier (1 = free)
tbRootFolder - root folder ID
```

---

## Endpoints

### 1. Login

```http
POST /api/rest-auth/login/
Content-Type: application/json
```

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 123456,
    "username": "username",
    "email": "user@example.com",
    "avatar": "https://storage.googleapis.com/topdekt-user/avatars/...",
    "rootFolder": 551947
  },
  "token": "..."
}
```

**Note:** Fields are named `access_token` and `refresh_token` (with underscore), not `access`/`refresh`.

---

### 2. Token Refresh

```http
POST /api/rest-auth/token/refresh/
Content-Type: application/json
```

**Request:**
```json
{
  "refresh": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
  "access": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

---

### 3. My Decks (Recently Used)

```http
GET /api/decks/curated/self-recent/
Authorization: JWT <token>
```

**Response:**
```json
{
  "results": [
    {
      "id": 19380413,
      "name": "Deck Name",
      "updatedAt": "2026-01-26 23:51:15.054276+00:00",
      "createdAt": "2026-01-26 23:51:15.041112+00:00",
      "deckFormat": 3,
      "edhBracket": null,
      "featured": "https://storage.googleapis.com/archidekt-card-images/.../art_crop.jpg",
      "customFeatured": "",
      "private": false,
      "unlisted": false,
      "viewCount": 2,
      "theorycrafted": false,
      "hasDescription": false,
      "parentFolderId": 551947,
      "owner": {
        "id": 123456,
        "username": "username",
        "avatar": "...",
        "pledgeLevel": 1
      },
      "colors": {"W": 0, "U": 0, "B": 0, "R": 0, "G": 0},
      "tags": []
    }
  ]
}
```

**Deck Formats (`deckFormat`):**
| Value | Format |
|-------|--------|
| 1 | Standard |
| 2 | Modern |
| 3 | Commander/EDH |
| 4 | Legacy |
| 5 | Vintage |
| 6 | Pauper |
| 7 | Pioneer |
| 8 | Brawl |

---

### 4. Folders

```http
GET /api/decks/folders/{folderId}/
Authorization: JWT <token>
```

**Response:**
```json
{
  "id": 1032166,
  "name": "Folder Name",
  "parentFolder": {
    "id": 551947,
    "name": "Home"
  },
  "private": false,
  "owner": {
    "id": 123456,
    "username": "username",
    "avatar": "..."
  },
  "subfolders": [
    {
      "id": 1134572,
      "name": "Subfolder",
      "createdAt": "2025-08-20T22:54:41.793071Z",
      "private": false
    }
  ],
  "decks": [
    {
      "id": 18815819,
      "name": "Deck Name",
      "size": 99,
      "updatedAt": "2026-01-29T16:55:00.844778Z",
      "deckFormat": 3,
      "featured": "https://...",
      "colors": {"W": 17, "U": 16, "B": 9, "R": 16, "G": 13},
      "parentFolderId": 1032166
    }
  ],
  "count": 6,
  "next": null
}
```

**Note:** `parentFolder` is an object `{id, name}`, NOT a number!

---

### 5. Deck Details (Next.js SSR)

Archidekt doesn't have a simple REST API for full deck details.
Data is embedded in the HTML page as `__NEXT_DATA__`.

**Fetching method:**
```
1. GET https://archidekt.com/decks/{deckId}
2. Extract <script id="__NEXT_DATA__">...</script>
3. Parse JSON
4. Data is in: props.pageProps.redux.deck
```

**`deck` structure:**
```json
{
  "id": 11386164,
  "name": "Deck Name",
  "description": "{\"ops\":[]}",
  "format": 3,
  "owner": "username",
  "ownerid": 123456,
  "ownerAvatar": "https://...",
  "updatedAt": "2026-01-27T10:49:37.110171Z",
  "createdAt": "2025-02-14T11:01:28.602261Z",
  "viewCount": 6,
  "parentFolder": 551947,

  "categories": {
    "Commander": {
      "id": 123313640,
      "name": "Commander",
      "isPremier": true,
      "includedInDeck": true,
      "includedInPrice": true
    },
    "Creatures": {...},
    "Lands": {...}
  },

  "colorLabels": [
    {"name": "", "color": "#656565"},
    {"name": "Have", "color": "#37d67a"},
    {"name": "Don't Have", "color": "#f47373"}
  ],

  "cardMap": {
    "NBPrhVn92": {
      "id": "NBPrhVn92",
      "name": "Card Name",
      "displayName": null,
      "cmc": 4,
      "castingCost": ["2", "B", "B"],
      "colorIdentity": ["Black"],
      "colors": ["Black"],
      "text": "Card text...",
      "set": "Set Name",
      "setCode": "abc",
      "uid": "b738612f-4e78-4b19-9392-6c074956d6e4",
      "types": ["Artifact", "Enchantment"],
      "subTypes": [],
      "superTypes": ["Legendary"],
      "power": "",
      "toughness": "",
      "rarity": "rare",
      "qty": 1,
      "modifier": "Normal",
      "categories": ["Recursion"],
      "colorLabel": {
        "name": "Have",
        "color": "#37d67a"
      },
      "deckRelationId": "2043883812",
      "cardId": "137434",
      "oracleCardId": 18749,
      "scryfallImageHash": "1726284925",
      "layout": "normal",
      "prices": {
        "ck": 8.99,
        "tcg": 5.18,
        "mp": 5.27
      }
    }
  }
}
```

**Double-Faced Cards:**
- `layout` field indicates card type: `"transform"`, `"modal_dfc"`, `"double_faced_token"`, `"reversible_card"`, `"flip"`, `"art_series"`
- `text` contains both sides separated by `\n-----\n`
- Back image URL uses `/back/` instead of `/front/` in Scryfall URL

---

### 6. Modify Cards in Deck

```http
PATCH /api/decks/{deckId}/modifyCards/v2/
Authorization: JWT <token>
Content-Type: application/json
```

**Request (modify category/tag/quantity):**
```json
{
  "cards": [
    {
      "action": "modify",
      "cardid": "148422",
      "customCardId": null,
      "categories": ["Creatures", "Ramp"],
      "patchId": "cNJaEBk06xLU",
      "modifications": {
        "quantity": 1,
        "modifier": "Normal",
        "customCmc": null,
        "companion": false,
        "flippedDefault": false,
        "label": "Have,#37d67a"
      },
      "deckRelationId": "2733240137"
    }
  ]
}
```

**Request (remove card):**
```json
{
  "cards": [
    {
      "action": "remove",
      "cardid": "145780",
      "customCardId": null,
      "categories": ["Land"],
      "patchId": "uoOCQF3mU08",
      "modifications": {
        "quantity": 1,
        "modifier": "Normal",
        "customCmc": null,
        "companion": false,
        "flippedDefault": false,
        "label": ",#656565"
      },
      "deckRelationId": "2498818443"
    }
  ]
}
```

**Request (add card):**
```json
{
  "cards": [
    {
      "action": "add",
      "cardid": "149186",
      "customCardId": null,
      "categories": ["Ramp"],
      "patchId": "BWI8yjp02",
      "modifications": {
        "quantity": 1,
        "modifier": "Normal",
        "customCmc": null,
        "companion": false,
        "flippedDefault": false,
        "label": ",#656565"
      }
    }
  ]
}
```

**Note:** For `add` action, do NOT include `deckRelationId`!

**Response (200 OK):**
```json
{
  "add": [
    {
      "deckRelationId": 2797185118,
      "patchId": "BWI8yjp02",
      "categories": ["Ramp"],
      "quantity": 1,
      "modifier": "Normal",
      "customCmc": null,
      "companion": false,
      "flippedDefault": false,
      "label": ",#656565",
      "cardId": "149186",
      "createdAt": "2026-01-30T13:28:25.884358+00:00"
    }
  ],
  "createdCategories": []
}
```

**Label format:**
- `"TagName,#hexColor"` - named tag
- `",#hexColor"` - unnamed tag (color only)
- `",#656565"` - default gray (no tag)

**Actions:**
| Action | Description | Requires `deckRelationId` |
|--------|-------------|---------------------------|
| `modify` | Change categories/tag/quantity | Yes |
| `add` | Add new card to deck | No |
| `remove` | Remove card from deck | Yes |

**Important fields:**
- `cardid` - Card ID (from `CardData.cardId` or search result `id`)
- `deckRelationId` - Card-deck relation (required for modify/remove)
- `patchId` - Random string (e.g., `UUID.randomUUID().toString()`)
- `categories` - List of category names (e.g., `["Creatures", "Ramp"]`)
- `label` - Tag in format `"name,#color"` or `",#656565"` for default

---

### 7. Card Search

```http
GET /api/cards/v2/?nameSearch={query}&includeTokens&includeDigital&includeEmblems&unique
Authorization: JWT <token>
```

**Response:**
```json
{
  "count": 1,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 149186,
      "artist": "Artist Name",
      "uid": "04002706-2236-4b79-bdea-4f263e43cb9c",
      "displayName": null,
      "releasedAt": "2026-01-23",
      "edition": {
        "editioncode": "ecc",
        "editionname": "Set Name",
        "editiondate": "2026-01-23",
        "editiontype": "commander"
      },
      "flavor": "Flavor text...",
      "scryfallImageHash": "1767730264",
      "oracleCard": {
        "id": 15342,
        "cmc": 1,
        "colorIdentity": [],
        "colors": [],
        "layout": "normal",
        "manaCost": "{1}",
        "name": "Sol Ring",
        "power": "",
        "toughness": "",
        "loyalty": null,
        "text": "{T}: Add {C}{C}.",
        "types": ["Artifact"],
        "subTypes": [],
        "superTypes": [],
        "keywords": [],
        "defaultCategory": "Ramp",
        "salt": 1.46,
        "edhrecRank": 1
      },
      "prices": {
        "ck": 2.29,
        "ckfoil": 0.0,
        "tcg": 0.95,
        "tcgfoil": 0.0,
        "cm": 1.22,
        "cmfoil": 0.0,
        "mp": 1.12,
        "mpfoil": 0.0
      },
      "rarity": "uncommon"
    }
  ]
}
```

**Search parameters:**
- `nameSearch` - Card name to search (URL encoded)
- `includeTokens` - Include token cards
- `includeDigital` - Include digital-only cards
- `includeEmblems` - Include emblems
- `unique` - Return only unique cards (by name)

---

### 8. Card Tokens/Emblems

```http
GET /api/cards/v2/?includeTokens&includeEmblems&includeArtCards&oracleCardIds={id1},{id2}&unique
Authorization: JWT <token>
```

**Response:**
```json
{
  "count": 2,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 148601,
      "artist": "Artist Name",
      "uid": "cddc0746-6d3e-441f-866f-28587bf54801",
      "releasedAt": "2025-11-21",
      "edition": {
        "editioncode": "ttla",
        "editionname": "Set Name Tokens"
      },
      "oracleCard": {
        "id": 48649,
        "name": "Token Name",
        "manaCost": "",
        "types": ["Creature", "Token"],
        "text": "Token text...",
        "power": "4",
        "toughness": "4"
      },
      "prices": {"ck": 0.35, "tcg": 0.0, "mp": 0.15},
      "rarity": "common"
    }
  ]
}
```

---

## Card Images

### Scryfall (Recommended)
```
https://cards.scryfall.io/{size}/front/{a}/{b}/{uuid}.jpg
```
Where:
- `{size}` = `small` (146x204), `normal` (488x680), `large` (672x936)
- `{a}` = first character of UUID
- `{b}` = second character of UUID

Example for UUID `b738612f-4e78-4b19-9392-6c074956d6e4`:
```
https://cards.scryfall.io/small/front/b/7/b738612f-4e78-4b19-9392-6c074956d6e4.jpg
https://cards.scryfall.io/normal/front/b/7/b738612f-4e78-4b19-9392-6c074956d6e4.jpg
```

**Double-faced card back:**
```
https://cards.scryfall.io/normal/back/b/7/b738612f-4e78-4b19-9392-6c074956d6e4.jpg
```

### Archidekt CDN
```
https://storage.googleapis.com/archidekt-card-images/{setCode}/{uuid}_art_crop.jpg
```

---

## Implementation Notes

1. **All authenticated requests** use `Authorization: JWT <token>` header
2. **Token refresh** - when access token expires, use refresh token to get a new one
3. **Auto-login** - save email/password in EncryptedSharedPreferences for automatic login
4. **parentFolder in folders** - it's an object `{id, name}`, not a number!
5. **Deck details** - fetch via scraping `__NEXT_DATA__`, not REST API
6. **Rate limiting** - unknown, but avoid excessive requests
7. **Card ID changes** - Archidekt generates NEW `id` for each card on every fetch. Use caching!
8. **Sideboard/Maybeboard** - cards with these categories have `includedInDeck: true`, but should be filtered by category name
9. **Category order matters** - the first category in the list determines where the card appears in the deck view

---

## Known Gotchas

### 1. Dynamic Card IDs
```
Fetch #1: cardMap["ABC123"] = { name: "Lightning Bolt", ... }
Fetch #2: cardMap["XYZ789"] = { name: "Lightning Bolt", ... }
```
IDs `ABC123` and `XYZ789` are the same card, but the ID changed!

**Solution:** Cache deck data in a singleton repository.

### 2. Label Format (Tag)
```
Correct:   "Have,#37d67a"     → Tag "Have" with color
Correct:   ",#656565"         → Default tag (no name)
Wrong:     "Have"             → Missing color - may not work
Wrong:     "#37d67a"          → Missing comma
```

### 3. Categories vs Types
- `categories` - user-defined (Ramp, Removal, Burn)
- `types` - MTG types (Creature, Artifact, Enchantment)
- Grouping "by type" uses `types`, not `categories`!

### 4. Add vs Modify Actions
- `add` - do NOT include `deckRelationId`
- `modify` and `remove` - MUST include `deckRelationId`

---

## Useful Links

- [Archidekt](https://archidekt.com)
- [Scryfall API Documentation](https://scryfall.com/docs/api)
