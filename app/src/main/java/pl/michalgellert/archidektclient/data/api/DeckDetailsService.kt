package pl.michalgellert.archidektclient.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.michalgellert.archidektclient.data.model.DeckData
import pl.michalgellert.archidektclient.data.model.DeckDetailsResponse

/**
 * Service to fetch deck details by scraping the Next.js data from the page
 */
class DeckDetailsService(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "DeckDetailsService"
    }

    suspend fun getDeckDetails(deckId: Int, authToken: String): Result<DeckData> {
        return withContext(Dispatchers.IO) {
            try {
                // First, try to get the deck page HTML
                val pageUrl = "https://archidekt.com/decks/$deckId"
                val request = Request.Builder()
                    .url(pageUrl)
                    .addHeader("Authorization", "JWT $authToken")
                    .addHeader("Cookie", "tbJwt=$authToken")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch deck page: ${response.code}")
                    return@withContext Result.failure(Exception("Failed to fetch deck: ${response.code}"))
                }

                val html = response.body?.string() ?: ""
                response.close()

                // Extract __NEXT_DATA__ from HTML
                val nextDataPattern = """<script id="__NEXT_DATA__" type="application/json">(.+?)</script>""".toRegex()
                val match = nextDataPattern.find(html)

                if (match == null) {
                    Log.e(TAG, "Could not find __NEXT_DATA__ in page")
                    return@withContext Result.failure(Exception("Could not parse deck data"))
                }

                val jsonString = match.groupValues[1]
                Log.d(TAG, "Found __NEXT_DATA__, parsing JSON (${jsonString.length} chars)")

                // Parse JSON
                val jsonElement = JsonParser.parseString(jsonString)
                val pageProps = jsonElement.asJsonObject
                    .getAsJsonObject("props")
                    .getAsJsonObject("pageProps")
                    .getAsJsonObject("redux")
                    .getAsJsonObject("deck")

                val deckData = gson.fromJson(pageProps, DeckData::class.java)
                Log.d(TAG, "Parsed deck: ${deckData.name}, ${deckData.cardMap.size} cards")

                Result.success(deckData)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching deck details", e)
                Result.failure(e)
            }
        }
    }
}
