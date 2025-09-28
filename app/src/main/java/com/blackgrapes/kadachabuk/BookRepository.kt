package com.blackgrapes.kadachabuk

import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer // Required for List<Chapter>
import java.io.File
import java.io.IOException

class BookRepository(private val context: Context) { // Pass context for file operations

    private val apiService = RetrofitInstance.api
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true } // Configure Json

    private val sheetUrls = mapOf(
        "bn" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=0&single=true&output=csv",
        "hi" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1815418271&single=true&output=csv",
        // ... other language URLs
        "en" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=680564409&single=true&output=csv",
        "as" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1703268117&single=true&output=csv",
        "od" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=217039634&single=true&output=csv",
        "tm" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1124108521&single=true&output=csv"
    )

    private fun getCacheFile(languageCode: String): File {
        // Use app's cache directory. You could also use internal storage.
        return File(context.cacheDir, "chapters_${languageCode}.json")
    }

    private fun saveChaptersToCache(languageCode: String, chapters: List<Chapter>) {
        val cacheFile = getCacheFile(languageCode)
        try {
            val jsonString = json.encodeToString(ListSerializer(Chapter.serializer()), chapters)
            cacheFile.writeText(jsonString)
            Log.i("BookRepository", "Chapters for '$languageCode' saved to cache.")
        } catch (e: Exception) { // Catch broader exceptions for serialization/IO
            Log.e("BookRepository", "Error saving chapters to cache for $languageCode: ${e.message}", e)
        }
    }

    private fun loadChaptersFromCache(languageCode: String): List<Chapter>? {
        val cacheFile = getCacheFile(languageCode)
        if (!cacheFile.exists()) {
            Log.i("BookRepository", "Cache file not found for '$languageCode'.")
            return null
        }
        return try {
            val jsonString = cacheFile.readText()
            val chapters = json.decodeFromString(ListSerializer(Chapter.serializer()), jsonString)
            Log.i("BookRepository", "Chapters for '$languageCode' loaded from cache.")
            chapters
        } catch (e: Exception) {
            Log.e("BookRepository", "Error reading chapters from cache for $languageCode: ${e.message}", e)
            // Optional: Delete corrupted cache file
            // cacheFile.delete()
            null
        }
    }

    suspend fun getChapters(languageCode: String): List<Chapter> {
        // 1. Try to load from cache
        val cachedChapters = loadChaptersFromCache(languageCode)
        if (cachedChapters != null) {
            // Later, you might add logic here to check if cache is stale
            // For now, if cache exists, return it.
            return cachedChapters
        }

        // 2. If not in cache (or cache load failed), fetch from network
        Log.i("BookRepository", "Cache miss for '$languageCode'. Fetching from network.")
        val url = sheetUrls[languageCode] ?: run {
            Log.w("BookRepository", "No URL found for language code: $languageCode")
            return emptyList()
        }
        try {
            val response = apiService.getBookCsvData(url)
            if (response.isSuccessful && response.body() != null) {
                val fetchedChapters = parseCsvWithKotlinCsv(response.body()!!)
                if (fetchedChapters.isNotEmpty()) {
                    // 3. Save fetched chapters to cache for next time
                    saveChaptersToCache(languageCode, fetchedChapters)
                }
                return fetchedChapters
            } else {
                Log.e("BookRepository", "Error fetching network data for $languageCode: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Exception fetching network data for $languageCode: ${e.message}", e)
        }
        return emptyList() // Return empty list if network fetch also fails
    }

    // parseCsvWithKotlinCsv remains the same as your previous version that uses kotlin-csv
    private fun parseCsvWithKotlinCsv(csvString: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        if (csvString.isBlank()) {
            Log.w("BookRepository", "CSV string is blank, cannot parse.")
            return emptyList()
        }
        try {
            val rows: List<Map<String, String>> = csvReader {
                // Configuration options
            }.readAllWithHeader(csvString)

            if (rows.isEmpty()) {
                Log.w("BookRepository", "kotlin-csv parsed with zero data rows (after header).")
            }
            rows.forEachIndexed { index, row ->
                val heading = row["heading"]?.trim()
                val dateStr = row["date"]?.trim()
                val writer = row["writer"]?.trim()
                val dataContent = row["data"]?.trim()
                val serial = row["serial"]?.trim()
                val version = row["version"]?.trim()

                if (heading.isNullOrBlank()) {
                    Log.w("BookRepository", "Skipping row ${index + 1} due to missing or blank 'heading'. Row data: $row")
                    return@forEachIndexed
                }
                if (writer.isNullOrBlank()) {
                    Log.w("BookRepository", "Skipping row ${index + 1} (Heading: '$heading') due to missing or blank 'writer'. Row data: $row")
                    return@forEachIndexed
                }
                // Optional: Log if other fields are blank but still proceed
                if (dataContent.isNullOrBlank()) Log.v("BookRepository", "Row ${index + 1} (Heading: '$heading') has blank 'data'.")
                if (serial.isNullOrBlank()) Log.v("BookRepository", "Row ${index + 1} (Heading: '$heading') has blank 'serial'.")
                if (version.isNullOrBlank()) Log.v("BookRepository", "Row ${index + 1} (Heading: '$heading') has blank 'version'.")


                val date = if (dateStr.isNullOrBlank()) null else dateStr
                chapters.add(
                    Chapter(
                        heading = heading,
                        date = date,
                        writer = writer,
                        data = dataContent ?: "",
                        serial = serial ?: "",
                        version = version ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error parsing CSV with kotlin-csv: ${e.message}", e)
        }
        if (chapters.isEmpty() && csvString.split("\n").getOrElse(1) { "" }.isNotBlank()) {
            Log.w("BookRepository", "kotlin-csv parsing resulted in zero chapters, but data lines seemed to exist. Check CSV column names and content.")
        }
        return chapters
    }
}
