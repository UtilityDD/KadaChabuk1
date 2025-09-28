package com.blackgrapes.kadachabuk

import android.app.Application // Import Application
import androidx.lifecycle.AndroidViewModel // Change from ViewModel to AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
// Removed: import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) { // Inherit from AndroidViewModel

    // Pass the application context to the repository
    private val repository = BookRepository(application.applicationContext)

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> = _chapters

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadChapters(languageCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // This call now uses the repository's caching logic
                val chapterList = repository.getChapters(languageCode)
                if (chapterList.isNotEmpty()) {
                    _chapters.value = chapterList
                } else {
                    // Check if the error is already set by the repository or if it's genuinely no chapters
                    if (_error.value == null) { // Avoid overwriting specific repo errors
                        _error.value = "No chapters found for '$languageCode'."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load chapters: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onErrorShown() {
        _error.value = null
    }
}
