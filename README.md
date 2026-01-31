# Archidekt Mobile Client

Unofficial Android mobile client for [Archidekt](https://archidekt.com) - a deck builder for Magic: The Gathering.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)

## Features

### Deck Management
- Browse your decks organized in folders
- View deck details with card images
- Group cards by type (Creature, Artifact...) or by custom tags
- Filter cards by tags (multi-select)
- Search cards within a deck
- Separate Sideboard/Maybeboard section
- Pull-to-refresh

### Card Editing
- Change card tags (single-select with color indicators)
- Change card categories (multi-select with ordering)
- Adjust quantity (+/-)
- View card image and Oracle text
- Support for double-faced cards (both sides displayed)
- Delete cards from deck
- Add card copies

### Card Search & Add
- Search cards via Archidekt API
- Add cards to deck with category selection
- Debounced search for smooth UX

### Authentication
- Login with automatic token refresh
- Secure credential storage (EncryptedSharedPreferences)

## Screenshots

*Coming soon*

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **HTTP Client:** Retrofit + OkHttp
- **JSON:** Gson
- **Images:** Coil 3 (with caching)
- **Architecture:** MVVM
- **Navigation:** Compose Navigation
- **Security:** EncryptedSharedPreferences

## Building

```bash
# Clone the repository
git clone https://github.com/migellal/archidekt-client.git
cd archidekt-client

# Build debug APK
./gradlew assembleDebug

# APK will be in app/build/outputs/apk/debug/
```

### Requirements
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34+
- Min SDK: 26 (Android 8.0)

## Project Structure

```
app/src/main/java/pl/michalgellert/archidektclient/
├── data/
│   ├── api/                    # Retrofit interfaces, OkHttp setup
│   ├── auth/                   # JWT token management
│   ├── model/                  # Data classes
│   └── repository/             # Data layer with caching
├── ui/
│   ├── login/                  # Login screen
│   ├── decks/                  # Deck list screen
│   ├── deck/                   # Deck details screen
│   ├── card/                   # Card edit screen
│   └── theme/                  # Material 3 theme
└── MainActivity.kt             # Navigation host
```

## Documentation

- [Project Brief](docs/ARCHIDEKT_CLIENT_BRIEF.md) - Architecture and implementation details
- [API Documentation](docs/API_DOCUMENTATION.md) - Archidekt API endpoints

## How It Works

This app is built on reverse-engineered Archidekt API. Since Archidekt doesn't provide official API documentation, the endpoints were discovered through browser DevTools and network inspection.

**Key implementation details:**
- Deck details are fetched by scraping `__NEXT_DATA__` from HTML (Archidekt uses Next.js SSR)
- Card IDs are dynamic and change on each fetch, so the app uses a singleton repository with caching
- Card images are loaded from Scryfall CDN using the card's UUID

## Disclaimer

- This is an **unofficial** application, not affiliated with Archidekt
- The API may change without notice, potentially breaking functionality
- Use at your own risk
- Please be respectful of Archidekt's servers - avoid excessive requests

## Contributing

This is a personal project, but contributions are welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

## Credits

**Development assistance:** This project was developed with assistance from [Claude](https://claude.ai) (Anthropic's AI assistant), which helped with:
- Reverse engineering the Archidekt API
- Implementing Jetpack Compose UI components
- Designing the MVVM architecture
- Debugging and problem-solving

## License

MIT License - see [LICENSE](LICENSE) for details.

---

*Built with ❤️ for the Magic: The Gathering community*
