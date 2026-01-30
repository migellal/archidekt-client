package pl.michalgellert.archidektclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.michalgellert.archidektclient.ui.ApiInterceptorScreen
import pl.michalgellert.archidektclient.ui.card.CardEditScreen
import java.net.URLEncoder
import java.net.URLDecoder
import pl.michalgellert.archidektclient.ui.deck.DeckDetailsScreen
import pl.michalgellert.archidektclient.ui.decks.DecksListScreen
import pl.michalgellert.archidektclient.ui.login.LoginScreen
import pl.michalgellert.archidektclient.ui.theme.ArchidektClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchidektClientTheme {
                ArchidektApp()
            }
        }
    }
}

@Composable
fun ArchidektApp() {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("decks") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onOpenWebView = {
                        navController.navigate("webview")
                    }
                )
            }

            composable("decks") {
                DecksListScreen(
                    onDeckClick = { deckId ->
                        navController.navigate("deck/$deckId")
                    }
                )
            }

            composable(
                route = "deck/{deckId}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: return@composable
                DeckDetailsScreen(
                    deckId = deckId,
                    deckName = "",
                    onBackClick = { navController.popBackStack() },
                    onCardClick = { card ->
                        val encodedCardId = URLEncoder.encode(card.id, "UTF-8")
                        navController.navigate("cardEdit/$deckId/$encodedCardId")
                    }
                )
            }

            composable(
                route = "cardEdit/{deckId}/{cardId}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.IntType },
                    navArgument("cardId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: return@composable
                val encodedCardId = backStackEntry.arguments?.getString("cardId") ?: return@composable
                val cardId = URLDecoder.decode(encodedCardId, "UTF-8")
                CardEditScreen(
                    deckId = deckId,
                    cardId = cardId,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            composable("webview") {
                ApiInterceptorScreen()
            }
        }
    }
}
