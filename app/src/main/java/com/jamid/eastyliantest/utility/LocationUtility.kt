package com.jamid.eastyliantest.utility

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.GeoPoint
import com.jamid.eastyliantest.model.Result
import com.jamid.eastyliantest.model.SimplePlace

class LocationUtility(context: Context) {

	private var mContext: Context = context
	private var mFusedLocationProviderClient: FusedLocationProviderClient =
		FusedLocationProviderClient(mContext)
	private var geoCoder: Geocoder = Geocoder(mContext)
	private var placesClient: PlacesClient = Places.createClient(mContext)

	private val _currentLocation = MutableLiveData<Location>().apply { value = null }
	val currentLocation: LiveData<Location> = _currentLocation

	private val _locationErrors = MutableLiveData<Exception>().apply { value = null }
//	val locationErrors: LiveData<Exception> = _locationErrors

	private val _nearbyPlaces = MutableLiveData<List<SimplePlace>>().apply { value = emptyList() }
	val nearbyPlaces: LiveData<List<SimplePlace>> = _nearbyPlaces

	private val _isLocationEnabled = MutableLiveData<Boolean>()
	val isLocationEnabled: LiveData<Boolean> = _isLocationEnabled

	private val _isLocationPermissionAvailable = MutableLiveData<Boolean>().apply { value = false }
	val isLocationPermissionAvailable: LiveData<Boolean> = _isLocationPermissionAvailable

	private val mLocationCallback: LocationCallback = object : LocationCallback() {
		override fun onLocationResult(locationResult: LocationResult) {
			val location = locationResult.lastLocation
			Log.d(TAG, "Location callback ..")
			_currentLocation.postValue(location)
		}
	}

	init {
		checkIfLocationEnabled()
		checkForLocationPermissions {
			_isLocationPermissionAvailable.postValue(it)
		}
	}

	fun updateCurrentLocation(location: Location) {
		_currentLocation.postValue(location)
	}

	private fun checkIfLocationEnabled() {
		val locationManager: LocationManager =
			mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		setLocationState(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
			LocationManager.NETWORK_PROVIDER
		))
	}

	fun setLocationState(state: Boolean) {
		_isLocationEnabled.postValue(state)
	}

	fun setLocationPermissionAvailability(isAvailable: Boolean) {
		_isLocationPermissionAvailable.postValue(isAvailable)
	}

	private fun checkForLocationPermissions(onCheck: (granted: Boolean) -> Unit) {
		when {
			ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
					||
					ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
				onCheck(true)
			}
			(mContext as Activity).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
				MaterialAlertDialogBuilder(mContext).setTitle("Enable Location Permissions")
					.setMessage("For locating your device using GPS. This helps us in adding your location to the post so that it can be filtered based on location. ")
					.setPositiveButton("OK") { dialog, _ ->
						dialog.dismiss()
					}.show()
			}
			else -> {
				onCheck(false)
			}
		}
	}


	private fun requestNewLocationData() {
		Log.d(TAG, "Requesting new location data")
		checkForLocationPermissions {
			if (it) {
				val request = createLocationRequest()
				mFusedLocationProviderClient.requestLocationUpdates(
					request, mLocationCallback,
					Looper.getMainLooper()
				)
			}
		}
	}

	fun buildDialogForLocationSettings(onComplete: (response: Result<LocationSettingsResponse>) -> Unit) {
		val builder = LocationSettingsRequest.Builder()
			.addLocationRequest(createLocationRequest())

		val client = LocationServices.getSettingsClient(mContext)
		val task = client.checkLocationSettings(builder.build())
		task.addOnCompleteListener {
			if (!task.isSuccessful) {
				onComplete(Result.Error(task.exception!!))
			} else {
				onComplete(Result.Success(it.result))
			}
		}
	}

	fun getAddressBasedOnLocation(location: GeoPoint, n: Int = 2): List<Address> {
		return geoCoder.getFromLocation(location.latitude, location.longitude, n)
	}

	private fun createLocationRequest(): LocationRequest {
		Log.d(TAG, "Creating location request")
		return LocationRequest.create().apply {
			interval = 10000
			fastestInterval = 5000
			priority = LocationRequest.PRIORITY_HIGH_ACCURACY
		}
	}

	fun getLastLocation(onLocationRetrieved: (locationResult: Result<Location>) -> Unit) {
		checkForLocationPermissions {
			if (it) {
				mFusedLocationProviderClient.lastLocation
					.addOnSuccessListener { location : Location? ->
						if (location != null) {
							onLocationRetrieved(Result.Success(location))
						} else {
							onLocationRetrieved(Result.Error(Exception("Location is null.")))
						}
					}.addOnFailureListener { it1 ->
						onLocationRetrieved(Result.Error(it1))
					}
			}
		}
	}

	// call this function only when there is location permission available
	fun getNearbyPlaces() {
		Log.d(TAG, "Getting nearby places")
		checkForLocationPermissions { granted ->
			if (granted) {
				requestNewLocationData()

				val placeFields =
					listOf(
						Place.Field.ID,
						Place.Field.NAME,
						Place.Field.ADDRESS,
						Place.Field.LAT_LNG
					)
				val request = FindCurrentPlaceRequest.newInstance(placeFields)
				val placeResponse = placesClient.findCurrentPlace(request)

				placeResponse.addOnCompleteListener { task ->
					if (task.isSuccessful) {
						val response = task.result
						val likelyPlaces = response.placeLikelihoods
						val nearbyLocations = mutableListOf<SimplePlace>()

						if (likelyPlaces.isEmpty())
							return@addOnCompleteListener

						likelyPlaces.sortedByDescending { it.likelihood }
							.forEachIndexed { index, placeLikelihood ->
								val currentPlace = placeLikelihood.place
								currentPlace.latLng?.let {
									val place = SimplePlace(
										currentPlace.id.orEmpty(),
										currentPlace.name.orEmpty(),
										it.latitude,
										it.longitude,
										currentPlace.address.orEmpty(),
										isGPSAccurate = false,
										isUserAccurate = index == 0
									)
									nearbyLocations.add(place)
								}
							}
						_nearbyPlaces.postValue(nearbyLocations)
					} else {
						val exception = task.exception
						if (exception is ApiException) {
							_locationErrors.postValue(exception)
							Log.e(TAG, "Place not found: ${exception.statusCode}")
						}
					}
				}
			}
		}
	}

	fun stopLocationUpdates() {
		mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
	}

	companion object {
		private const val TAG = "LocationUtility"
	}

}