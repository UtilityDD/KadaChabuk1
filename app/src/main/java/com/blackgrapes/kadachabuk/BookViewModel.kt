package com.blackgrapes.kadachabuk

import android.app.Application
import android.util.Log
// import androidx.glance.layout.size // This import seems unused
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application.applicationContext)

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> = _chapters

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loadingStatusMessage = MutableLiveData<String?>()
    val loadingStatusMessage: LiveData<String?> = _loadingStatusMessage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _downloadingChaptersList = MutableLiveData<List<ChapterDownloadStatus>>()
    val downloadingChaptersList: LiveData<List<ChapterDownloadStatus>> = _downloadingChaptersList

    fun fetchAndLoadChapters(
        languageCode: String,
        languageName: String,
        forceDownload: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _downloadingChaptersList.postValue(emptyList()) // Clear previous list
            _loadingStatusMessage.postValue("Loading chapters for $languageName...")
            _error.postValue(null)
            Log.d("BookViewModel", "Requesting chapters for $languageName ($languageCode). Force refresh: $forceDownload")

            val result = repository.getChaptersForLanguage(languageCode, forceDownload) { parsedChapter ->
                // This block is the callback, executed for each parsed chapter
                val currentList = _downloadingChaptersList.value ?: emptyList()
                val newItem = ChapterDownloadStatus(heading = parsedChapter.heading, isDownloaded = true)
                _downloadingChaptersList.postValue(currentList + newItem)
            }

            result.fold(
                onSuccess = { loadedChapters -> // Renamed to avoid confusion with the LiveData
                    Log.i("BookViewModel", "Successfully loaded ${loadedChapters.size} chapters for $languageCode from repository.")
                    if (loadedChapters.isNotEmpty()) {
                        _chapters.postValue(loadedChapters)
                        _loadingStatusMessage.postValue("Chapters loaded.")
                    } else {
                        _chapters.postValue(emptyList())
                        if (_error.value == null) {
                            _loadingStatusMessage.postValue("No chapters found for ${languageCode.uppercase()}.")
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e("BookViewModel", "Failed to load chapters for $languageCode from repository", exception)
                    _error.postValue("Error loading chapters: ${exception.localizedMessage}")
                    _chapters.postValue(emptyList())
                    _loadingStatusMessage.postValue(null)
                }
            )
            _isLoading.postValue(false)
        }
    }

    fun onErrorShown() {
        _error.value = null
    }

    fun onStatusMessageShown() {
        _loadingStatusMessage.value = null
    }
}
