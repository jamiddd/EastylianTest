package com.jamid.eastyliantest.ui.auth

import android.net.Uri
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.repo.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(val repository: MainRepository): ViewModel() {

    private val _currentImage = MutableLiveData<Uri?>().apply { value = null }
    val currentImage: LiveData<Uri?> = _currentImage

    private val _currentUser = MutableLiveData<FirebaseUser>().apply { value = null }
    val currentUser: LiveData<FirebaseUser> = _currentUser

    fun setCurrentUser(user: FirebaseUser?) {
        _currentUser.postValue(user)
        repository.firebaseUtility.currentFirebaseUserLive.postValue(user)
    }

    fun setCurrentImage(image: Uri? = null) {
        _currentImage.postValue(image)
    }

    fun checkIfUserExists(userId: String, onComplete: ((result: Task<DocumentSnapshot>) -> Unit)? = null) {
        repository.checkIfUserExists(userId, onComplete)
    }

    fun uploadNewUser(name: String, phoneNumber: String, email: String, photo: String? = null) = viewModelScope.launch(
        Dispatchers.IO) {
        repository.uploadNewUser(name, phoneNumber, email, photo)
    }

    fun updateFirebaseUser(changes: Map<String, String?>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        repository.updateFirebaseUser(changes, onComplete)
    }

    fun uploadImage(image: Uri, onComplete: (uri: Uri?) -> Unit) {
        repository.uploadImage(image) {
            onComplete(it)
        }
    }

    fun insertUser(user: User) = viewModelScope.launch (Dispatchers.IO) {
        repository.insertUser(user)
    }

}

@Suppress("UNCHECKED_CAST")
class AuthViewModelFactory(private val repository: MainRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AuthViewModel(repository) as T
    }
}