package com.blackgrapes.kadachabuk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BookViewModel : ViewModel() {

    private val repository = BookRepository()

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
                val chapterList = repository.getChapters(languageCode)
                if (chapterList.isNotEmpty()) {
                    _chapters.value = chapterList
                } else {
                    _error.value = "No chapters found or error loading data for '$languageCode'."
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
