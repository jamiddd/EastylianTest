package com.jamid.eastyliantest.ui

import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.material.snackbar.Snackbar
import com.jamid.eastyliantest.utility.LocationUtility
import com.jamid.eastyliantest.model.Result

abstract class LocationAwareActivity: AppCompatActivity() {

	open val locationUtility: LocationUtility by lazy { LocationUtility(this) }
	open var requestingLocationUpdates = false
	private var rootLayout: View? = null
	private var snackBarAnchorView: View? = null

	open val activityResult =
		registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
			locationUtility.setLocationState(it.resultCode == RESULT_OK)
		}

	open val requestLocationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		locationUtility.setLocationPermissionAvailability(isGranted)
	}

	open fun initiate(rootView: View, anchorView: View? = null) {
		rootLayout = rootView
		snackBarAnchorView = anchorView
	}

	open fun stopLocationUpdates() {
		locationUtility.stopLocationUpdates()
	}

	open fun askUserToEnableLocation(onComplete: (result: Result<LocationSettingsResponse>) -> Unit) {
		showSnack("Please enable location!", "Enable") {
			locationUtility.buildDialogForLocationSettings {
				onComplete(it)
			}
		}
	}

	open fun showSnack(msg: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
		val anchor = snackBarAnchorView
		val snackBar = if (anchor != null) {
			Snackbar.make(rootLayout!!, msg, Snackbar.LENGTH_LONG)
				.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
				.setAnchorView(anchor)
		} else {
			Snackbar.make(rootLayout!!, msg, Snackbar.LENGTH_LONG)
				.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
		}

		if (actionText != null) {
			snackBar.setAction(actionText) {
				onAction!!()
			}
		}

		snackBar.show()
	}
}