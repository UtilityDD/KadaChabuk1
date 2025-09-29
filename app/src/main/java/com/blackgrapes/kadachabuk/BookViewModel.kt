package com.blackgrapes.kadachabuk

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application.applicationContext)

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> = _chapters

    private val _isLoading = MutableLiveData<Boolean>() // True for overall process
    val isLoading: LiveData<Boolean> = _isLoading

    // New LiveData for more specific status messages
    private val _loadingStatusMessage = MutableLiveData<String?>()
    val loadingStatusMessage: LiveData<String?> = _loadingStatusMessage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchAndLoadChapters(languageCode: String, forceDownload: Boolean = false) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _loadingStatusMessage.postValue("Preparing to load chapters for ${languageCode.uppercase()}...") // Initial message
            _error.postValue(null)
            Log.d("BookViewModel", "Starting fetch/load for $languageCode. Force download: $forceDownload")

            // You could further refine messages based on repository events if you expose Flows from it
            // For now, we'll set a general message before parsing starts.

            val inputStreamResult = repository.getChapterCsvInputStream(languageCode, forceDownload)

            inputStreamResult.fold(
                onSuccess = { csvInputStream ->
                    // InputStream is ready (either from cache or successful download)
                    Log.d("BookViewModel", "InputStream obtained for $languageCode. Preparing to parse.")
                    _loadingStatusMessage.postValue("Processing chapters for ${languageCode.uppercase()}...") // Update before parsing
                    parseCsvStream(languageCode, csvInputStream)
                },
                onFailure = { exception ->
                    Log.e("BookViewModel", "Failed to get CSV InputStream for $languageCode", exception)
                    _error.postValue("Error preparing chapter data: ${exception.localizedMessage}")
                    _chapters.postValue(emptyList())
                    _loadingStatusMessage.postValue(null) // Clear status message on error
                    _isLoading.postValue(false)
                }
            )
        }
    }

    private suspend fun parseCsvStream(languageCode: String, csvInputStream: InputStream) {
        Log.d("BookViewModel", "Starting CSV parsing for $languageCode.")
        // The message "Processing chapters..." is already set before this function is called.
        var streamClosed = false
        try {
            val startTime = System.currentTimeMillis() // For performance measurement
            val parsedChapters = withContext(Dispatchers.IO) {
                val chapterList = mutableListOf<Chapter>()
                csvReader {
                    // escapeChar = '\\' // Example: If your data contains quotes and needs escaping
                    // skipEmptyLine = true
                }.open(csvInputStream) {
                    streamClosed = true
                    var isFirstRow = true
                    var rowCount = 0
                    readAllAsSequence().forEach { row ->
                        rowCount++
                        if (isFirstRow && isHeaderRow(row)) {
                            isFirstRow = false
                            return@forEach
                        }
                        isFirstRow = false
                        if (row.size >= 6) {
                            val heading = row.getOrElse(0) { "Unknown Heading" }.trim()
                            val dateStr = row.getOrElse(1) { "" }.trim()
                            val date = if (dateStr.isNotEmpty()) dateStr else null
                            val writer = row.getOrElse(2) { "Unknown Writer" }.trim()
                            val dataText = row.getOrElse(3) { "No Data" }.trim()
                            val serial = row.getOrElse(4) { "N/A" }.trim()
                            val version = row.getOrElse(5) { "N/A" }.trim()
                            chapterList.add(Chapter(heading, date, writer, dataText, serial, version))
                        } else {
                            Log.w("BookViewModel", "Skipping malformed CSV row #$rowCount for $languageCode: $row")
                        }
                    }
                }
                val endTime = System.currentTimeMillis()
                Log.d("BookViewModel", "CSV parsing for $languageCode took ${endTime - startTime} ms. Found ${chapterList.size} chapters.")
                chapterList
            }

            if (parsedChapters.isNotEmpty()) {
                _chapters.postValue(parsedChapters)
            } else {
                if (_error.value == null) _error.postValue("No chapters found in CSV for '$languageCode'.")
                _chapters.postValue(emptyList())
            }
            // Message for successful completion (optional, as UI might just show data)
            // _loadingStatusMessage.postValue("Chapters loaded successfully!")
        } catch (e: Exception) {
            Log.e("BookViewModel", "Error parsing CSV for $languageCode", e)
            _error.postValue("Failed to parse chapters: ${e.localizedMessage}")
            _chapters.postValue(emptyList())
        } finally {
            _isLoading.postValue(false)
            _loadingStatusMessage.postValue(null) // Clear status message when done or on error
            if (!streamClosed) {
                try {
                    csvInputStream.close()
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Error closing CSV input stream in finally for $languageCode", e)
                }
            }
        }
    }

    private fun isHeaderRow(row: List<String>): Boolean {
        return row.any {
            it.trim().equals("heading", ignoreCase = true) ||
                    it.trim().equals("date", ignoreCase = true) ||
                    it.trim().equals("writer", ignoreCase = true) ||
                    it.trim().equals("serial", ignoreCase = true)
            // Add other potential header column names if needed
        }
    }

    fun onErrorShown() {
        _error.value = null
    }

    // Optional: Call this to clear the status message if it's sticky
    fun onStatusMessageShown() {
        _loadingStatusMessage.value = null
    }
}

