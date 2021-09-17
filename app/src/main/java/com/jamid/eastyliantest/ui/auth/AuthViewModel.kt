package com.jamid.eastyliantest.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jamid.eastyliantest.repo.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(val repository: MainRepository): ViewModel() {

    suspend fun checkIfUserRegistered(uid: String): Boolean? {
        return repository.checkIfUserRegistered(uid)
    }

    fun uploadNewUser(name: String, phoneNumber: String?, email: String?) = viewModelScope.launch(
        Dispatchers.IO) {
        repository.uploadNewUser(name, phoneNumber, email)
    }

    fun updateFirebaseUser(name: String, email: String) {
        repository.updateFirebaseUser(name, email)
    }

}

@Suppress("UNCHECKED_CAST")
class AuthViewModelFactory(private val repository: MainRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AuthViewModel(repository) as T
    }
}