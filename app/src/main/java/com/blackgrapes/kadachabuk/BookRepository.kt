package com.blackgrapes.kadachabuk

import android.content.Context
import android.util.Log
// Imports for Room database components
import com.blackgrapes.kadachabuk.AppDatabase // Ensure this path is correct
// import com.blackgrapes.kadachabuk.ChapterDao // DAO is accessed via AppDatabase instance
// import com.blackgrapes.kadachabuk.Chapter // Your Chapter entity
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader // For parsing CSV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// Base part of the Google Sheet publish URL (before gid)
private const val GOOGLE_SHEET_BASE_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub"
// Suffix part of the Google Sheet publish URL (after gid)
private const val GOOGLE_SHEET_URL_SUFFIX = "&single=true&output=csv"
private const val ABOUT_GID = "1925993700"

// --- PREFERENCES KEYS ---
private const val ABOUT_INFO_PREFS = "AboutInfoPrefs"
private const val VERSION_INFO_PREFS = "VersionInfoPrefs"
// GID for the 'versions' sheet
private const val VERSIONS_GID = "1804189470"

// Map language codes to their respective GID
private val languageToGidMap = mapOf(
    "bn" to "0",          // Bengali
    "hi" to "1815418271", // Hindi
    "en" to "680564409",  // English
    "as" to "1703268117", // Assamese
    "od" to "217039634",  // Odia
    "tm" to "1124108521"  // Tamil
)

class BookRepository(private val context: Context) {

    // Get an instance of the DAO from the AppDatabase
    private val chapterDao = AppDatabase.getDatabase(context).chapterDao()

    // --- Temporary CSV File handling for download and parse ---
    private fun getTemporaryCsvFile(languageCode: String): File {
        val dir = File(context.cacheDir, "csv_temp_downloads") // Use cacheDir for temp files
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "chapters_temp_${languageCode.lowercase()}.csv")
    }

    private fun getCsvUrlForLanguage(languageCode: String): URL? {
        val gid = languageToGidMap[languageCode.lowercase()] ?: run {
            Log.e("BookRepository", "No GID found for language code: $languageCode")
            return null
        }
        val urlString = "$GOOGLE_SHEET_BASE_URL?gid=$gid$GOOGLE_SHEET_URL_SUFFIX"
        return try {
            URL(urlString)
        } catch (e: Exception) {
            Log.e("BookRepository", "Malformed URL for $languageCode: $urlString", e)
            null
        }
    }

    private fun getVersionsSheetUrl(): URL? {
        val urlString = "$GOOGLE_SHEET_BASE_URL?gid=$VERSIONS_GID$GOOGLE_SHEET_URL_SUFFIX"
        return try {
            URL(urlString)
        } catch (e: Exception) {
            Log.e("BookRepository", "Malformed URL for versions sheet: $urlString", e)
            null
        }
    }

    private fun getAboutSheetUrl(): URL? {
        val urlString = "$GOOGLE_SHEET_BASE_URL?gid=$ABOUT_GID$GOOGLE_SHEET_URL_SUFFIX"
        return try {
            URL(urlString)
        } catch (e: Exception) {
            Log.e("BookRepository", "Malformed URL for about sheet: $urlString", e)
            null
        }
    }

    /**
     * Fetches chapters for the given language.
     * Checks the local Room database first. If data is found and forceRefresh is false,
     * it returns the cached data. Otherwise, it downloads the CSV, parses it,
     * updates the database, and then returns the new data.
     */
    suspend fun getChaptersForLanguage(
        languageCode: String,
        forceRefreshFromServer: Boolean = false,
        onChapterParsed: (Chapter) -> Unit = {} // Callback for progress
    ): Result<List<Chapter>> {
        return withContext(Dispatchers.IO) {
            try {
                val versionPrefs = context.getSharedPreferences(VERSION_INFO_PREFS, Context.MODE_PRIVATE)
                val localVersion = versionPrefs.getString("version_$languageCode", "0.0") ?: "0.0"
                val remoteVersionResult = getRemoteMasterVersion(languageCode)
                val remoteVersion = remoteVersionResult.getOrNull()

                // Decide if a download is needed
                val needsDownload = if (forceRefreshFromServer) {
                    Log.i("BookRepository", "Force refresh triggered for $languageCode.")
                    true
                } else if (remoteVersion == null) {
                    Log.w("BookRepository", "Could not fetch remote version for $languageCode. Will rely on local DB if available.")
                    false // Cannot compare, so don't download.
                } else {
                    (remoteVersion.toFloatOrNull() ?: 0.0f) > (localVersion.toFloatOrNull() ?: 0.0f)
                }

                if (!needsDownload && chapterDao.getChapterCountForLanguage(languageCode) > 0) {
                    Log.i("BookRepository", "DB CACHE HIT for $languageCode (v$localVersion). Loading from database.")
                    // FIX: Sort the results from the database to maintain sequence.
                    // We sort by the 'serial' field, treating it as a number.
                    val chaptersFromDb = chapterDao.getChaptersByLanguage(languageCode)
                    val sortedChapters = chaptersFromDb.sortedBy { it.serial.toIntOrNull() ?: Int.MAX_VALUE }                    
                    return@withContext Result.success(sortedChapters)
                } else {
                    if (forceRefreshFromServer) {
                        Log.i("BookRepository", "FORCE REFRESH requested for $languageCode. Fetching from server.")
                    } else {
                        Log.i("BookRepository", "DB CACHE MISS for $languageCode. Fetching from server.")
                    }

                    if (needsDownload && remoteVersion != null) {
                        Log.i("BookRepository", "New version available for $languageCode. Remote: v$remoteVersion, Local: v$localVersion. Downloading...")
                    }

                    // Download the CSV to a temporary file
                    val downloadResult = downloadCsvToTempFile(languageCode)

                    downloadResult.fold(
                        onSuccess = { tempCsvFileStream ->
                            Log.d("BookRepository", "CSV downloaded to temp file for $languageCode. Parsing and updating DB.")
                            // Parse the CSV stream from the temporary file
                            val parseResult = parseCsvStreamInternal(languageCode, tempCsvFileStream, onChapterParsed)

                            // Clean up: Close the stream and delete the temp file
                            try { tempCsvFileStream.close() } catch (e: Exception) { Log.e("BookRepository", "Error closing temp file stream", e) }
                            getTemporaryCsvFile(languageCode).delete()

                            parseResult.fold(
                                onSuccess = { parsedChaptersWithoutLang ->
                                    // Add languageCode to each Chapter object before DB insert
                                    val chaptersToStoreInDb = parsedChaptersWithoutLang.map { chapter ->
                                        // Ensure your Chapter data class has 'languageCode' as a var
                                        // or use .copy() if it's a val and the constructor allows it.
                                        chapter.copy(languageCode = languageCode)
                                    }

                                    // Replace existing chapters for this language in the DB
                                    chapterDao.replaceChaptersForLanguage(languageCode, chaptersToStoreInDb)
                                    Log.i("BookRepository", "Database updated for $languageCode with ${chaptersToStoreInDb.size} chapters.")

                                    // On successful update, save the new master version
                                    if (remoteVersion != null) {
                                        versionPrefs.edit().putString("version_$languageCode", remoteVersion).apply()
                                    }
                                    Result.success(chaptersToStoreInDb) // Return the newly parsed and saved chapters
                                },
                                onFailure = { parsingException ->
                                    Log.e("BookRepository", "Failed to parse CSV for $languageCode after download.", parsingException)
                                    Result.failure(parsingException)
                                }
                            )
                        },
                        onFailure = { downloadException ->
                            Log.e("BookRepository", "Failed to download CSV for $languageCode.", downloadException)
                            // If download fails, but we have old data, return the old data to prevent a blank screen.
                            val chaptersFromDb = chapterDao.getChaptersByLanguage(languageCode)
                            if (chaptersFromDb.isNotEmpty()) {
                                Log.w("BookRepository", "Download failed, but returning stale data from DB to avoid blank screen.")
                                return@fold Result.success(chaptersFromDb.sortedBy { it.serial.toIntOrNull() ?: Int.MAX_VALUE })
                            }
                            Result.failure(downloadException)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Error in getChaptersForLanguage for $languageCode", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches the master version string for a given language from the 'versions' sheet.
     */
    private suspend fun getRemoteMasterVersion(languageCode: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val url = getVersionsSheetUrl() ?: return@withContext Result.failure(Exception("Could not create URL for versions sheet"))
            var connection: HttpURLConnection? = null
            try {
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // Use a shorter timeout for this small file
                connection.readTimeout = 5000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val version = csvReader { skipEmptyLine = true }.open(connection.inputStream) {
                        readAllAsSequence()
                            .drop(1) // Skip header
                            .firstOrNull { row -> row.size >= 2 && row[0].trim().equals(languageCode, ignoreCase = true) }
                            ?.get(1)?.trim()
                    }
                    if (version != null) {
                        Result.success(version)
                    } else {
                        Result.failure(Exception("Version not found for language '$languageCode' in versions sheet."))
                    }
                } else {
                    Result.failure(Exception("Failed to download versions sheet: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Error fetching remote master version", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Downloads the CSV for the given language to a temporary local file.
     * Returns a Result containing an InputStream to the temporary file.
     * The caller is responsible for closing this InputStream and deleting the temp file.
     */
    private suspend fun downloadCsvToTempFile(languageCode: String): Result<InputStream> {
        val downloadUrl = getCsvUrlForLanguage(languageCode)
            ?: return Result.failure(IllegalArgumentException("Could not construct URL for language: $languageCode"))

        Log.d("BookRepository", "Downloading CSV for $languageCode from: $downloadUrl to a temporary file.")
        val tempFile = getTemporaryCsvFile(languageCode)
        var connection: HttpURLConnection? = null

        try {
            connection = downloadUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                var totalBytesRead = 0L
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                        }
                        output.flush()
                    }
                }
                if (totalBytesRead > 0) {
                    Log.i("BookRepository", "CSV downloaded ($totalBytesRead bytes) to temp file: ${tempFile.absolutePath}")
                    return Result.success(FileInputStream(tempFile))
                } else {
                    Log.w("BookRepository", "Downloaded CSV for $languageCode was empty.")
                    tempFile.delete()
                    return Result.failure(Exception("Downloaded CSV for $languageCode was empty."))
                }
            } else {
                Log.e("BookRepository", "Download failed: $responseCode ${connection.responseMessage}")
                if(tempFile.exists()) tempFile.delete()
                return Result.failure(Exception("Download failed: $responseCode ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Exception during CSV download to temp file", e)
            if(tempFile.exists()) tempFile.delete()
            return Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Internal CSV parsing logic.
     * Parses the given InputStream and returns a list of Chapter objects (without languageCode set).
     */
    private suspend fun parseCsvStreamInternal(
        languageCodeForLog: String,
        csvInputStream: InputStream,
        onChapterParsed: (Chapter) -> Unit // Accept the callback
    ): Result<List<Chapter>> {
        return try {
            Log.d("BookRepository", "Starting internal CSV parsing for $languageCodeForLog.")
            val parsedList = withContext(Dispatchers.IO) {
                val chapterList = mutableListOf<Chapter>()
                val isHeaderRow = { row: List<String> ->
                    row.any {
                        it.trim().equals("heading", ignoreCase = true) ||
                                it.trim().equals("date", ignoreCase = true) ||
                                it.trim().equals("writer", ignoreCase = true) ||
                                it.trim().equals("serial", ignoreCase = true)
                    }
                }

                csvReader { skipEmptyLine = true }.open(csvInputStream) { // csvInputStream will be closed by this block
                    readAllAsSequence().forEachIndexed forEach@{ index, row ->
                        // FIX: Simplify header check. Only skip the very first row if it's a header.
                        if (index == 0 && isHeaderRow(row)) {
                            return@forEach // Skip header row
                        }
                        // ...
                        // ...
                        // ...
                        if (row.size >= 6) {
                            val chapter = Chapter(
                                // id and languageCode are handled elsewhere or by Room's defaults
                                heading = row.getOrElse(0) { "Unknown Heading" }.trim(),
                                date = row.getOrElse(1) { "" }.trim().let { if (it.isNotEmpty()) it else null },
                                writer = row.getOrElse(2) { "Unknown Writer" }.trim(),
                                dataText = row.getOrElse(3) { "No Data" }.trim(), // Ensure this line is present and correct
                                serial = row.getOrElse(4) { "N/A" }.trim(),
                                version = row.getOrElse(5) { "N/A" }.trim()
                            )
                            chapterList.add(chapter)
                            onChapterParsed(chapter) // Invoke the callback with the newly parsed chapter
                        } else {
                            Log.w("BookRepository", "Skipping malformed CSV row for $languageCodeForLog (expected at least 6 columns, found ${row.size})")
                        }
                        // ...

                        // ...

                        // ...

                    }
                }
                chapterList
            }
            Log.i("BookRepository", "Internal CSV parsing complete for $languageCodeForLog. Found ${parsedList.size} chapters.")
            Result.success(parsedList)
        } catch (e: Exception) {
            Log.e("BookRepository", "Error during internal CSV parsing for $languageCodeForLog", e)
            Result.failure(e)
        }
    }

    suspend fun getAboutInfo(languageCode: String, forceRefresh: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(ABOUT_INFO_PREFS, Context.MODE_PRIVATE)
            val cacheKey = "about_$languageCode"

            if (!forceRefresh) {
                val cachedInfo = prefs.getString(cacheKey, null)
                if (cachedInfo != null) {
                    Log.d("BookRepository", "CACHE HIT for about info: $languageCode")
                    return@withContext Result.success(cachedInfo)
                }
            }

            Log.d("BookRepository", "CACHE MISS for about info: $languageCode. Fetching from network.")
            try {
                val url = getAboutSheetUrl() ?: return@withContext Result.failure(Exception("Could not create URL for about sheet"))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val aboutText = parseAboutCsv(connection.inputStream, languageCode)
                    if (aboutText != null) {
                        // Save to cache before returning
                        prefs.edit().putString(cacheKey, aboutText).apply()
                        Log.d("BookRepository", "Saved about info for $languageCode to cache.")
                        Result.success(aboutText)
                    } else {
                        Result.failure(Exception("About info not found for language: $languageCode"))
                    }
                } else {
                    Result.failure(Exception("Failed to download about info: ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Error fetching about info", e)
                Result.failure(e)
            }
        }
    }

    private fun parseAboutCsv(inputStream: InputStream, languageCode: String): String? {
        return csvReader { skipEmptyLine = true }.open(inputStream) {
            readAllAsSequence()
                .drop(1) // Skip header row
                .firstOrNull { row ->
                    // Find the first row where the language code matches
                    row.size >= 2 && row[0].trim().equals(languageCode, ignoreCase = true)
                }
                ?.get(1) // If a row is found, get the second column (the "about" text)
                ?.trim() // And trim it
        }
    }

    // The old getChapterCsvInputStream and downloadAndSaveCsv can now be removed
    // if getChaptersForLanguage is the sole entry point for fetching chapter data.
    // If you need to keep them for other purposes, you can, but they are not used
    // by the getChaptersForLanguage method above.
}
