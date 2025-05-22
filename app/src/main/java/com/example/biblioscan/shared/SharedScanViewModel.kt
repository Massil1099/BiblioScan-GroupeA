package com.example.biblioscan.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedScanViewModel : ViewModel() {
    private val _capturedImagePath = MutableLiveData<String>()
    val capturedImagePath: LiveData<String> = _capturedImagePath

    private val _detectedTexts = MutableLiveData<List<String>>()
    val detectedTexts: LiveData<List<String>> = _detectedTexts

    fun setResults(imagePath: String, texts: List<String>) {
        _capturedImagePath.value = imagePath
        _detectedTexts.value = texts
    }

    fun clearResults() {
        _capturedImagePath.value = null
        _detectedTexts.value = emptyList()
    }
}
