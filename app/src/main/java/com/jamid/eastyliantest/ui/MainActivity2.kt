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
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.ActivityMain2Binding
import com.jamid.eastyliantest.databinding.FeedbackLayoutBinding
import com.jamid.eastyliantest.databinding.RateAnswerLayoutBinding
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.interfaces.*
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.*
import com.jamid.eastyliantest.views.zoomable.ImageViewFragment
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.util.*


class MainActivity2 : LocationAwareActivity(), CakeClickListener, OrderImageClickListener,
    PaymentResultWithDataListener, CartItemClickListener, OrderClickListener, FaqListener, OnPaymentModeSelected, RefundClickListener {

    private lateinit var navController: NavController
    private lateinit var repository: MainRepository
    private lateinit var binding: ActivityMain2Binding
    private lateinit var notificationManager: NotificationManager

    val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }
    private val orderFeedbacksTemp = mutableMapOf<String, Feedback>()

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val image = it.data?.data
            viewModel.setCurrentImage(image)
        } else {
            viewModel.setCurrentImage(null)
        }
    }

    private fun onUPIResultReceived(result: String) {
        val abstractKeyValuePairs = result.split('&')
        val resultMap = mutableMapOf<String, String>()
        for (abstractPair in abstractKeyValuePairs) {
            val keyValuePair = abstractPair.split('=')
            if (keyValuePair.size > 1) {
                resultMap[keyValuePair[0]] = keyValuePair[1]
            }
        }

        if (resultMap.containsKey("Status") || resultMap.containsKey("status")) {
            val successArray = listOf("success", "Success", "SUCCESS")
            val failureArray = listOf("failure", "Failure", "FAILURE")
            val statusResult = resultMap["Status"]
            when {
                successArray.contains(statusResult) -> {
                    val currentOrder = viewModel.currentCartOrder.value
                    val paymentId = resultMap["txnId"]
                    if (currentOrder != null) {
                        currentOrder.status = listOf(OrderStatus.Paid)

                        // uploading order only after successful payment
                        currentOrder.createdAt = System.currentTimeMillis()
                        viewModel.uploadOrder(currentOrder) { it1 ->
                            if (it1.isSuccessful) {
                                viewModel.setCurrentPaymentResult(Result.Success(false))
                                // previously used signed methods
                                viewModel.confirmOrder(currentOrder.orderId, currentOrder.senderId, mapOf(
                                    STATUS to listOf(
                                        PAID
                                    ), PAYMENT_ID to paymentId.orEmpty()), mapOf(
                                    TOTAL_ORDER_COUNT to FieldValue.increment(1), TOTAL_SALES_AMOUNT to FieldValue.increment(currentOrder.prices.total)))
                            } else {
                                viewModel.setCurrentPaymentResult(Result.Error(Exception("Couldn't upload or create order.")))
                                Log.d(TAG, it1.exception?.localizedMessage.orEmpty())

                                val refund = Refund(randomId(), currentOrder.orderId, currentOrder.senderId, paymentId.orEmpty(), currentOrder.prices.total, "created")
                                viewModel.createRefundRequest(refund) { it2 ->
                                    if (it2.isSuccessful) {
                                        toast("Order couldn't be created due to some error. A refund has been initiated if any money was deducted from your bank account.")
                                    } else {
                                        Snackbar.make(binding.root, "Something went wrong. Contact us if money has been deducted from your account.", Snackbar.LENGTH_INDEFINITE).show()
                                    }
                                }
                            }
                        }
                    }
                }
                failureArray.contains(statusResult) -> {
                    viewModel.setCurrentPaymentResult(Result.Error(Exception("Transaction Failed")))
                }
                else -> {
                    viewModel.setCurrentPaymentResult(Result.Error(Exception("Unknown Error")))
                }
            }
        } else {
            viewModel.setCurrentPaymentResult(Result.Error(Exception("Transaction failed or couldn't complete.")))
        }

        navController.navigate(R.id.action_containerFragment_to_paymentResultFragment2, null, slideRightNavOptions())

    }

    private val paymentIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data
            if (data != null) {
                data.getStringExtra("response")?.let { it1 ->
                    onUPIResultReceived(it1)
                }
            } else {
                toast("Transaction failed. Unknown error")
            }
        } else {
            toast("Payment cancelled.")
        }
    }

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
            .setGraph(R.navigation.main_navigation_new)
            .setDestination(R.id.containerFragment)
            .setArguments(bundleOf("page" to 3))
            .createPendingIntent()

        val notificationBuilder = NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)

        if (image != null) {
            notificationBuilder.applyImageUrl(image)
        }

        return notificationBuilder.setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_stat_cake_1)
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)

        setSupportActionBar(binding.mainToolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.mainCollapse.setupWithNavController(binding.mainToolbar, navController, appBarConfiguration)

        viewModel.getCakes()

        viewModel.getPastOrders()

        locationUtility.getNearbyPlaces()

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

        binding.coItemCount.setFactory {
            TextView(this).apply {
                setTextAppearance(android.R.style.TextAppearance_Material_Medium)
                setTextColor(ContextCompat.getColor(this@MainActivity2, R.color.white))
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(tokenReceiver, IntentFilter("tokenIntent"))
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(NOTIFICATION_INTENT))

        viewModel.currentCartOrder.observe(this) { currentOrder ->
            if (currentOrder != null) {

                if (binding.mainNavigation.selectedItemId != R.id.home_nav_2) {
                    binding.viewCartCard.slideReset()
                }

                val inAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
                val outAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)

                inAnim.duration = 150
                outAnim.duration = 150

                binding.coItemCount.inAnimation = inAnim
                binding.coItemCount.outAnimation = outAnim

                if (currentOrder.items.size == 1) {
                    binding.coItemCount.setText("${currentOrder.items.size} Item")
                } else {
                    binding.coItemCount.setText("${currentOrder.items.size} Items")
                }

            } else {
                Log.d(TAG, "Current order is null")
                binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
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

        viewModel.repo.currentUser.observe(this) { currentUser ->
            if (currentUser != null) {
                addCurrentOrdersListener(currentUser)
            }
        }

        viewModel.currentPlace.observe(this) {
            if (it == null) {
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(R.id.action_containerFragment_to_locationFragment4)
            }
        }

        viewModel.currentPlace.observe(this) {
            if (it != null) {
                if (it.name.isNotBlank()) {
                    binding.currentLocationText.text = it.name
                } else {
                    binding.currentLocationText.text = getString(R.string.no_location_text)
                }
            } else {
                binding.currentLocationText.text = getString(R.string.no_location_text)
            }
        }

        binding.currentLocationText.setOnClickListener {
            navController.navigate(R.id.action_containerFragment_to_locationFragment4, null, slideRightNavOptions())
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->

            if (destination.id != R.id.containerFragment) {
                binding.mainCollapse.setExpandedTitleTextAppearance(R.style.TitleTextExpandedNormal)
                binding.mainCollapse.setCollapsedTitleTextAppearance(R.style.TitleTextNormal)
                binding.mainToolbar.logo = null

                binding.viewCartCard.slideDown(convertDpToPx(150).toFloat())
            } else {
                binding.mainToolbar.setLogo(R.drawable.ic_logo_6_med)
                binding.mainCollapse.setExpandedTitleTextAppearance(R.style.TitleTextExpanded)
                binding.mainCollapse.setCollapsedTitleTextAppearance(R.style.TitleText)

                if (viewModel.currentCartOrder.value != null) {
                    binding.viewCartCard.slideReset()
                }

                binding.viewCartCard.setOnClickListener {
                    binding.mainNavigation.selectedItemId = R.id.home_nav_2
                }

                if (viewModel.shouldCheckAccount) {
                    lifecycleScope.launch {
                        delay(500)
                        binding.mainNavigation.selectedItemId = R.id.home_nav_3
                        viewModel.shouldCheckAccount = false
                        delay(500)
                        val v = findViewById<View>(R.id.currentOrdersHeader)
                        val co = intArrayOf(0, 0)
                        v.getLocationOnScreen(co)

                        val scroll = findViewById<NestedScrollView>(R.id.accountScroll)
                        scroll?.smoothScrollTo(co[0], co[1] - binding.mainAppbar.measuredHeight)
                    }
                }

            }

            when (destination.id) {
                R.id.containerFragment -> {
                    binding.mainAppbar.show()
                    binding.mainImage.hide()
                    binding.mainNavigation.show()
                    binding.bottomCartAction.hide()
                    binding.currentLocationText.show()
                }
                R.id.locationFragment4 -> {
                    binding.mainAppbar.setExpanded(false, false)

                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.layoutParams = params

                    binding.mainAppbar.hide()
                    binding.bottomCartAction2.hide()

                    binding.mainNavigation.hide()
                }
                R.id.customizeFragment4 -> {
                    binding.mainAppbar.show()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params

                    binding.currentLocationText.hide()
                    binding.mainImage.show()

                    binding.mainNavigation.hide()
                    binding.bottomCartAction.show()
                    binding.bottomCartAction2.hide()
                }
                R.id.addressFragment2 -> {
                    binding.currentLocationText.hide()
                    binding.mainImage.hide()

                    binding.mainAppbar.show()
                    binding.mainAppbar.setExpanded(false)

                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()

                    lifecycleScope.launch {
                        delay(500)
                        findViewById<NestedScrollView>(R.id.addressScroll)?.isNestedScrollingEnabled = false
                    }
                }
                R.id.refundFragment3 -> {
                    binding.currentLocationText.hide()
                    binding.mainImage.hide()

                    binding.mainAppbar.setExpanded(false)
                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()

                    lifecycleScope.launch {
                        delay(500)
                        findViewById<NestedScrollView>(R.id.refundScroll)?.isNestedScrollingEnabled = false
                    }
                }
                R.id.helpFragment2 -> {
                    binding.currentLocationText.hide()
                    binding.mainImage.hide()

                    binding.mainAppbar.setExpanded(false)

                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()

                    lifecycleScope.launch {
                        delay(500)
                        findViewById<NestedScrollView>(R.id.helpScroll)?.isNestedScrollingEnabled = false
                    }
                }
                R.id.contactFragment3 -> {
                    binding.currentLocationText.hide()
                    binding.mainImage.hide()

                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()

                    lifecycleScope.launch {
                        delay(500)
                        findViewById<NestedScrollView>(R.id.contactScroll)?.isNestedScrollingEnabled = false
                    }
                }
                R.id.paymentResultFragment2 -> {
                    binding.mainAppbar.hide()
                    binding.bottomCartAction2.hide()
                    binding.mainNavigation.hide()
                }
                R.id.pastOrdersFragment2 -> {
                    binding.currentLocationText.hide()
                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()
                }
                R.id.imageViewFragment2 -> {
                    binding.currentLocationText.hide()
                    binding.mainNavigation.hide()
                    binding.bottomCartAction.hide()
                }
            }
        }

        viewModel.repo.restaurant.observe(this) {
            if (it != null) {
                Log.d(TAG, "Invoking the observer.")
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

        Firebase.messaging.subscribeToTopic(GENERAL)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    viewModel.setCurrentError(task.exception!!)
                } else {
                    Log.d(TAG, "Subscribing to :\"General\": topic in FCM")
                }
            }

        val preDefinedPage = intent.extras?.getInt("page")
        if (preDefinedPage != null) {
            if (preDefinedPage == 3) {
                binding.mainNavigation.selectedItemId = R.id.home_nav_3
            }
        }

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

    companion object {
        private const val TAG = "MainActivity2"
        private const val PRIMARY_CHANNEL_ID = "PRIMARY_CHANNEL_ID"
        private const val NOTIFICATION_ID = 14
    }

    override fun onCakeClick(cake: Cake) {
        when (navController.currentDestination?.id) {
            R.id.containerFragment -> {
                val bundle = Bundle().apply {
                    putParcelable(CustomizeFragment.ARG_CAKE, cake)
                    putString("title", cake.baseName)
                }
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(
                    R.id.action_containerFragment_to_customizeFragment4,
                    bundle,
                    slideRightNavOptions()
                )
            }
            R.id.favoritesFragment -> {
                val bundle = Bundle().apply {
                    putParcelable(CustomizeFragment.ARG_CAKE, cake)
                }
                Navigation.findNavController(this, R.id.nav_host_fragment).navigate(
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

    override fun onBaseCakeClick(cakeMenuItem: CakeMenuItem) {

    }

    override fun onBaseCakeLongClick(cakeMenuItem: CakeMenuItem) {

    }

    override fun onImageClick(view: View, image: String) {
        val bundle = Bundle().apply {
            putString(ImageViewFragment.ARG_IMAGE, image)
        }
        val controller = Navigation.findNavController(this, R.id.nav_host_fragment)
        when (controller.currentDestination?.id) {
            R.id.pastOrdersFragment2 -> {
                controller.navigate(R.id.action_pastOrdersFragment2_to_imageViewFragment2, bundle)
            }
            R.id.containerFragment -> {
                controller.navigate(R.id.action_containerFragment_to_imageViewFragment2, bundle)
            }
        }
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        val currentOrder = viewModel.currentCartOrder.value
        if (currentOrder != null) {
            if (p0 != null) {
                val data = "${currentOrder.razorpayOrderId}|${p0}"

                val generatedSignature =
                    Signature.generateHashWithHmac256(data, getString(R.string.razorpay_secret_key))
                if (p1?.signature == generatedSignature) {

                    viewModel.setCurrentPaymentResult(Result.Success(false))

                    currentOrder.status = listOf(OrderStatus.Paid)
                    viewModel.setCurrentCartOrder(currentOrder)

                    viewModel.confirmOrder(currentOrder.orderId, currentOrder.senderId, mapOf(
                        STATUS to listOf(
                            PAID
                        ), PAYMENT_ID to p0), mapOf(
                        TOTAL_ORDER_COUNT to FieldValue.increment(1), TOTAL_SALES_AMOUNT to FieldValue.increment(currentOrder.prices.total)))
                } else {
                    Log.d(TAG, "Payment signature didn't match. [Original signature - ${p1?.signature}, Generated Signature")
                }
            }
        } else {
            Log.d(TAG, "current order == null")
        }
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        val errorMsg = when (p0) {
            Checkout.NETWORK_ERROR -> "There was a problem with the network. The transaction couldn't take place."
            Checkout.INVALID_OPTIONS -> "Invalid way of ordering."
            Checkout.PAYMENT_CANCELED -> "Payment cancelled by the user."
            Checkout.TLS_ERROR -> "Couldn't create a secure connection to perform the transaction."
            else -> "Something went wrong. If money was deducted wrongfully, report for refund."
        }

        viewModel.setCurrentPaymentResult(Result.Error(Exception(errorMsg)))

        findViewById<Button>(R.id.checkOutBtn)?.enable()
        findViewById<ProgressBar>(R.id.checkOutProgress)?.hide()
    }

    override fun onAddItemClick(cartItem: CartItem) {
        viewModel.shouldUpdateCart = true
        viewModel.updateCurrentCartOrder(cartItem = cartItem, change = CartItemChange.Increment)
    }

    override fun onRemoveItemClick(cartItem: CartItem) {
        viewModel.shouldUpdateCart = true
        viewModel.updateCurrentCartOrder(cartItem = cartItem, change = CartItemChange.Decrement)
    }

    override fun onCustomizeClick(cartItem: CartItem) {
        val bundle = Bundle().apply {
            putParcelable(CustomizeFragment.ARG_CART_ITEM, cartItem)
            putBoolean(CustomizeFragment.ARG_IS_EDIT_MODE, true)
        }
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.navigate(
            R.id.action_containerFragment_to_customizeFragment4,
            bundle,
            slideRightNavOptions()
        )
    }

    override fun onAnswerClick(faq: Faq) {

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

    private fun createNewOrderFromPreviousOrder(previousOrder: Order) : Order {
        previousOrder.orderId = randomId()
        previousOrder.status = listOf(OrderStatus.Created)
        previousOrder.createdAt = System.currentTimeMillis()
        previousOrder.deliveryAt = System.currentTimeMillis() + (12 * 60 * 60 * 1000)

        for (item in previousOrder.items) {
            item.cartItemId = randomId()
            item.orderId = previousOrder.orderId
        }

        return previousOrder
    }

    override fun onPrimaryActionClick(vh: OrderViewHolder, order: Order) {
        if (order.status.first() == OrderStatus.Paid) {
            val refund = Refund(randomId(), order.orderId, order.senderId, order.paymentId, order.prices.total, "created")
            viewModel.createRefundRequest(refund) {
                vh.resetState()
                if (it.isSuccessful) {
                    order.status = listOf(OrderStatus.Cancelled)
                    viewModel.updateOrder(order.orderId, order.senderId, mapOf("status" to listOf(
                        CANCELLED))) {
                        viewModel.insertOrder(order)
                    }
                } else {
                    it.exception?.localizedMessage?.let { it1 ->
                        Log.d(TAG, it1)
                    }
                }
            }
        }

        if (order.status.first() == OrderStatus.Due) {
            vh.resetState()
            order.status = listOf(OrderStatus.Cancelled)
            viewModel.updateOrder(order.orderId, order.senderId, mapOf("status" to listOf(
                CANCELLED))) {
                viewModel.insertOrder(order)
            }
        }

        if (order.status.first() == OrderStatus.Delivered) {
            // creating new order from previous order
            val newOrder = createNewOrderFromPreviousOrder(order)
            viewModel.setCurrentCartOrder(newOrder)

            if (navController.currentDestination?.id == R.id.containerFragment) {
                binding.mainNavigation.selectedItemId = R.id.home_nav_2
            } else {
                navController.navigateUp()
                lifecycleScope.launch {
                    delay(500)
                    binding.mainNavigation.selectedItemId = R.id.home_nav_2
                }
            }


        } else if (order.status.first() == OrderStatus.Delivering) {
            val latitude = 25.560272
            val longitude = 91.904471

            val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        vh.resetState()
    }

    override fun onSecondaryActionClick(vh: OrderViewHolder, order: Order) {
        if (order.status.first() != OrderStatus.Delivered) {
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
                vh.resetState()
            }
        } else {
            openFeedbackDialog(order.orderId)
            vh.resetState()

        }
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
                    this@MainActivity2,
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

    private fun startPayment(uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = uri
        val chooser = Intent.createChooser(intent, "Pay with...")
        paymentIntentLauncher.launch(chooser)

    }

    override fun onCustomerClick(vh: OrderViewHolder, user: User) {

    }

    fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
    }

    override fun onCashOnDeliverySelected() {
        val currentOrder = viewModel.currentCartOrder.value
        if (currentOrder != null) {
            currentOrder.paymentMethod = COD
            currentOrder.status = listOf(OrderStatus.Due)
            viewModel.setCurrentCartOrder(currentOrder)
            navController.navigate(R.id.action_containerFragment_to_paymentResultFragment2, null, slideRightNavOptions())

            viewModel.uploadOrder(currentOrder) {
                if (it.isSuccessful) {
                    viewModel.setCurrentPaymentResult(Result.Success(true))
                } else {
                    it.exception?.let { e ->
                        viewModel.setCurrentError(e)
                    }
                }
            }
        }
    }

    override fun onUpiSelected(amount: String) {

        val tr = randomId()

        val intentUri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", "EASTYLIAN.49075420@hdfcbank")
            .appendQueryParameter("pn", "EASTYLIAN")
            .appendQueryParameter("mc", "TQ8243")
            .appendQueryParameter("tr", tr)
            .appendQueryParameter("tn", "With love from Eastylian")
            /*.appendQueryParameter("am", amount) For real payments */
            .appendQueryParameter("am", "1.00")
            .appendQueryParameter("cu", "INR")
            .build()

        startPayment(intentUri)
    }

    override fun onUpdateBtnClick(refund: Refund, user: User) {

        val choices = arrayOf("Processing", "Processed")

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Status")
            .setItems(choices) { _, which ->
                val status = choices[which]
                Firebase.firestore.collection(USERS).document(refund.receiverId)
                    .collection(REFUNDS)
                    .document(refund.refundId)
                    .update(mapOf("status" to status))
                    .addOnSuccessListener {
                        refund.status = status
                        viewModel.insertRefund(refund)
                    }.addOnFailureListener {
                        toast("Something went wrong. " + it.localizedMessage.orEmpty())
                    }
            }.show()

    }

}