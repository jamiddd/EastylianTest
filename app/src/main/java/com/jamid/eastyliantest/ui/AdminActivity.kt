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
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.CakeMiniAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.ActivityAdminBinding
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.interfaces.*
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.model.OrderStatus.*
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.*
import com.jamid.eastyliantest.views.zoomable.ImageViewFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.*

class AdminActivity : LocationAwareActivity(), OrderClickListener, CakeMiniListener, FaqListener, CakeClickListener, OrderImageClickListener, NotificationClickListener, RefundClickListener {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var navController: NavController
    private lateinit var repository: MainRepository
    private var previousVH: CakeMiniAdapter.CakeMiniViewHolder? = null
    private lateinit var notificationManager: NotificationManager

    val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val image = it.data?.data
            viewModel.setCurrentImage(image)
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

    private val notificationReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.extras?.get(TITLE) as String? ?: ""
            val content = intent?.extras?.get(BODY) as String? ?: ""
            val image = intent?.extras?.get(IMAGE) as String?

            val notifyBuilder = getNotificationBuilder(title, content, image)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.adminToolbar)

        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)

        createNotificationChannel()

        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, IntentFilter(NOTIFICATION_INTENT))

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.adminContainer) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.adminToolbar.setupWithNavController(navController, appBarConfiguration)

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
            Log.d(TAG, it.toString())
            viewModel.insertNearbyPlaces(it)
        }

        locationUtility.currentLocation.observe(this) { location ->
            if (location != null) {
                viewModel.gpsLocation.postValue(LatLng(location.latitude, location.longitude))
            }
            stopLocationUpdates()
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


        viewModel.repo.restaurant.observe(this) {
            if (it != null) {
                Log.d(TAG, "Invoking the observer.")
            }
        }

        Firebase.firestore.collection(RESTAURANT)
            .document(EASTYLIAN)
            .addSnapshotListener (this) { value, error ->
                if (error != null) {
                    error.localizedMessage?.let { msg ->
                        Log.e(TAG, msg)
                    }
                    return@addSnapshotListener
                }

                if (value != null && value.exists()) {
                    val restaurant = value.toObject(Restaurant::class.java)!!
                    viewModel.insertRestaurantData(restaurant)
                }

            }


        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.adminHomeFragment) {
                binding.adminTabLayout.show()
                binding.adminToolbar.setLogo(R.drawable.ic_logo_6_med)
                binding.adminToolbar.setTitleTextAppearance(this, R.style.TitleText)
            } else {
                binding.adminTabLayout.hide()
                binding.adminToolbar.logo = null
                binding.adminToolbar.setTitleTextAppearance(this, R.style.TitleTextNormal)
            }
        }

        Firebase.messaging.subscribeToTopic("admin")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    viewModel.setCurrentError(task.exception!!)
                } else {
                    Log.d(TAG, "Subscribing to :\"admin\": topic in FCM")
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

    override fun onPrimaryActionClick(vh: OrderViewHolder, order: Order) {

        val data = mutableMapOf<String, Any>()

        when (order.status[0]) {
            Due, Paid -> {
                if (order.status[0] == Due) {
                    data[STATUS] = listOf(PREPARING, DUE)
                } else {
                    data[STATUS] = listOf(PREPARING)
                }

                viewModel.updateOrder(order.orderId, order.senderId, data) {
                    if (it.isSuccessful) {
                        if (order.status[0] == Due) {
                            order.status = listOf(Preparing, Due)
                        } else {
                            order.status = listOf(Preparing)
                        }
                        viewModel.insertOrder(order)
                    } else {
                        it.exception?.let { e ->
                            viewModel.setCurrentError(e)
                        }
                    }

                }
            }
            Preparing -> {
                if (order.status.size > 1) {
                    data[STATUS] = listOf(DELIVERING, DUE)
                } else {
                    data[STATUS] = listOf(DELIVERING)
                }

                viewModel.updateOrder(order.orderId, order.senderId, data) {
                    if (it.isSuccessful) {
                        if (order.status.size > 1) {
                            order.status = listOf(Delivering, Due)
                        } else {
                            order.status = listOf(Delivering)
                        }
                        viewModel.insertOrder(order)

                        if (!order.delivery) {
                            Firebase.firestore.collection(USERS)
                                .document(order.senderId)
                                .get()
                                .addOnSuccessListener { it1 ->
                                    if (it1.exists()) {

                                        vh.resetState()

                                        val user = it1.toObject(User::class.java)!!
                                        val phoneNumber = user.phoneNo.split(" ")
                                        if (phoneNumber.size > 1) {
                                            val number = phoneNumber[1]
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                            startActivity(intent)
                                        } else {
                                            toast("Phone number not found")
                                        }
                                    }
                                }.addOnFailureListener { ex ->
                                    viewModel.setCurrentError(ex)
                                }
                        }
                    } else {
                        it.exception?.let { e ->
                            viewModel.setCurrentError(e)
                        }
                    }
                }
            }
            Delivering -> {
                if (order.status.size > 1) {
                    data[STATUS] = listOf(DELIVERED, DUE)
                } else {
                    data[STATUS] = listOf(DELIVERED)
                }

                viewModel.updateOrder(order.orderId, order.senderId, data) {
                    if (it.isSuccessful) {
                        if (order.status.size > 1) {
                            order.status = listOf(Delivered, Due)
                        } else {
                            order.status = listOf(Delivered)
                        }
                        viewModel.insertOrder(order)
                    } else {
                        it.exception?.let { e ->
                            viewModel.setCurrentError(e)
                        }
                    }
                }
            }
            Delivered -> {
                data[TOTAL_ORDER_COUNT] = FieldValue.increment(1)
                data[TOTAL_SALES_AMOUNT] = FieldValue.increment(order.prices.total)

                // no callback function .. might add later.
                viewModel.confirmOrder(order.orderId, order.senderId, mapOf(STATUS to listOf(Delivered), PAYMENT_ID to CASH_ON_DELIVERY), data) {
                    if (it.isSuccessful) {
                        order.paymentId = CASH_ON_DELIVERY
                        order.status = listOf(Delivered)
                        viewModel.insertOrder(order)
                    } else {
                        it.exception?.let { e -> // 6003869493
                            viewModel.setCurrentError(e)
                        }
                    }
                }

                vh.resetState()

            }
            else -> throw IllegalStateException("Order status is in a state which is not allowed for an Admin.")
        }
    }

    override fun onSecondaryActionClick(vh: OrderViewHolder, order: Order) {
        if (order.status.first() == Paid) {
            lifecycleScope.launch (Dispatchers.IO) {
                /*RazorpayUtility.initiateRefund(order) {
                    vh.resetState()
                    if (it.isSuccessful) {
                        order.status = listOf(Cancelled)
                        viewModel.insertOrder(order)
                    } else {
                        it.exception?.localizedMessage?.let { it1 ->
                            Log.d(TAG, it1)
                        }
                    }
                }*/

                val refund = Refund(randomId(), order.orderId, order.senderId, order.paymentId, order.prices.total, "created")
                viewModel.createRefundRequest(refund) {
                    vh.resetState()
                    if (it.isSuccessful) {
                        order.status = listOf(Cancelled)
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
        } else {
            order.status = listOf(Cancelled)
            viewModel.updateOrder(order.orderId, order.senderId, mapOf("status" to listOf(CANCELLED))) {
                viewModel.insertOrder(order)
            }
        }
    }

    override fun onCustomerClick(vh: OrderViewHolder, user: User) {
        val phoneNumber = user.phoneNo.split(" ")
        if (phoneNumber.size > 1) {
            val number = phoneNumber[1]
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
        } else {
            toast("Phone number not found")
        }
    }

	fun selectImage() {
		val intent = Intent().apply {
            type = "image/*"
		    action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
	}

	companion object {
        private const val TAG = "AdminActivity"
        private const val PRIMARY_CHANNEL_ID = "PRIMARY_CHANNEL_ID"
        private const val NOTIFICATION_ID = 15
    }

    override fun onCakeAvailabilityChange(cake: Cake) {
        Firebase.firestore.collection(CAKES)
            .document(cake.id)
            .update(mapOf(AVAILABLE to cake.available))
            .addOnSuccessListener {
                Log.d(TAG, "Updated cake availability status.")
            }.addOnFailureListener {
                Log.e(TAG, it.localizedMessage!!)
            }
    }

    override fun onCakeDelete(cake: Cake) {
        viewModel.deleteCake(cake.id) {
            if (it.isSuccessful) {
                Log.d(TAG, "Deleted cake with cake id : ${cake.id}")
            } else {
                it.exception?.let { e ->
                    viewModel.setCurrentError(e)
                }
            }
        }
    }

    override fun onCakeUpdate(cake: Cake) {
        val bundle = Bundle().apply {
            putParcelable(CAKE, cake)
            putBoolean(IS_EDIT_MODE, true)
            putString("title", "Update Cake")
        }
        navController.navigate(R.id.addCakeFragment, bundle, slideRightNavOptions())
    }

    override fun onContextActionMode(vh:CakeMiniAdapter.CakeMiniViewHolder, cake: Cake) {
        previousVH?.inactiveBackground()
        vh.activeBackground()

        val popupMenu = PopupMenu(this, vh.view)
        popupMenu.inflate(R.menu.cake_item_menu)

        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.change -> {
                    val bundle = Bundle().apply {
                        putParcelable(CAKE, cake)
                        putBoolean(IS_EDIT_MODE, true)
                        putString("title", "Update Cake")
                    }
                    navController.navigate(R.id.addCakeFragment, bundle, slideRightNavOptions())
                }
                R.id.delete -> {
                    showDialog(
                        title = "Deleting cake ..",
                        message ="Are you sure you want to delete this item?",
                        positiveBtn = "Delete",
                        negativeBtn = "Cancel", {
                            viewModel.deleteCake(cake.id) { it1 ->
                                if (it1.isSuccessful) {
                                    toast("Cake removed")
                                    Log.d(TAG, "Deleted cake with cake id : ${cake.id}")
                                } else {
                                    it1.exception?.let { e ->
                                        viewModel.setCurrentError(e)
                                    }
                                }
                            }
                        }, { it1 ->
                            it1.dismiss()
                        })
                }
            }
            true
        }

        popupMenu.show()
        previousVH = vh

//        viewModel.contextModeState.postValue(ContextObject(true, cake))
    }

    override fun onClick(vh: CakeMiniAdapter.CakeMiniViewHolder, cake: Cake) {
        if (vh == previousVH) {
            actionModeCallback.actionMode?.finish()
            return
        }
        if (actionModeCallback.actionMode != null) {
            previousVH?.inactiveBackground()
            vh.activeBackground()
            previousVH = vh
            viewModel.contextModeState.postValue(ContextObject(true, cake))
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {

        var actionMode: ActionMode? = null

        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater? = mode?.menuInflater
            actionMode = mode
            inflater?.inflate(R.menu.context_menu, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.delete -> {
                    val contextObject = viewModel.contextModeState.value
                    if (contextObject != null) {
                        val cake = contextObject.cake

                        showDialog(
                            title = "Deleting cake ..",
                            message ="Are you sure you want to delete this item?",
                            positiveBtn = "Delete",
                            negativeBtn = "Cancel", {
                            viewModel.deleteCake(cake.id) {
                                if (it.isSuccessful) {
                                    toast("Cake removed")
                                    Log.d(TAG, "Deleted cake with cake id : ${cake.id}")
                                } else {
                                    it.exception?.let { e ->
                                        viewModel.setCurrentError(e)
                                    }
                                }
                            }
                        }, {
                            it.dismiss()
                        })

                    }

                    mode?.finish()
                    true
                }
                R.id.change -> {
                    val contextObject = viewModel.contextModeState.value
                    if (contextObject != null) {
                        val bundle = Bundle().apply {
                            putParcelable(CAKE, contextObject.cake)
                            putBoolean(IS_EDIT_MODE, true)
                        }
                        navController.navigate(R.id.addCakeFragment, bundle, slideRightNavOptions())
                    }
                    mode?.finish()
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            viewModel.contextModeState.postValue(ContextObject(false, Cake()))
        }
    }

    override fun onAnswerClick(faq: Faq) {
        val fragment = AnswerSheetFragment.newInstance(faq)
        fragment.show(supportFragmentManager, AnswerSheetFragment.TAG)
    }

    override fun onReviewClick(faq: Faq) {

    }

    override fun onCakeClick(cake: Cake) {

    }

    override fun onCakeAddClick(cake: Cake) {

    }

    override fun onCakeSetFavorite(cake: Cake) {

    }

    override fun onBaseCakeClick(cakeMenuItem: CakeMenuItem) {
        val bundle = Bundle().apply {
            putParcelable("cakeMenuItem", cakeMenuItem)
            putString("title", "Update Item")
        }
        navController.navigate(R.id.action_changeMenuFragment_to_addMenuItemFragment, bundle, slideRightNavOptions())
    }

    override fun onBaseCakeLongClick(cakeMenuItem: CakeMenuItem) {
        showDialog("Remove item", "Are you sure you want to delete this menu item?", "Delete", "Cancel", {
            viewModel.removeCakeMenuItem(cakeMenuItem)
        }, {
            it.dismiss()
        })
    }

    override fun onImageClick(view: View, image: String) {
        val bundle = Bundle().apply {
            putString(ImageViewFragment.ARG_IMAGE, image)
        }
        navController.navigate(R.id.action_adminHomeFragment_to_imageViewFragment, bundle)
    }

    override fun onNotificationResend(notification: SimpleNotification) {
        viewModel.resendNotification(notification)
    }

    override fun onNotificationClick(notification: SimpleNotification) {
        val bundle = bundleOf("notification" to notification)
        navController.navigate(R.id.action_notificationsFragment_to_addNotificationFragment, bundle, slideRightNavOptions())
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
                        refund.status = status.lowercase(Locale.getDefault())
                        viewModel.insertRefund(refund)
                    }.addOnFailureListener {
                        toast("Something went wrong. " + it.localizedMessage.orEmpty())
                    }
            }.show()

    }

}