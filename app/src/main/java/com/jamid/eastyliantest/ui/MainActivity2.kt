package com.jamid.eastyliantest.ui

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener


class MainActivity2 : LocationAwareActivity(), CakeClickListener, OrderImageClickListener,
    PaymentResultWithDataListener, CartItemClickListener, OrderClickListener, FaqListener {

    private lateinit var navController: NavController
    private lateinit var repository: MainRepository
    private lateinit var binding: ActivityMain2Binding
    val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }
    private val orderFeedbacksTemp = mutableMapOf<String, Feedback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)

        setSupportActionBar(binding.mainToolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.containerFragment))
        binding.mainToolbar.setupWithNavController(navController, appBarConfiguration)

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

        viewModel.currentPlace.observe(this) {
            if (it != null) {
                Log.d(TAG, "Place is not null")
            } else {
                Log.d(TAG, "Place is null")
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
            navController.navigate(R.id.action_containerFragment_to_locationFragment4)
        }

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.containerFragment -> {
                    binding.mainAppbar.show()
                    binding.mainImage.hide()
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                    binding.navHostFragment.layoutParams = params

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

                    binding.mainNavigation.hide()
                }
                R.id.customizeFragment4 -> {
                    binding.currentLocationText.hide()
                    binding.mainImage.show()

                    binding.mainNavigation.hide()
                    binding.bottomCartAction.show()
                }
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
    }

    override fun onCakeClick(cake: Cake) {
        when (navController.currentDestination?.id) {
            R.id.containerFragment -> {
                val bundle = Bundle().apply {
                    putParcelable(CustomizeFragment.ARG_CAKE, cake)
                    putString("title", cake.baseName)
                }
                navController.navigate(
                    R.id.action_containerFragment_to_customizeFragment4,
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

    override fun onBaseCakeClick(cakeMenuItem: CakeMenuItem) {

    }

    override fun onBaseCakeLongClick(cakeMenuItem: CakeMenuItem) {

    }

    override fun onImageClick(view: View, image: String) {
        TODO("Not yet implemented")
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
        if (order.status.first() == OrderStatus.Delivered) {

            val tab = binding.mainNavigation.getTabAt(1)

            // creating new order from previous order
            val newOrder = createNewOrderFromPreviousOrder(order)
            viewModel.setCurrentCartOrder(newOrder)

            if (binding.mainNavigation.selectedTabPosition == 2) {
                tab?.select()
            } else {
                navController.navigateUp()
                tab?.select()
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

    override fun onCustomerClick(vh: OrderViewHolder, user: User) {

    }

}