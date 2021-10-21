package com.jamid.eastyliantest.repo

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.functions.HttpsCallableResult
import com.jamid.eastyliantest.COD
import com.jamid.eastyliantest.STATUS
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.utility.FirebaseUtility

class MainRepository(db: EastylianDatabase) {

    val feedbackDao = db.feedbackDao()
    val refundDao = db.refundDao()
    val orderDao = db.orderDao()
    val notificationDao = db.notificationDao()
    val faqDao = db.faqDao()

    private val userDao = db.userDao()
    private val placeDao = db.placeDao()
    private val cartItemDao = db.cartDao()
    private val cakeDao = db.cakeDao()
    private val restaurantDao = db.restaurantDao()
    private val cakeMenuItemDao = db.cakeMenuItemDao()

    val firebaseUtility = FirebaseUtility()
    val currentUser: LiveData<User> = userDao.currentUser()
    val currentPlace: LiveData<SimplePlace> = placeDao.getCurrentPlace()

    val allOrders: LiveData<List<OrderAndCartItems>> = orderDao.getAllOrders()

    val allCakeMenuItems: LiveData<List<CakeMenuItem>> = cakeMenuItemDao.allMenuItems()

    val pastPlaces = placeDao.getPastLocations()
    val favoriteCakes = cakeDao.favoriteCakes()

    val customCakes = cakeDao.customCakesLive()
    val allFaqs = faqDao.allFaqs()
    val restaurant = restaurantDao.restaurant()





    suspend fun uploadNewUser(name: String, phoneNumber: String, email: String, photo: String? = null) {
        val user = firebaseUtility.uploadNewUser(name, phoneNumber, email, photo)
        if (user != null) {
            userDao.insert(user)
        } else {
            throw Exception("Something went wrong. Couldn't create user.")
        }
    }

    fun updateFirebaseUser(changes: Map<String, String?>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        firebaseUtility.updateFirebaseUser(changes, onComplete)
    }

    fun checkIfUserExists(userId: String, onComplete: ((result: Task<DocumentSnapshot>) -> Unit)? = null) {
        firebaseUtility.checkIfUserExists(userId, onComplete)
    }

    suspend fun insertNearbyPlaces(nearbyPlaces: List<SimplePlace>) {
        placeDao.insertItems(nearbyPlaces)
    }

    suspend fun confirmLocation(currentPlace: SimplePlace) {
        placeDao.updatePastLocations()
        placeDao.insert(currentPlace)
    }

    suspend fun insertOrders(orders: List<Order>) {
        for (order in orders) {
            insertOrder(order)
        }
    }

    suspend fun insertOrder(order: Order) {
        orderDao.insert(order)
        cartItemDao.deletePreviousItems(order.orderId)
        cartItemDao.insertItems(order.items)
    }

    suspend fun getPastOrders() {
        when (val queryResult = firebaseUtility.getPastOrders()) {
            is Result.Error -> {
                Log.e(TAG, queryResult.exception.localizedMessage!!)
            }
            is Result.Success -> {
                val querySnapshot = queryResult.data
                val orders = querySnapshot.toObjects(Order::class.java)
                insertOrders(orders)
            }
        }
    }

    suspend fun insertCake(cake: Cake) {
        cakeDao.insert(cake)
    }

	suspend fun clearOrders() {
		orderDao.clearTable()
	}

    fun uploadTokenToFirebaseUserDocument(token: String) {
        firebaseUtility.uploadToken(token)
    }

    suspend fun clearCartItems() {
        cartItemDao.clearTable()
    }

    fun addCakeToDatabase(cake: Cake) {
        firebaseUtility.addCakeToDatabase(cake)
    }

    fun uploadImage(image: Uri, onComplete: (downloadUrl: Uri?) -> Unit) {
        firebaseUtility.uploadImage(image, onComplete)
    }

    fun updateCakeInDatabase(previousCake: Cake) {
        firebaseUtility.updateCake(previousCake)
    }

    fun sendQuestion(question: String) {
        firebaseUtility.sendQuestion(question)
    }

    suspend fun insertCakes(cakes: List<Cake>) {
        cakeDao.insertItems(cakes)
    }

    suspend fun insertFaqs(faqs: List<Faq>) {
        faqDao.insertItems(faqs)
    }

	suspend fun signOut() {
        userDao.clearTable()
        placeDao.clearTable()
        orderDao.clearTable()
        cartItemDao.clearTable()
        cakeDao.clearTable()
        faqDao.clearTable()
        notificationDao.clearTable()
        restaurantDao.clearTable()
        refundDao.clearTable()
        feedbackDao.clearTable()

        firebaseUtility.signOut()
	}

    suspend fun updateOrderFromNotification(orderId: String, status: OrderStatus) {
        val currentOrderInFocus = orderDao.getParticularOrder(orderId)
        if (currentOrderInFocus != null) {
            val currentOrder = currentOrderInFocus.order
            if (currentOrder.status[0] != status && status != OrderStatus.Created && status != OrderStatus.Paid) {
                if (currentOrder.paymentMethod == COD) {
                    currentOrder.status = listOf(status, OrderStatus.Due)
                } else {
                    currentOrder.status = listOf(status)
                }
                currentOrder.items = currentOrderInFocus.cartItems
                insertOrder(currentOrder)
            }
        }
    }

    suspend fun insertFeedbacks(feedbacks: List<Feedback>) {
        feedbackDao.insertItems(feedbacks)
    }

    suspend fun getCakes() {
        when (val cakesResult = firebaseUtility.getCakes()) {
            is Result.Success -> {
                Log.d(TAG, "Got the non customizable cakes")
                if (!cakesResult.data.isEmpty) {
                    val cakes = cakesResult.data.toObjects(Cake::class.java)
                    for (cake in cakes) {
                        cake.isCustomizable = false
                    }
                    insertCakes(cakes)
                }
            }
            is Result.Error -> {
                cakesResult.exception.let { e ->
                    Log.d(TAG, "Something went wrong while getting cakes. " + e.localizedMessage)
                }
            }
        }
    }

    suspend fun clearOrdersBasedOnStatus(status1: String = STATUS, status2: String = STATUS) {
        orderDao.clearOrdersBasedOnStatus(status1, status2)
    }

    suspend fun insertRefunds(refunds: List<Refund>) {
        refundDao.insertItems(refunds)
    }

    suspend fun insertNotifications(notifications: List<SimpleNotification>) {
        notificationDao.insertItems(notifications)
    }

    suspend fun insertQuestions(questions: List<Faq>) {
        faqDao.insertItems(questions)
    }

    suspend fun insertRestaurantData(restaurant: Restaurant) {
        restaurantDao.insert(restaurant)
    }

    fun createOrDeleteModerator(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) {
        firebaseUtility.createOrDeleteModerator(changes, onComplete)
    }

    fun createOrDeleteDeliveryExecutive(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) {
        firebaseUtility.createOrDeleteDeliveryExecutive(changes, onComplete)
    }

    fun confirmCashOnDeliveryOrder(
        orderId: String,
        orderSenderId: String,
        orderChanges: Map<String, Any>,
        restaurantChanges: Map<String, Any>,
        onComplete: ((result: Task<Void>) -> Unit)? = null
    ) {
        firebaseUtility.confirmCashOnDeliveryOrder(orderId, orderSenderId, orderChanges, restaurantChanges, onComplete)
    }

	suspend fun getMenuItems() {
        when (val querySnapshotResult = firebaseUtility.getMenuItems()) {
		    is Result.Success -> {
		        val querySnapshot = querySnapshotResult.data
                cakeMenuItemDao.insertItems(querySnapshot.toObjects(CakeMenuItem::class.java))
		    }
            is Result.Error -> {
                querySnapshotResult.exception.localizedMessage?.toString()?.let { Log.e(TAG, it) }
            }
		}
	}

    suspend fun insertUser(user: User) {
        userDao.insert(user)
    }

    fun deleteOrderFromFirebase(currentOrder: Order) {
        firebaseUtility.deleteOrder(currentOrder)
    }

    suspend fun getCustomCakes(): List<Cake> {
        return cakeDao.customCakes()
    }

    suspend fun removeCakeFromCartOrder(cakeId: String) {
        cakeDao.updateCake(cakeId, 0)
    }

    suspend fun addCakeToCartOrder(cakeId: String) {
        cakeDao.updateCake(cakeId, 1)
    }

    fun createRefundRequest(refund: Refund, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        firebaseUtility.createRefundRequest(refund, onComplete)
    }

    fun setCurrentUserUpiNumber(upiNumber: String, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        firebaseUtility.setCurrentUserUpiNumber(upiNumber, onComplete)
    }

    companion object {

        private const val TAG = "MainRepository"

        @JvmStatic
        fun newInstance(db: EastylianDatabase) = MainRepository(db)

    }

}