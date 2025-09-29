package com.blackgrapes.kadachabuk

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow // If you want to implement progress later
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

    // Example for download progress (optional, but good for UX)
    // val downloadProgress = MutableStateFlow<Int?>(null)
    // val downloadStatusMessage = MutableStateFlow<String?>(null)

    private fun getLocalCsvFile(languageCode: String): File {
        val dir = File(context.filesDir, "csv_files")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "chapters_${languageCode.lowercase()}.csv")
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

    suspend fun getChapterCsvInputStream(languageCode: String, forceDownload: Boolean = false): Result<InputStream> {
        return withContext(Dispatchers.IO) {
            val localFile = getLocalCsvFile(languageCode)

            // --- Enhanced Logging for Cache Check ---
            Log.d("BookRepository", ">>> Checking cache for $languageCode. Path: ${localFile.absolutePath}")
            Log.d("BookRepository", ">>> forceDownload: $forceDownload")
            Log.d("BookRepository", ">>> localFile.exists(): ${localFile.exists()}")
            if (localFile.exists()) {
                Log.d("BookRepository", ">>> localFile.length(): ${localFile.length()}")
            }
            // --- End Enhanced Logging ---

            if (!forceDownload && localFile.exists() && localFile.length() > 0) {
                Log.i("BookRepository", ">>> CACHE HIT for $languageCode: Using local file.")
                try {
                    Result.success(FileInputStream(localFile))
                } catch (e: Exception) {
                    Log.e("BookRepository", "Error opening local CSV for $languageCode", e)
                    Log.w("BookRepository", "Falling back to download for $languageCode due to error opening local file.")
                    downloadAndSaveCsv(languageCode, localFile)
                }
            } else {
                Log.w("BookRepository", ">>> CACHE MISS for $languageCode. Reason(s):")
                if (forceDownload) Log.w("BookRepository", " - Force download was true.")
                if (!localFile.exists()) Log.w("BookRepository", " - Local file does not exist.")
                if (localFile.exists() && localFile.length() == 0L) Log.w("BookRepository", " - Local file exists but is empty.")
                Log.d("BookRepository", ">>> Proceeding to download for $languageCode.")
                // downloadStatusMessage.value = "Downloading ${languageCode.uppercase()} chapters..."
                // downloadProgress.value = 0
                downloadAndSaveCsv(languageCode, localFile)
            }
        }
    }

    private suspend fun downloadAndSaveCsv(languageCode: String, targetFile: File): Result<InputStream> {
        val downloadUrl = getCsvUrlForLanguage(languageCode)
            ?: return Result.failure(IllegalArgumentException("Could not construct URL for language: $languageCode"))

        Log.d("BookRepository", "Attempting to download CSV for $languageCode from: $downloadUrl")
        // downloadStatusMessage.value = "Downloading ${languageCode.uppercase()} chapters..."

        var successfullyDownloadedAndSaved = false
        var connection: HttpURLConnection? = null

        // Ensure target file doesn't exist in a partially downloaded state from a previous failed attempt within this function call
        if (targetFile.exists()) {
            // More controlled deletion: only delete if we are about to overwrite it anyway.
            // If a previous call to this function failed and left a 0-byte file, this is fine.
            // The main concern is if this function itself creates a 0-byte file due to error.
        }

        try {
            connection = downloadUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000 // 20 seconds
            connection.readTimeout = 20000  // 20 seconds
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            Log.d("BookRepository", "Download response code for $languageCode: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                var totalBytesRead = 0L
                connection.inputStream.use { input ->
                    // FileOutputStream will create the file if it doesn't exist, or overwrite if it does.
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                        }
                        output.flush() // Ensure all data is written to the file system
                    }
                } // input stream is closed here

                if (totalBytesRead > 0) {
                    successfullyDownloadedAndSaved = true
                    Log.i("BookRepository", "CSV downloaded ($totalBytesRead bytes) and saved for $languageCode to ${targetFile.absolutePath}")
                } else {
                    Log.w("BookRepository", "Download for $languageCode completed with HTTP_OK, but 0 bytes were written to file.")
                    // successfullyDownloadedAndSaved remains false
                }
            } else { // HTTP Code was not OK
                Log.e("BookRepository", "Download failed for $languageCode. Server returned: $responseCode ${connection.responseMessage}")
                // successfullyDownloadedAndSaved remains false
            }

            // After attempting download and stream operations:
            if (!successfullyDownloadedAndSaved && targetFile.exists()) {
                Log.w("BookRepository", "Deleting unsuccessful/empty download file for $languageCode: ${targetFile.name}")
                targetFile.delete()
            }

            return if (successfullyDownloadedAndSaved) {
                // downloadStatusMessage.value = "Download complete."
                // downloadProgress.value = 100
                Result.success(FileInputStream(targetFile))
            } else {
                val errorMsg = "Download was not successful for $languageCode (HTTP: $responseCode, Message: ${connection?.responseMessage}). File not cached or is empty."
                Log.e("BookRepository", errorMsg)
                // downloadStatusMessage.value = "Download issue: ${connection?.responseMessage ?: "Unknown"}"
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            Log.e("BookRepository", "Exception during download process for $languageCode", e)
            if (targetFile.exists()) { // Clean up on any exception during the process
                Log.w("BookRepository", "Deleting file after exception during download for $languageCode: ${targetFile.name}")
                targetFile.delete()
            }
            // downloadStatusMessage.value = "Download error: ${e.message}"
            return Result.failure(e)
        } finally {
            connection?.disconnect()
            // downloadProgress.value = null // Reset progress
        }
    }

    // This method is less relevant if the ViewModel is responsible for parsing.
    /*
    suspend fun getChapters(languageCode: String, forceDownload: Boolean = false): Result<List<Chapter>> {
        return getChapterCsvInputStream(languageCode, forceDownload).mapCatching { inputStream ->
            // Move your CSV parsing logic here if you want Repository to return List<Chapter>
            // For example:
            // val parsedChapters = withContext(Dispatchers.IO) { parseCsvStreamInternal(inputStream) }
            // parsedChapters
            emptyList() // Placeholder
        }
    }
    */
}
