package com.jamid.eastyliantest.ui

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
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
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.model.Result
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.updateLayout

class DeliveryActivity : LocationAwareActivity(), OrderClickListener {

	private lateinit var repository: MainRepository
	val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }
	private lateinit var binding: ActivityDeliveryBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityDeliveryBinding.inflate(layoutInflater)
		setContentView(binding.root)
		initiate(binding.root)

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

		binding.deliveryPager.adapter = DeliveryPagerAdapter(this)

		TabLayoutMediator(binding.deliveryTabLayout, binding.deliveryPager) { a, b ->
			when (b) {
				0 -> a.text = "Preparing"
				1 -> a.text = "Deliveries"
			}
		}.attach()

		/*val bottomSheetBehavior = BottomSheetBehavior.from(binding.deliveryExecutiveDashboard)
		bottomSheetBehavior.isHideable = true
		bottomSheetBehavior.peekHeight = 0
		bottomSheetBehavior.skipCollapsed = true*/

		/*bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
			override fun onStateChanged(bottomSheet: View, newState: Int) {

			}

			override fun onSlide(bottomSheet: View, slideOffset: Float) {

			}
		})*/

		binding.deliveryDashboardBtn.setOnClickListener {
			/*if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
				bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
			} else {
				bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
			}*/
			val fragment = DeliveryDashboardFragment.newInstance()
			fragment.show(supportFragmentManager, DeliveryDashboardFragment.TAG)
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
					val latitude = order.place.latitude
					val longitude = order.place.longitude

					val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
					val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
					mapIntent.setPackage("com.google.android.apps.maps")
					startActivity(mapIntent)
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
							Toast.makeText(this, "Phone number not found.", Toast.LENGTH_SHORT)
								.show()
						}
					}
				}.addOnFailureListener {
					viewModel.setCurrentError(it)
				}
		} else
			throw IllegalStateException("Order status is in a state which is not allowed for a delivery executive.")
	}

	override fun onSelectDirection(order: Order) {

	}

}