package com.jamid.eastyliantest.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.paging.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.db.*
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.repo.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

data class ContextObject(val state: Boolean, val cake: Cake)

enum class CartItemChange {
    Increment, Decrement, Update
}

class MainViewModel(val repo: MainRepository): ViewModel() {

    init {

        viewModelScope.launch (Dispatchers.IO) {
            repo.clearCartItems()
            repo.clearOrders()
            repo.getMenuItems()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result
                repo.uploadTokenToFirebaseUserDocument(token)
            } else {
                Log.e(TAG, it.exception?.localizedMessage.orEmpty())
            }
        }

        Firebase.firestore.collection(USERS).document(Firebase.auth.currentUser?.uid.orEmpty())
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, error.localizedMessage!!)
                    return@addSnapshotListener
                }

                if (value != null && value.exists()) {
                    try {
                        val user = value.toObject(User::class.java)
                        viewModelScope.launch (Dispatchers.IO) {
                            if (user != null) {
                                repo.insertUser(user)
                            } else {
                                Log.d(TAG, "User is null")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.localizedMessage!!)
                    }
                    Log.d(TAG, value.toString())
                }
            }

    }

    private val allCakeMenuItems = repo.allCakeMenuItems

    val flavorMenuItems = Transformations.map(allCakeMenuItems) {
        it.filter { it1 ->
            it1.category == "flavor"
        }
    }

    val baseMenuItems = Transformations.map(allCakeMenuItems) {
        it.filter { it1 ->
            it1.category != "flavor"
        }
    }

   /* val flavorList = Transformations.map(flavorMenuItems) {
        it.map { it1 ->
            it1.title
        }
    }

    val priceList = Transformations.map(flavorMenuItems) {
        it.map { it1 ->
            it1.title to it1.price
        }
    }*/


    private val _currentCartOrder = MutableLiveData<Order>().apply { value = null }
    val currentCartOrder: LiveData<Order> = _currentCartOrder

    fun setCurrentCartOrder(order: Order?) {
        _currentCartOrder.postValue(order)
    }

    private val _currentPaymentResult = MutableLiveData<Result<Boolean>?>()
    val currentPaymentResult: LiveData<Result<Boolean>?> = _currentPaymentResult

    fun setCurrentPaymentResult(result: Result<Boolean>? = null) {
        _currentPaymentResult.postValue(result)
    }

    private val _currentSelectedPlace = MutableLiveData<SimplePlace>().apply { value = null }
    val currentSelectedPlace: LiveData<SimplePlace> = _currentSelectedPlace

    val currentPlace: LiveData<SimplePlace> = repo.currentPlace

    val gpsLocation = MutableLiveData<LatLng>().apply { value = null }

    val windowInsets = MutableLiveData<Pair<Int, Int>>().apply { value = 0 to 0 }

    val favoriteCakes = repo.favoriteCakes

    var isHomeAppBarExpanded = true

    val currentError = MutableLiveData<Exception>()

    val contextModeState = MutableLiveData<ContextObject>().apply { value = ContextObject(false, Cake()) }

    private fun getBasePriceFromItems(cartItems: List<CartItem>): Long {
        var basePrice: Long = 0
        for (item in cartItems) {
            basePrice += item.totalPrice
        }
        return basePrice
    }

    fun updateCurrentCartOrder(cake: Cake? = null, cartItem: CartItem? = null, change: CartItemChange) {
        val currentOrder = _currentCartOrder.value

        if (currentOrder != null) {

            val existingItemsList = currentOrder.items.toMutableList()

            fun onExistingItemExists(existingCartItem: CartItem, newItem: CartItem): Boolean {
                // check if the quantity increased or decreased
                when (change) {
                    CartItemChange.Increment -> {
                        val newCartItem = increaseCartItemCount(existingCartItem)

                        existingItemsList.findAndReplace(newCartItem) {
                            it.cartItemId == newCartItem.cartItemId
                        }
                    }
                    CartItemChange.Decrement -> {
                        val newCartItem = decreaseCartItemCount(existingCartItem)
                        if (newCartItem == null) {

                            // remove the cart item from the list
                            existingItemsList.removeAtIf {
                                it.cartItemId == existingCartItem.cartItemId
                            }

                            // after removing check if it was the only item
                            if (existingItemsList.isEmpty()) {
                                setCurrentCartOrder(null)
                                return false
                            }

                        } else {
                            // because the quantity is not 0, the item does have cake in it
                            existingItemsList.findAndReplace(newCartItem) {
                                it.cartItemId == newCartItem.cartItemId
                            }
                        }
                    }
                    CartItemChange.Update -> {
                        existingItemsList.findAndReplace(newItem) {
                            it.cartItemId == newItem.cartItemId
                        }
                    }
                }

                return true
            }

            if (cartItem != null) {
                val existingCartItem = currentOrder.items.find {
                    it.cartItemId == cartItem.cartItemId
                }

                if (existingCartItem == null) {
                    // add new cart item
                    existingItemsList.add(cartItem)
                } else {
                    if (!onExistingItemExists(existingCartItem, cartItem)) {
                        return
                    }
                }
            } else {
                val existingItem = existingItemsList.find {
                    it.cake.id == cake?.id
                }

                if (existingItem == null) {
                    // didn't find any cart item which has the cake
                    val newCartItem = CartItem.newInstance(cake!!, currentOrder.orderId)
                    existingItemsList.add(newCartItem)
                } else {
                    existingItem.cake = cake!!

                    // update the price here
                    existingItem.totalPrice = existingItem.cake.price * existingItem.quantity

                    onExistingItemExists(existingItem, existingItem)
                }
            }

            currentOrder.items = existingItemsList
            setOrderAmount(currentOrder)
            setCurrentCartOrder(currentOrder)
        } else {
            val newOrder = Order.normalOrder(listOf(), repo.firebaseUtility.uid)
            if (cartItem != null) {
                cartItem.orderId = newOrder.orderId
                newOrder.items = listOf(cartItem)
            } else {
                val newCartItem = CartItem.newInstance(cake!!, newOrder.orderId)
                newOrder.items = listOf(newCartItem)
            }
            setOrderAmount(newOrder)
            setCurrentCartOrder(newOrder)
        }
    }

    private fun increaseCartItemCount(cartItem: CartItem): CartItem {
        cartItem.quantity += 1
        cartItem.totalPrice = cartItem.cake.price * cartItem.quantity
        return cartItem
    }

    private fun decreaseCartItemCount(cartItem: CartItem): CartItem? {
        return if (cartItem.quantity == one) {
            null
        } else {
            cartItem.quantity -= 1
            cartItem.totalPrice = cartItem.cake.price * cartItem.quantity
            cartItem
        }
    }


    /*fun addItemToCurrentOrder(cake: Cake): Boolean {
        val previousOrderAndItems = currentOrder.value
        return if (previousOrderAndItems != null) {
            val existingCartItems = previousOrderAndItems.cartItems.toMutableList()

            val existingCartItem = existingCartItems.find {
                it.cake.id == cake.id
            }

            if (existingCartItem != null) {

                existingCartItem.quantity += 1
                existingCartItem.totalPrice = existingCartItem.cake.price * existingCartItem.quantity

                existingCartItems.findAndReplace(existingCartItem) {
                    it.cartItemId == existingCartItem.cartItemId
                }

            } else {

                val cartItem = CartItem.newInstance(cake, previousOrderAndItems.order.orderId)
                existingCartItems.add(cartItem)

                setOrderAmount(previousOrderAndItems.order)

            }

            previousOrderAndItems.order.items = existingCartItems
            insertOrder(previousOrderAndItems.order)

            true

        } else {
            val userId = Firebase.auth.currentUser?.uid.orEmpty()
            val order = Order.normalOrder(listOf(), userId)
            val items = listOf(CartItem.newInstance(cake, order.orderId))
            order.items = items
            val place = currentPlace.value
            if (place != null) {
                order.place = place
            } else {
                order.delivery = false
            }

            setOrderAmount(order)
            insertOrder(order)
            true
        }
    }*/

    fun insertOrder(order: Order) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertOrder(order)
    }

    private fun setOrderAmount(previousOrder: Order) {
        val prices = if (previousOrder.delivery) {
            CartPrices.newInstance(getBasePriceFromItems(previousOrder.items), DELIVERY_PRICE)
        } else {
            CartPrices.newInstance(getBasePriceFromItems(previousOrder.items), ZERO_L)
        }
        previousOrder.prices = prices
    }

    private fun <T: Any> MutableList<T>.removeAtIf(predicate: (T) -> Boolean) {
        var removePos = -1
        for (i in this.indices) {
            if (predicate(this[i])) {
                removePos = i
            }
        }
        this.removeAt(removePos)
    }

    /*fun updateItemToCurrentOrder(cartItem: CartItem): Boolean {
        val previousOrderAndItems = currentOrder.value
        return if (previousOrderAndItems != null) {
            val existingCartItems = previousOrderAndItems.cartItems.toMutableList()

            if (cartItem.quantity == zero) {

                addedCakeList.remove(cartItem.cake.id)

                existingCartItems.removeAtIf {
                    it.cartItemId == cartItem.cartItemId
                }

                if (existingCartItems.isEmpty()) {
                    deleteCurrentOrderLocally(previousOrderAndItems.order)
                } else {
                    previousOrderAndItems.order.items = existingCartItems
                    setOrderAmount(previousOrderAndItems.order)
                    insertOrder(previousOrderAndItems.order)
                }
                return true
            } else {
                existingCartItems.findAndReplace(cartItem) {
                    it.cartItemId == cartItem.cartItemId
                }

                previousOrderAndItems.order.items = existingCartItems

                setOrderAmount(previousOrderAndItems.order)
                insertOrder(previousOrderAndItems.order)
                return true
            }
        } else {
            false
        }
    }*/

    private fun <T> MutableList<T>.findAndReplace(newItem: T, predicate: (T) -> Boolean) {
        var pos = -1
        for (i in this.indices) {
            if (predicate(this[i])) {
                pos = i
            }
        }
        if (pos != -1)
            this.removeAt(pos)
        this.add(newItem)
    }


    fun updateDeliveryMethod(isDelivery: Boolean): Boolean {
        val previousOrder = currentCartOrder.value
        return if (previousOrder != null) {
            previousOrder.delivery = isDelivery
            if (!isDelivery) {
                previousOrder.paymentMethod = "online|counter"
            }
            setOrderAmount(previousOrder)
            setCurrentCartOrder(previousOrder)
            true
        } else {
            false
        }
    }

    fun setCurrentPlace(simplePlace: SimplePlace?) = viewModelScope.launch(Dispatchers.IO) {
        _currentSelectedPlace.postValue(simplePlace)
    }

    fun setCurrentError(e: Exception) {
        currentError.postValue(e)
    }

    fun insertNearbyPlaces(nearbyLocations: List<SimplePlace>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertNearbyPlaces(nearbyLocations)
    }

    fun confirmPlace(currentPlace: SimplePlace) = viewModelScope.launch(Dispatchers.IO) {
        repo.confirmLocation(currentPlace)
    }

    fun updateOrderTime(time: Long, isSetByUser: Boolean = true) {
        val previousOrder = currentCartOrder.value
        if (previousOrder != null) {
            previousOrder.timeSetByUser = isSetByUser
            previousOrder.deliveryAt = time
            setCurrentCartOrder(previousOrder)
        }
    }

    fun updateOrderLocation(currentPlace: SimplePlace) {
        val previousOrder = currentCartOrder.value
        if (previousOrder != null) {
            previousOrder.place = currentPlace
            setCurrentCartOrder(previousOrder)
        }
    }

    fun getPastOrders() = viewModelScope.launch (Dispatchers.IO) {
        Log.d(TAG, "Getting past orders")
        repo.getPastOrders()
    }

    fun insertCake(cake: Cake) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertCake(cake)
    }

    fun updateOrder(orderId: String, orderSenderId: String, changes: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) = viewModelScope.launch (Dispatchers.IO) {
        repo.firebaseUtility.updateOrder(orderId, orderSenderId, changes) {
            onComplete?.let { it1 -> it1(it) }
        }
    }

    fun insertOrders(orders: List<Order>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertOrders(orders)
    }

    fun sendRegistrationTokenToServer(token: String) {
        repo.uploadTokenToFirebaseUserDocument(token)
    }

    fun addCakeToDatabase(cake: Cake) {
        repo.addCakeToDatabase(cake)
    }


    private val _currentImage = MutableLiveData<Uri?>().apply { value = null }
    val currentImage: LiveData<Uri?> = _currentImage

    fun setCurrentImage(image: Uri?) {
        _currentImage.postValue(image)
    }

    fun uploadImage(image: Uri, onComplete: (uri: Uri?) -> Unit) {
        repo.uploadImage(image, onComplete)
    }

    fun updateCakeInDatabase(previousCake: Cake) {
        repo.updateCakeInDatabase(previousCake)
    }

    fun sendQuestion(question: String) {
        repo.sendQuestion(question)
    }

    fun insertFaqs(faqs: List<Faq>) = viewModelScope.launch(Dispatchers.IO) {
        repo.insertFaqs(faqs)
    }

	fun signOut() = viewModelScope.launch(Dispatchers.IO) {
		repo.signOut()
	}

    @ExperimentalPagingApi
    fun pastOrdersFlow(query: Query): Flow<PagingData<OrderAndCartItems>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
                    remoteMediator = OrdersRemoteMediator(query, repo, LIKE_DELIVERED, LIKE_CANCELLED)
        ) {
            repo.orderDao.getPagedOrdersBasedOnStatus(LIKE_DELIVERED, LIKE_CANCELLED)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun deliveredOrdersFlow(): Flow<PagingData<OrderAndCartItems>> {
        val query = Firebase.firestore.collectionGroup(ORDERS)
            .whereArrayContainsAny(STATUS, listOf(DELIVERED))

        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = OrdersRemoteMediator(query, repo, LIKE_DELIVERED)
        ) {
            repo.orderDao.getPagedOrdersBasedOnStatus(LIKE_DELIVERED)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun deliveringOrdersFlow(query: Query): Flow<PagingData<OrderAndCartItems>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = OrdersRemoteMediator(query, repo, LIKE_DELIVERING)
        ) {
            repo.orderDao.getPagedOrdersBasedOnStatus(LIKE_DELIVERING)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun pendingOrdersFlow(): Flow<PagingData<OrderAndCartItems>> {
        val query = Firebase.firestore.collectionGroup(ORDERS)
            .whereArrayContainsAny(STATUS, listOf(PREPARING))

        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = OrdersRemoteMediator(query, repo, LIKE_PREPARING)
        ) {
            repo.orderDao.getPagedOrdersBasedOnStatus(LIKE_PREPARING)
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun requestedOrdersFlow(): Flow<PagingData<OrderAndCartItems>> {
        val query = Firebase.firestore.collectionGroup(ORDERS)
            .whereArrayContainsAny(STATUS, listOf(PAID, DUE))

        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = OrdersRemoteMediator(query, repo, LIKE_PAID, "Due%")
        ) {
            repo.orderDao.getPagedOrdersBasedOnStatus(LIKE_PAID, "Due%")
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun pagedRefundsFlow(query: Query): Flow<PagingData<Refund>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = RefundRemoteMediator(query, repo)
        ) {
            repo.refundDao.pagedRefunds()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun pagedNotificationsFlow(query: Query): Flow<PagingData<SimpleNotification>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = NotificationRemoteMediator(query, repo)
        ) {
            repo.notificationDao.pagedNotifications()
        }.flow.cachedIn(viewModelScope)
    }

    @ExperimentalPagingApi
    fun pagedQuestionsFlow(query: Query): Flow<PagingData<Faq>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = QuestionsRemoteMediator(query, repo)
        ) {
            repo.faqDao.pagedQuestions()
        }.flow.cachedIn(viewModelScope)
    }

    fun updateOrderFromNotification(orderId: String, status: OrderStatus) = viewModelScope.launch(Dispatchers.IO) {
        repo.updateOrderFromNotification(orderId, status)
    }

    fun updateOrderPaymentMethod(order: Order) = viewModelScope.launch (Dispatchers.IO) {
        setCurrentCartOrder(order)
    }

    var addedCakeList = mutableListOf<String>()

    fun setNewCakeList(newAddedCakeList: List<String>) {
        addedCakeList = newAddedCakeList.toMutableList()
    }

	@ExperimentalPagingApi
    fun getFeedbacks(query: Query): Flow<PagingData<Feedback>> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            remoteMediator = FeedbackRemoteMediator(query, repo)
        ) {
            repo.feedbackDao.pagedFeedbacks()
        }.flow.cachedIn(viewModelScope)
	}

	fun uploadOrder(order: Order, onComplete: ((result: Task<Void>) -> Unit)? = null) {
		repo.firebaseUtility.uploadNewOrder(order, onComplete)
	}

    fun updateRestaurantData(changes: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        repo.firebaseUtility.updateRestaurantData(changes, onComplete)
    }

    fun confirmOrder(orderId: String, orderSenderId: String, orderChanges: Map<String, Any>, restaurantChanges: Map<String, Any>, onComplete: ((result: Task<Void>) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        repo.confirmCashOnDeliveryOrder(orderId, orderSenderId, orderChanges, restaurantChanges, onComplete)
    }

    fun getCakes() = viewModelScope.launch(Dispatchers.IO) {
        repo.getCakes()
    }

    fun insertRestaurantData(restaurant: Restaurant) = viewModelScope.launch (Dispatchers.IO) {
        repo.insertRestaurantData(restaurant)
    }

    fun createOrDeleteModerator(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) = viewModelScope.launch (Dispatchers.IO) {
        repo.createOrDeleteModerator(changes, onComplete)
    }

    fun createOrDeleteDeliveryExecutive(changes: Map<String, Any>, onComplete: ((result: Task<HttpsCallableResult>) -> Unit)? = null) = viewModelScope.launch (Dispatchers.IO) {
        repo.createOrDeleteDeliveryExecutive(changes, onComplete)
    }

    fun deleteCake(id: String, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        repo.firebaseUtility.deleteCake(id, onComplete)
    }

    fun removeCakeMenuItem(cakeMenuItem: CakeMenuItem) {
        repo.firebaseUtility.removeCakeMenuItem(cakeMenuItem)
    }

    fun updateFirebaseUser(changes: Map<String, String?>, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        repo.firebaseUtility.updateFirebaseUser(changes, onComplete)
    }

    fun updateUser(changes: Map<String, String?>) {
        repo.firebaseUtility.updateUser(changes)
    }

    fun uploadNotification(notification: SimpleNotification, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        repo.firebaseUtility.uploadNotification(notification, onComplete)
    }

    fun resendNotification(notification: SimpleNotification, onComplete: ((result: Task<Void>) -> Unit)? = null) {
        uploadNotification(notification, onComplete)
    }

    fun deleteOrderFromFirebase() {
        val currentOrder = currentCartOrder.value
        if (currentOrder != null) {
            repo.deleteOrderFromFirebase(currentOrder)
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val one: Long = 1
        private const val LIKE_DELIVERED = "%Delivered%"
        private const val LIKE_DELIVERING = "%Delivering%"
        private const val LIKE_PREPARING = "%Preparing%"
        private const val LIKE_PAID = "%Paid%"
        private const val LIKE_CANCELLED = "%Cancelled%"
    }

}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val repository: MainRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}