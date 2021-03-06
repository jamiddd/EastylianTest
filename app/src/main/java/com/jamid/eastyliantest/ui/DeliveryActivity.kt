package com.jamid.eastyliantest.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.DeliveryPagerAdapter
import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.databinding.ActivityDeliveryBinding
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.interfaces.OrderClickListener
import com.jamid.eastyliantest.interfaces.OrderImageClickListener
import com.jamid.eastyliantest.model.*
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.toast
import com.jamid.eastyliantest.utility.updateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeliveryActivity : LocationAwareActivity(), OrderClickListener, OrderImageClickListener {

	private lateinit var repository: MainRepository
	val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }
	private lateinit var binding: ActivityDeliveryBinding

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.admin_home_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.admin_dash -> {
				val fragment = DeliveryDashboardFragment.newInstance()
				fragment.show(supportFragmentManager, DeliveryDashboardFragment.TAG)
			}
		}
		return super.onOptionsItemSelected(item)
	}

	private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		if (it.resultCode == Activity.RESULT_OK) {
			val image = it.data?.data
			viewModel.setCurrentImage(image)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityDeliveryBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initiate(binding.root)
		setSupportActionBar(binding.deliveryToolbar)

		val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
		repository = MainRepository.newInstance(database)

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

			binding.deliveryToolbar.updateLayout(marginTop = top)

			viewModel.windowInsets.postValue(Pair(top, bottom))
			insets
		}

		locationUtility.currentLocation.observe(this) { location ->
			if (location != null) {
				viewModel.gpsLocation.postValue(LatLng(location.latitude, location.longitude))
			}
			stopLocationUpdates()
		}

		getLastLocation()

		locationUtility.isLocationEnabled.observe(this) { isEnabled ->
			if (!isEnabled) {
				askUserToEnableLocation {
					onLocationSettingsRetrieved(it)
				}
			} else {
				if (locationUtility.isLocationPermissionAvailable.value == true) {
					requestingLocationUpdates = true
					getLastLocation()
				} else {
					requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
				}
			}
		}

		locationUtility.isLocationPermissionAvailable.observe(this) { isGranted ->
			if (isGranted) {
				requestingLocationUpdates = true
				if (locationUtility.isLocationEnabled.value == true) {
					getLastLocation()
				} else {
					askUserToEnableLocation {
						onLocationSettingsRetrieved(it)
					}
				}
			} else {
				requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
			}
		}

		lifecycleScope.launch {
			delay(500)
			binding.deliveryPager.adapter = DeliveryPagerAdapter(this@DeliveryActivity)

			TabLayoutMediator(binding.deliveryTabLayout, binding.deliveryPager) { a, b ->
				when (b) {
					0 -> a.text = "Preparing"
					1 -> a.text = "Deliveries"
				}
			}.attach()
		}

		Firebase.firestore.collection(RESTAURANT)
			.document(EASTYLIAN)
			.get()
			.addOnSuccessListener {
				if (it.exists()) {
					val restaurant = it.toObject(Restaurant::class.java)!!
					viewModel.insertRestaurantData(restaurant)
				}
			}.addOnFailureListener {
				toast("Something went wrong while fetching restaurant data.")
			}
	}

	fun selectImage() {
		val intent = Intent().apply {
			type = "image/*"
			action = Intent.ACTION_GET_CONTENT
		}
		selectImageLauncher.launch(intent)
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
				getLastLocation()
			}
		}
	}

	private fun getLastLocation() {
		locationUtility.getLastLocation {
			when (it) {
				is Result.Error -> {
					Log.d(TAG, it.exception.localizedMessage!!)
				}
				is Result.Success -> {
					locationUtility.updateCurrentLocation(it.data)
				}
			}
		}
	}

	companion object {
		private const val TAG = "DeliveryActivity"
	}

	override fun onPrimaryActionClick(vh: OrderViewHolder, order: Order) {

		val data = mutableMapOf<String, Any>()

		when (order.status[0]) {
			OrderStatus.Preparing -> {
				data[DELIVERY_EXECUTIVE] = Firebase.auth.currentUser!!.uid
				if (order.status.size > 1) {
					data[STATUS] = listOf(DELIVERING, DUE)
					order.status = listOf(OrderStatus.Delivering, OrderStatus.Due)
				} else {
					data[STATUS] = listOf(DELIVERING)
					order.status = listOf(OrderStatus.Delivering)
				}
			}
			OrderStatus.Delivering -> {
				if (order.status.size > 1) {
					data[STATUS] = listOf(DELIVERED, DUE)
					order.status = listOf(OrderStatus.Delivered, OrderStatus.Due)
				} else {
					data[STATUS] = listOf(DELIVERED)
					order.status = listOf(OrderStatus.Delivered)
				}
			}
			else -> throw IllegalStateException("Order status is in a state which is not allowed for a delivery executive.")
		}

		viewModel.updateOrder(order.orderId, order.senderId, data) {
			if (it.isSuccessful) {
				viewModel.insertOrder(order)

				if (order.status[0] == OrderStatus.Delivering) {
					if (order.delivery) {
						val latitude = order.place.latitude
						val longitude = order.place.longitude

						val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
						val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
						mapIntent.setPackage("com.google.android.apps.maps")
						startActivity(mapIntent)
					} else {
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
				}
			} else {
				it.exception?.let { e ->
					viewModel.setCurrentError(e)
				}
			}
		}

	}

	override fun onSecondaryActionClick(vh: OrderViewHolder, order: Order) {
		if (order.status[0] == OrderStatus.Delivering) {
			Firebase.firestore.collection(USERS)
				.document(order.senderId)
				.get()
				.addOnSuccessListener {
					if (it.exists()) {

						vh.resetState()

						val user = it.toObject(User::class.java)!!
						val phoneNumber = user.phoneNo.split(" ")
						if (phoneNumber.size > 1) {
							val number = phoneNumber[1]
							val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
							startActivity(intent)
						} else {
							toast("Phone number not found")
						}
					}
				}.addOnFailureListener {
					viewModel.setCurrentError(it)
				}
		} else
			throw IllegalStateException("Order status is in a state which is not allowed for a delivery executive.")
	}

	override fun onCustomerClick(vh: OrderViewHolder, user: User) {
		TODO("Not yet implemented")
	}


	override fun onImageClick(view: View, image: String) {
		/*val bundle = Bundle().apply {
			putString(ImageViewFragment.ARG_IMAGE, image)
		}
		navController.navigate(R.id.action_adminHomeFragment_to_imageViewFragment, bundle)*/
	}

}