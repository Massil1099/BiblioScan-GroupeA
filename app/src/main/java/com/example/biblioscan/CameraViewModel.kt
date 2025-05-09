package com.example.biblioscan

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    private val _imageUri = MutableLiveData<Uri?>()
    val imageUri: LiveData<Uri?> = _imageUri

    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }

    fun clearImageUri() {
        _imageUri.value = null
    }
}
