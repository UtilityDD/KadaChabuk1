package com.blackgrapes.kadachabuk

import android.app.Application
import android.util.Log
// import androidx.glance.layout.size // This import seems unused
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
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

    private val _showInitialLoadMessage = MutableLiveData<Boolean>()
    val showInitialLoadMessage: LiveData<Boolean> = _showInitialLoadMessage

    private val _aboutInfo = MutableLiveData<Result<String>>()
    val aboutInfo: LiveData<Result<String>> = _aboutInfo

    var hasShownInitialAboutDialog = false
    val isFetchingAboutForDialog = MutableLiveData<Boolean>(false)

    fun fetchAndLoadChapters(
        languageCode: String,
        languageName: String,
        forceDownload: Boolean = false
    ) {
        viewModelScope.launch {
            // Try to load from DB first without showing a loading screen.
            val chaptersFromDb = repository.getChaptersFromDb(languageCode)
            val needsInitialLoadingScreen = chaptersFromDb.isNullOrEmpty() || forceDownload

            if (chaptersFromDb != null && !forceDownload) {
                _chapters.postValue(chaptersFromDb)
                Log.d("BookViewModel", "Instantly loaded ${chaptersFromDb.size} chapters from DB for $languageCode.")
                // Now, check for updates silently in the background.
            }

            if (needsInitialLoadingScreen) {
                _isLoading.postValue(true)
                _showInitialLoadMessage.postValue(true) // Signal to show the message
                _downloadingChaptersList.postValue(emptyList()) // Clear previous list
                _loadingStatusMessage.postValue("Preparing download...")
                _error.postValue(null)
                Log.d("BookViewModel", "Showing loading screen for $languageName ($languageCode). Force refresh: $forceDownload")
            }

            // This will run for both initial load and silent background updates.
            viewModelScope.launch {
                val result = repository.getChaptersForLanguage(
                    languageCode = languageCode,
                    forceRefreshFromServer = forceDownload
                ) { progress: DownloadProgress ->
                    // This callback is now correctly executed for each parsed chapter
                    viewModelScope.launch(Dispatchers.Main) {
                        val newItem = ChapterDownloadStatus(heading = progress.chapter.heading, isDownloaded = true)
                        _downloadingChaptersList.value = (_downloadingChaptersList.value ?: emptyList()) + newItem
                    }
                }

                result.fold(
                    onSuccess = { loadedChapters ->
                        Log.i("BookViewModel", "Successfully loaded/updated ${loadedChapters.size} chapters for $languageCode from repository.")
                        if (loadedChapters.isNotEmpty()) {
                            _chapters.postValue(loadedChapters) // This will update the UI with new data if any
                            _loadingStatusMessage.postValue("Downlaoding chapters...")
                        } else {
                            _chapters.postValue(emptyList())
                            if (_error.value == null) {
                                _loadingStatusMessage.postValue("No chapters found for ${languageCode.uppercase()}.")
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e("BookViewModel", "Failed to load chapters for $languageCode from repository", exception)
                        // Only show error if we didn't already load from DB.
                        if (needsInitialLoadingScreen) {
                            _error.postValue("Error loading chapters: ${exception.localizedMessage}")
                            _chapters.postValue(emptyList())
                            _loadingStatusMessage.postValue(null)
                        }
                    }
                )
                // Only hide the loading screen if it was shown in the first place.
                if (needsInitialLoadingScreen) {
                    _isLoading.postValue(false)
                    _showInitialLoadMessage.postValue(false) // Signal to hide the message
                }
            }
        }
    }

    fun fetchAboutInfo(languageCode: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _aboutInfo.postValue(repository.getAboutInfo(languageCode, forceRefresh))
        }
    }

    fun onErrorShown() {
        _error.value = null
    }

    fun onStatusMessageShown() {
        _loadingStatusMessage.value = null
    }
}
