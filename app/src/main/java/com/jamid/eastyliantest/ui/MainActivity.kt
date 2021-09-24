package com.jamid.eastyliantest.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.ActivityMainBinding
import com.jamid.eastyliantest.databinding.FeedbackLayoutBinding
import com.jamid.eastyliantest.databinding.RateAnswerLayoutBinding
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.interfaces.*
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.model.OrderStatus.*
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.*
import com.jamid.eastyliantest.views.zoomable.ImageViewFragment
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.net.URL


class MainActivity : LocationAwareActivity(), CakeClickListener, OrderImageClickListener, PaymentResultWithDataListener, CartItemClickListener, OrderClickListener, FaqListener {

    val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var repository: MainRepository
    private lateinit var notificationManager: NotificationManager


    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val image = it.data?.data
            viewModel.setCurrentImage(image)
        } else {
            viewModel.setCurrentImage(null)
        }
    }

    private val orderFeedbacksTemp = mutableMapOf<String, Feedback>()

    private val tokenReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val token = intent?.extras?.getString(TOKEN)
            if (token != null) {
                viewModel.sendRegistrationTokenToServer(token)
            }
        }
    }

    private val notificationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.extras?.get(TITLE) as String? ?: ""
            val content = intent?.extras?.get(BODY) as String? ?: ""
            val image = intent?.extras?.get(IMAGE) as String?

            val notifyBuilder = getNotificationBuilder(title, content, image)

            val orderId = intent?.extras?.get(ORDER_ID) as String?
            val status = intent?.extras?.get(STATUS) as String?

            if (orderId != null && status != null) {
                viewModel.updateOrderFromNotification(orderId, status.toOrderStatus())

                if (status == DELIVERED) {
                    openFeedbackDialog(orderId)
                }

            }

            notificationManager.notify(NOTIFICATION_ID, notifyBuilder.build())
        }
    }

    private fun onLocationSettingsRetrieved(result: Result<LocationSettingsResponse>) {
        when (result) {
            is Result.Error -> {
                val exception = result.exception
                if (exception is ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        activityResult.launch(
                            IntentSenderRequest.Builder(exception.resolution.intentSender)
                                .build()
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
            is Result.Success -> {
                locationUtility.getNearbyPlaces()
            }
        }
    }

   /* private fun deleteCurrentOrderFromFirebase() {
        val currentOrder = viewModel.currentCartOrder.value ?: return
        viewModel.deleteCurrentOrderFromFirebase(currentOrder)
    }*/

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                PRIMARY_CHANNEL_ID,
                "Mascot Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notification from Mascot"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun NotificationCompat.Builder.applyImageUrl(
        imageUrl: String
    ) = runBlocking {
        val url = URL(imageUrl)

        withContext(Dispatchers.IO) {
            try {
                val input = url.openStream()
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                null
            }
        }?.let { bitmap ->
            this@applyImageUrl.setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(bitmap)
            ).setLargeIcon(bitmap)
        }
    }

    private fun getNotificationBuilder(title: String, content: String, image: String? = null): NotificationCompat.Builder {
        val pendingIntent = NavDeepLinkBuilder(this)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.dashboardFragment)
            .setArguments(null)
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)

        Log.d(TAG, image.toString())

        if (image != null) {
            notificationBuilder.applyImageUrl(image)

        }

        return notificationBuilder.setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_stat_cake_1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        updateValuesFromBundle(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initiate(binding.root, binding.navView)
        Checkout.preload(applicationContext)
        createNotificationChannel()
        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        locationUtility.getNearbyPlaces()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.dashboardFragment, R.id.cartFragment -> {
                    val badge = binding.navView.getOrCreateBadge(R.id.cart_navigation)
                    badge.backgroundColor = ContextCompat.getColor(this, R.color.primaryColor)
                    binding.navView.slideReset()
                    binding.orderDeliveryProgressCard.slideReset()
                    badge.isVisible = destination.id != R.id.cartFragment && viewModel.currentCartOrder.value != null

                    binding.viewCartCard.setOnClickListener {
                        binding.navView.selectedItemId = R.id.cart_navigation
                    }

                    if (destination.id == R.id.cartFragment) {
                        binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                    } else {
                        if (viewModel.currentCartOrder.value != null) {
                            binding.viewCartCard.slideReset()
                        } else {
                            binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                        }
                    }
                }
                else -> {
                    binding.viewCartCard.setOnClickListener {
                        navController.navigateUp()
                        binding.navView.selectedItemId = R.id.cart_navigation
                    }
                    binding.navView.slideDown(convertDpToPx(100).toFloat())
                    binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                    binding.orderDeliveryProgressCard.slideDown(convertDpToPx(100).toFloat())

                }
            }
        }
        navView.setupWithNavController(navController)

        navView.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.home_navigation -> {
                    findViewById<NestedScrollView>(R.id.homeParentScroll)?.smoothScrollTo(0, 0)
                }
                R.id.account_navigation -> {
                    findViewById<NestedScrollView>(R.id.dashboardScroller)?.smoothScrollTo(0, 0)
                }
            }
        }

        binding.currentOrderItemCount.setFactory {
            TextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Medium)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            }
        }

        // layout, inset related
        binding.root.setOnApplyWindowInsetsListener { _, insets ->
            // should only be less than
            val (top: Int, bottom: Int) = if (Build.VERSION.SDK_INT < 30) {
                @Suppress("DEPRECATION")
                val statusBarSize = insets.systemWindowInsetTop

                @Suppress("DEPRECATION")
                val navBarSize = insets.systemWindowInsetBottom
                statusBarSize to navBarSize
            } else {
                val statusBar = insets.getInsets(WindowInsets.Type.statusBars())
                val navBar = insets.getInsets(WindowInsets.Type.navigationBars())
                val keyBoard = insets.getInsets(WindowInsets.Type.ime())

                val statusBarSize = statusBar.top - statusBar.bottom
                val navBarSize = navBar.bottom - navBar.top
                val keyBoardSize = keyBoard.bottom - keyBoard.top
                if (keyBoardSize > 0) {
                    statusBarSize to keyBoardSize
                } else {
                    statusBarSize to navBarSize
                }
            }

            viewModel.windowInsets.postValue(Pair(top, bottom))
            insets
        }

        viewModel.currentCartOrder.observe(this) { currentOrder ->
            val badge = binding.navView.getOrCreateBadge(R.id.cart_navigation)
            if (currentOrder != null) {

                val inAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
                val outAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)

                inAnim.duration = 150
                outAnim.duration = 150

                binding.currentOrderItemCount.inAnimation = inAnim
                binding.currentOrderItemCount.outAnimation = outAnim

                if (currentOrder.items.size == 1) {
                    binding.currentOrderItemCount.setText("${currentOrder.items.size} Item")
                } else {
                    binding.currentOrderItemCount.setText("${currentOrder.items.size} Items")
                }

                if (navController.currentDestination?.id == R.id.cartFragment) {
                    binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                } else if (navController.currentDestination?.id == R.id.homeFragment || navController.currentDestination?.id == R.id.dashboardFragment) {
                    binding.viewCartCard.slideReset()
                } else {
                    binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                }

                when (currentOrder.status[0]) {
                    Created -> {
                        badge.backgroundColor = ContextCompat.getColor(this, R.color.primaryColor)
                        badge.isVisible = true
                    }
                    Paid -> {
                       // do nothing
                    }
                    Preparing, Delivering, Delivered -> {
                        // Do nothing
                    }
                    Cancelled -> {
                        toast("The order was cancelled. If money was deducted it will be refunded shortly after checking.")
                    }
                    Rejected -> {
                        toast("It seems you have failed to retrieve the order in time. You may be charged with the order amount.")
                    }
                    Due -> {

                    }
                }

                updateHomeCakes(currentOrder)

            } else {
                badge.isVisible = false
                binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
                viewModel.addedCakeList.clear()
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver, IntentFilter("tokenIntent"))
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(NOTIFICATION_INTENT))

        viewModel.currentError.observe(this) {
            it?.localizedMessage?.let { it1 ->
                toast(it1)
            }
        }

        Firebase.messaging.subscribeToTopic(GENERAL)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    viewModel.setCurrentError(task.exception!!)
                } else {
                    Log.d(TAG, "Subscribing to :\"General\": topic in FCM")
                }
            }

        viewModel.getCakes()

        viewModel.getPastOrders()

        locationUtility.isLocationEnabled.observe(this) { isEnabled ->
            if (!isEnabled) {
                askUserToEnableLocation {
                    onLocationSettingsRetrieved(it)
                }
            } else {
                if (locationUtility.isLocationPermissionAvailable.value == true) {
                    requestingLocationUpdates = true
                    locationUtility.getNearbyPlaces()
                } else {
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        locationUtility.isLocationPermissionAvailable.observe(this) { isGranted ->
            if (isGranted) {
                requestingLocationUpdates = true
                if (locationUtility.isLocationEnabled.value == true) {
                    locationUtility.getNearbyPlaces()
                } else {
                    askUserToEnableLocation {
                        onLocationSettingsRetrieved(it)
                    }
                }
            } else {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        locationUtility.nearbyPlaces.observe(this) {
            viewModel.insertNearbyPlaces(it)
        }

        locationUtility.currentLocation.observe(this) { location ->
            if (location != null) {
                viewModel.gpsLocation.postValue(LatLng(location.latitude, location.longitude))
            }
            stopLocationUpdates()
        }

        // currently delivering orders
        viewModel.repo.allOrders.observe(this) {
            val currentlyDeliveringOrders = it.filter { it1 ->
                it1.order.status[0] == Delivering && it1.order.delivery
            }

            if (currentlyDeliveringOrders.isNotEmpty()) {
                if (navController.currentDestination?.id == R.id.homeFragment || navController.currentDestination?.id == R.id.cartFragment || navController.currentDestination?.id == R.id.dashboardFragment) {
                    binding.orderDeliveryProgressCard.show()
                    binding.orderDeliveryProgressCard.slideReset()
                }
                binding.navView.selectedItemId = R.id.account_navigation
            } else {
                binding.orderDeliveryProgressCard.slideDown(convertDpToPx(100).toFloat())
                binding.orderDeliveryProgressCard.hide()
            }
        }

        viewModel.repo.currentUser.observe(this) { currentUser ->
            if (currentUser != null) {
                addCurrentOrdersListener(currentUser)
            }
        }

        Firebase.firestore.collection(RESTAURANT)
            .document(EASTYLIAN)
            .get()
            .addOnSuccessListener {
                val restaurant = it.toObject(Restaurant::class.java)!!
                viewModel.insertRestaurantData(restaurant)
            }.addOnFailureListener { error ->
                error.localizedMessage?.let { msg ->
                    Log.e(TAG, msg)
                }
            }


        /*viewModel.repo.firebaseUtility.currentFirebaseUserLive.observe(this) {
            if (it != null) {
                val photo = if (it.photoUrl == null) {
                    null
                } else {
                    it.photoUrl.toString()
                }
                val changes = mapOf("name" to it.displayName, "photoUrl" to photo)
                viewModel.updateUser(changes)
            }
        }*/

    }

    // add listener for all current orders
    private fun addCurrentOrdersListener(currentUser: User) {
        Firebase.firestore.collection(USERS)
            .document(currentUser.userId)
            .collection(ORDERS)
            .whereArrayContainsAny(STATUS, listOf(DUE, PAID, PREPARING, DELIVERING))
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    viewModel.setCurrentError(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val orders = querySnapshot.toObjects(Order::class.java)
                    viewModel.insertOrders(orders)
                }
            }
    }

    private fun updateHomeCakes(currentOrder: Order) {
        val addedCakeList = mutableListOf<String>()

        val customCakeList = viewModel.repo.customCakes.value
        if (!customCakeList.isNullOrEmpty()) {
            for (i in customCakeList.indices) {
                for (item in currentOrder.items) {
                    if (customCakeList[i].id == item.cake.id) {
                        addedCakeList.add(customCakeList[i].id)
                    }
                }
            }
        }

        viewModel.setNewCakeList(addedCakeList)

    }

    /*override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        val currentOrder = viewModel.currentCartOrder.value
        if (currentOrder != null) {
            outState.putString(CURRENT_ORDER_ID, currentOrder.orderId)
        }
        super.onSaveInstanceState(outState)
    }*/

    override fun onResume() {
        super.onResume()
        locationUtility.getNearbyPlaces()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onCakeClick(cake: Cake) {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> {
                val bundle = Bundle().apply {
                    putParcelable(CustomizeFragment.ARG_CAKE, cake)
                }
                navController.navigate(
                    R.id.action_homeFragment_to_customizeFragment,
                    bundle,
                    slideRightNavOptions()
                )
            }
            R.id.favoritesFragment -> {
                val bundle = Bundle().apply {
                    putParcelable(CustomizeFragment.ARG_CAKE, cake)
                }
                navController.navigate(
                    R.id.action_favoritesFragment_to_customizeFragment3,
                    bundle,
                    slideRightNavOptions()
                )
            }
        }
    }

    override fun onCakeAddClick(cake: Cake) {
        viewModel.updateCurrentCartOrder(cake, change = CartItemChange.Increment)
    }

    override fun onCakeSetFavorite(cake: Cake) {
        val favoriteRef = Firebase.firestore.collection(USERS)
            .document(Firebase.auth.currentUser?.uid.orEmpty())
            .collection("favorites")
            .document(cake.id)

        val task = if (cake.isFavorite) {
            favoriteRef.set(cake)
        } else {
            favoriteRef.delete()
        }

        task.addOnSuccessListener {
            viewModel.insertCake(cake)
        }.addOnFailureListener {
            viewModel.setCurrentError(it)
        }
    }


    fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
    }



    override fun onBaseCakeClick(cakeMenuItem: CakeMenuItem) {
        //
    }

    override fun onBaseCakeLongClick(cakeMenuItem: CakeMenuItem) {
        //
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData) {
        val currentOrder = viewModel.currentCartOrder.value
        if (currentOrder != null) {
             if (razorpayPaymentId != null) {
                 val data = "${currentOrder.razorpayOrderId}|${razorpayPaymentId}"

                val generatedSignature =
                    Signature.generateHashWithHmac256(data, getString(R.string.razorpay_secret_key))
                if (paymentData.signature == generatedSignature) {

                    viewModel.setCurrentPaymentResult(Result.Success(false))

                    currentOrder.status = listOf(Paid)
                    viewModel.setCurrentCartOrder(currentOrder)

                    viewModel.confirmOrder(currentOrder.orderId, currentOrder.senderId, mapOf(
                        STATUS to listOf(
                            PAID
                        ), PAYMENT_ID to razorpayPaymentId), mapOf(
                        TOTAL_ORDER_COUNT to FieldValue.increment(1), TOTAL_SALES_AMOUNT to FieldValue.increment(currentOrder.prices.total)))
                } else {
                    Log.d(TAG, "Payment signature didn't match. [Original signature - ${paymentData.signature}, Generated Signature")
                }
            }
        } else {
            Log.d(TAG, "current order == null")
        }

    }

    override fun onPaymentError(errorCode: Int, p1: String?, paymentData: PaymentData?) {
        val errorMsg = when (errorCode) {
            Checkout.NETWORK_ERROR -> "There was a problem with the network. The transaction couldn't take place."
            Checkout.INVALID_OPTIONS -> "Invalid way of ordering."
            Checkout.PAYMENT_CANCELED -> "Payment cancelled by the user."
            Checkout.TLS_ERROR -> "Couldn't create a secure connection to perform the transaction."
            else -> "Something went wrong. If money was deducted wrongfully, report for refund."
        }

        viewModel.setCurrentPaymentResult(Result.Error(Exception(errorMsg)))

        findViewById<Button>(R.id.checkOutBtn)?.enable()
        findViewById<ProgressBar>(R.id.checkOutProgress)?.hide()

//        deleteCurrentOrderFromFirebase()

        /*val bundle = Bundle().apply {
            putBoolean(IS_PAYMENT_SUCCESSFUL, false)
        }

        lifecycleScope.launch {
            delay(500)
            if (navController.currentDestination?.id == R.id.cartFragment) {
                navController.navigate(R.id.paymentResultFragment, bundle)
            }
        }*/

    }

    override fun onAddItemClick(cartItem: CartItem) {
        viewModel.updateCurrentCartOrder(cartItem = cartItem, change = CartItemChange.Increment)
    }

    override fun onRemoveItemClick(cartItem: CartItem) {
        viewModel.updateCurrentCartOrder(cartItem = cartItem, change = CartItemChange.Decrement)
    }

    override fun onCustomizeClick(cartItem: CartItem) {
        val bundle = Bundle().apply {
            putParcelable(CustomizeFragment.ARG_CART_ITEM, cartItem)
            putBoolean(CustomizeFragment.ARG_IS_EDIT_MODE, true)
        }
        navController.navigate(
            R.id.action_cartFragment_to_customizeFragment2,
            bundle,
            slideRightNavOptions()
        )
    }

    private fun createNewOrderFromPreviousOrder(previousOrder: Order) : Order {
        previousOrder.orderId = randomId()
        previousOrder.status = listOf(Created)
        previousOrder.createdAt = System.currentTimeMillis()
        previousOrder.deliveryAt = System.currentTimeMillis() + (12 * 60 * 60 * 1000)

        for (item in previousOrder.items) {
            item.cartItemId = randomId()
            item.orderId = previousOrder.orderId
        }

        return previousOrder
    }

    override fun onPrimaryActionClick(vh: OrderViewHolder, order: Order) {
        if (order.status.first() == Delivered) {

            // creating new order from previous order
            val newOrder = createNewOrderFromPreviousOrder(order)
            viewModel.setCurrentCartOrder(newOrder)

            if (navController.currentDestination?.id == R.id.dashboardFragment) {
                binding.navView.selectedItemId = R.id.cart_navigation
            } else {
                navController.navigateUp()
                binding.navView.selectedItemId = R.id.cart_navigation
            }

        } else if (order.status.first() == Delivering) {
            val latitude = 25.560272
            val longitude = 91.904471

            val gmmIntentUri =
                Uri.parse("google.navigation:q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        vh.resetState()
    }

    override fun onSecondaryActionClick(vh: OrderViewHolder, order: Order) {
        if (order.status.first() != Delivered) {
            if (order.delivery) {
                Firebase.firestore.collection(USERS)
                    .document(order.deliveryExecutive!!)
                    .get()
                    .addOnSuccessListener {

                        vh.resetState()

                        if (it.exists()) {
                            val user = it.toObject(User::class.java)!!
                            val phoneNumber = user.phoneNo.split(" ")
                            if (phoneNumber.size > 1) {
                                val number = phoneNumber[1]
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Phone number not found.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }.addOnFailureListener {
                        vh.resetState()
                        viewModel.setCurrentError(it)
                    }
            } else {
                // call admin
                /*Toast.makeText(this, "Cannot call cause order is not delivery", Toast.LENGTH_SHORT)
					.show()*/
                vh.resetState()
            }
        } else {
            openFeedbackDialog(order.orderId)
            vh.resetState()

        }
    }

    override fun onCustomerClick(vh: OrderViewHolder, user: User) {

    }

    private fun openFeedbackDialog(orderId: String) {
        val feedbackView = layoutInflater.inflate(R.layout.feedback_layout, null, false)
        val feedbackLayoutBinding = FeedbackLayoutBinding.bind(feedbackView)

        if (orderFeedbacksTemp.containsKey(orderId)) {
            val oldFeedback = orderFeedbacksTemp[orderId]!!

            feedbackLayoutBinding.orderFeedbackTextLayout.editText?.setText(oldFeedback.content)
            feedbackLayoutBinding.orderRatingBar.rating = oldFeedback.rating
        }

        showDialog("Feedback", "Write your views on this order ... ", extraView = feedbackView, positiveBtn = "Submit", onPositiveBtnClick = {
            val rating = feedbackLayoutBinding.orderRatingBar.rating
            val feedbackContent = feedbackLayoutBinding.orderFeedbackTextLayout.editText?.text

            if (feedbackContent.isNullOrBlank() || feedbackContent.length < 10) {
                it.dismiss()
                Toast.makeText(
                    this@MainActivity,
                    "The feedback cannot be empty or too small.",
                    Toast.LENGTH_SHORT
                ).show()
                return@showDialog
            }

            val feedbackDocRef = Firebase.firestore
                .collection("feedbacks")
                .document(orderId)

            val feedback = Feedback(orderId, rating, feedbackContent.toString(), viewModel.repo.firebaseUtility.uid, System.currentTimeMillis())

            feedbackDocRef.set(feedback)
                .addOnSuccessListener {
                    orderFeedbacksTemp[orderId] = feedback
                    if (feedbackLayoutBinding.orderRatingBar.rating < 3f) {
                        Toast.makeText(this, "Sorry for the inconvenience! We will try to make our services better.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Thank you for submitting your valuable feedback.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, e.localizedMessage!!)
                }
        }, negativeBtn = "Cancel", onNegativeBtnClick = {
            it.dismiss()
        })
    }

    override fun onAnswerClick(faq: Faq) {
        // not needed in customer side
    }

    override fun onReviewClick(faq: Faq) {
        val binding = RateAnswerLayoutBinding.bind(layoutInflater.inflate(R.layout.rate_answer_layout, null, false))

        showDialog(extraView = binding.root, positiveBtn = "Submit", onPositiveBtnClick = {
            Firebase.firestore.collection("faq")
                .document(faq.id)
                .update(mapOf("rating" to binding.answerRatingBar.rating))
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Thanks for submitting your feedback.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnFailureListener {
                    viewModel.setCurrentError(it)
                }
        }, negativeBtn = "Cancel", onNegativeBtnClick = {
            it.dismiss()
        })

    }

    override fun onImageClick(view: View, image: String) {
        val bundle = Bundle().apply {
            putString(ImageViewFragment.ARG_IMAGE, image)
        }
        navController.navigate(R.id.action_adminHomeFragment_to_imageViewFragment, bundle)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val PRIMARY_CHANNEL_ID = "PRIMARY_CHANNEL_ID"
        private const val NOTIFICATION_ID = 14
    }

}