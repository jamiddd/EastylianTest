package com.jamid.eastyliantest.utility

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.model.*
import kotlinx.coroutines.tasks.await

class FirebaseUtility {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    val currentFirebaseUserLive = MutableLiveData<FirebaseUser?>().apply { value = auth.currentUser }

    val networkErrors = MutableLiveData<Exception>()

    val uid: String by lazy {
        auth.currentUser?.uid ?: throw NullPointerException("User Id is null. User not signed in.")
    }

    suspend fun uploadNewUser(name: String, phone: String, email: String, photo: String? = null): User? {
        return try {
            val user = User(
                uid,
                name,
                phone,
                0,
                email,
                photo,
                "",
                false
            )
            val task = db.collection(USERS).document(uid).set(user)
            task.await()
            user
        } catch (e: Exception) {
            networkErrors.postValue(e)
            null
        }
    }

    fun updateFirebaseUser(changes: Map<String, String?>, onComplete: ((result: Task<Void>) -> Unit)? = null) {

        val userEmail = changes["email"]
        val fullName = changes["fullName"]
        val photoUrl = changes["photoUrl"]

        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (userEmail != null) {
                currentUser.updateEmail(userEmail)
                    .addOnFailureListener {
                        networkErrors.postValue(it)
                    }
            }

            val profileUpdates = userProfileChangeRequest {
                displayName = fullName
                photoUri = photoUrl?.toUri()
            }

            val task = currentUser.updateProfile(profileUpdates)
            task.addOnCompleteListener(onComplete)

        }
    }

    fun checkIfUserExists(userId: String, onComplete: ((result: Task<DocumentSnapshot>) -> Unit)? = null) {
        val task = db.collection(USERS).document(userId).get()
        task.addOnCompleteListener(onComplete)
    }

    suspend fun getPastOrders(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(USERS)
                .document(uid)
                .collection(ORDERS)
                .whereArrayContainsAny(STATUS, listOf(DELIVERED, CANCELLED))
                .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                .limit(2)
                .get()

            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            networkErrors.postValue(e)
            Result.Error(e)
        }
    }

    fun confirmCashOnDeliveryOrder(orderId: String, orderSenderId: String, orderChanges: Map<String, Any>, restaurantChanges: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        val batch = db.batch()

        batch.update(db.collection(USERS)
            .document(orderSenderId)
            .collection(ORDERS)
            .document(orderId), orderChanges)

        batch.update(db.collection(RESTAURANT)
            .document(EASTYLIAN), restaurantChanges)

        batch.commit()
            .addOnCompleteListener {
                if (onComplete != null) {
                    onComplete(it)
                }
            }
    }

    fun updateOrder(orderId: String, orderSenderId: String, changes: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        db.collection(USERS)
            .document(orderSenderId)
            .collection(ORDERS)
            .document(orderId)
            .update(changes)
            .addOnCompleteListener {
                onComplete?.let { it1 ->
                    it1(it)
                }
            }
    }

    fun deleteOrder(order: Order) {
        db.collection(USERS)
            .document(uid)
            .collection(ORDERS)
            .document(order.orderId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Order with ${order.orderId} was deleted.")
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    /*suspend fun getCurrentOrders(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(USERS)
                .document(uid)
                .collection(ORDERS)
                .whereIn(STATUS, listOf(PAID, PREPARING, DELIVERING))
                .get()

            val result = task.await()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }*/

    fun uploadToken(token: String) {
        db.collection(USERS)
            .document(uid)
            .update(mapOf("registrationToken" to token))
            .addOnSuccessListener {
                /*Firebase.functions.getHttpsCallable("addRegistrationToken")
                    .call(mapOf("userRegistrationToken" to token))
                    .addOnCompleteListener {
                        if (!it.isSuccessful) {
                            Log.d(TAG, "Successful addition of user registration token.")
                        } else {
                            networkErrors.postValue(it.exception)
                        }
                    }*/
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    fun addCakeToDatabase(cake: Cake) {
        val cakeRef = db.collection(CAKES)
            .document()

        val cakeId = cakeRef.id
        cake.id = cakeId

        cakeRef.set(cake)
            .addOnSuccessListener {
                Log.d(TAG, "Added cake to the database with cake id: $cakeId")
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }

    }

    /*fun removeCakeFromDatabase(cake: Cake) {
        val cakeRef = db.collection(CAKES)
            .document(cake.id)

        cakeRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Removed cake from database with cake id: ${cake.id}")
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }

    }*/

    fun uploadImage(image: Uri, onComplete: (downloadUrl: Uri?) -> Unit) {
        val randomImageName = randomId()
        val ref = storage.reference.child("images/$randomImageName")
        ref.putFile(image)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    onComplete(it)
                }.addOnFailureListener {
                    networkErrors.postValue(it)
                    onComplete(null)
                }
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

    fun updateCake(previousCake: Cake) {
        db.collection(CAKES).document(previousCake.id)
            .set(previousCake, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Cake updated.")
            }.addOnFailureListener {
                networkErrors.postValue(it)
                Log.e(TAG, it.localizedMessage!!)
            }
    }

    fun sendQuestion(question: String) {
         val newFaqRef = db.collection("faq")
            .document()
        val faq = Faq(newFaqRef.id, question, "", uid, 0.0f, false, System.currentTimeMillis())

        newFaqRef.set(faq)
            .addOnSuccessListener {
                Log.d(TAG, "Sent question with id: " + newFaqRef.id)
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }
    }

	fun signOut() {
	    auth.signOut()
	}

	suspend fun fetchItems(query: Query, lim: Int = 20, lastSnapshot: DocumentSnapshot? = null): Result<QuerySnapshot> {
		return if (lastSnapshot != null) {
            try {
                val task = query.startAfter(lastSnapshot)
                    .orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(lim.toLong())
                    .get()

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        } else {
            try {
                val task = query.orderBy(CREATED_AT, Query.Direction.DESCENDING)
                    .limit(lim.toLong())
                    .get()

                val result = task.await()
                Result.Success(result)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
	}

	fun uploadNewOrder(order: Order, onComplete: ((result: Task<Void>) -> Unit)? = null) {
		db.collection(USERS)
            .document(uid)
            .collection(ORDERS)
            .document(order.orderId)
            .set(order)
            .addOnCompleteListener { it1 ->
                onComplete?.let {
                    onComplete(it1)
                }
            }
	}

    fun updateRestaurantData(changes: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        db.collection(RESTAURANT)
            .document(EASTYLIAN)
            .update(changes)
            .addOnCompleteListener { result ->
                onComplete?.let {
                    it(result)
                }
            }
    }

	suspend fun getCakes(): Result<QuerySnapshot> {
        return try {
        	val task = db.collection(CAKES)
                .whereEqualTo(AVAILABLE, true)
                .get()

            Result.Success(task.await())
        } catch (e: Exception) {
            Result.Error(e)
        }
	}

    private fun call(funcName: String, data: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) {
        Firebase.functions.getHttpsCallable(funcName)
            .call(data)
            .addOnCompleteListener(onComplete)
    }

    fun createOrDeleteModerator(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) {
        call("createOrDeleteModerator", changes, onComplete)
    }

    fun createOrDeleteDeliveryExecutive(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) {
        call("createOrDeleteDeliveryExecutive", changes, onComplete)
    }

    fun deleteCake(id: String, onComplete: ((task: Task<Void>) -> Unit)? = null) {
        Firebase.firestore.collection(CAKES)
            .document(id)
            .delete()
            .addOnCompleteListener(onComplete)
    }

    suspend fun getMenuItems(): Result<QuerySnapshot> {
        return try {
            val task = db.collection(RESTAURANT).document(EASTYLIAN).collection("menuItems")
                .get()

            Result.Success(task.await())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun removeCakeMenuItem(cakeMenuItem: CakeMenuItem) {
        db.collection(RESTAURANT)
            .document(EASTYLIAN)
            .collection("menuItems")
            .document(cakeMenuItem.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Cake menu item deleted.")
            }.addOnFailureListener {
                networkErrors.postValue(it)
            }

    }

    fun updateUser(changes: Map<String, String?>) {
        db.collection(USERS)
            .document(uid)
            .update(changes)
            .addOnSuccessListener {
                Log.d(TAG, "User updated")
            }.addOnFailureListener {
                Log.e(TAG, it.localizedMessage!!)
            }
    }

    fun uploadNotification(notification: SimpleNotification, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        val newNotificationRef = Firebase.firestore.collection(NOTIFICATIONS).document()
        val task = newNotificationRef.set(notification)
        task.addOnCompleteListener(onComplete)
    }


    companion object {
        private const val TAG = "FirebaseUtility"
    }

}
