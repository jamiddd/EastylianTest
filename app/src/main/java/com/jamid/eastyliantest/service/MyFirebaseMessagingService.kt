package com.jamid.eastyliantest.service

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jamid.eastyliantest.NOTIFICATION_INTENT


class MyFirebaseMessagingService: FirebaseMessagingService() {

	private val auth = Firebase.auth

	override fun onNewToken(p0: String) {
		super.onNewToken(p0)
		if (auth.currentUser != null) {
			if (application != null) {
				val intent = Intent("tokenIntent").apply {
					putExtra("token", p0)
				}
				LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
			}
		}
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		super.onMessageReceived(remoteMessage)
		val intent = Intent(NOTIFICATION_INTENT)

		Log.d(TAG, "New message received + $remoteMessage")

		intent.putExtra("title", remoteMessage.notification?.title)
		intent.putExtra("body", remoteMessage.notification?.body)
		intent.putExtra("image", remoteMessage.notification?.imageUrl)

		if (remoteMessage.data.isNotEmpty()) {

			Log.d(TAG, "remote message data exists")

			if (remoteMessage.data.containsKey("image")) {
				Log.d(TAG, "remote message image exists")
				intent.putExtra("image", remoteMessage.data["image"])
			} else {
				Log.d(TAG, "remote message image doesn't exists")
			}

			val orderId = remoteMessage.data["orderId"]
			val orderStatus = remoteMessage.data["status"]
			intent.putExtra("orderId", orderId)
			intent.putExtra("status", orderStatus)
		} else {
			Log.d(TAG, "remote message data doesn't exists")
		}

		LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
	}

	companion object {
		private const val TAG = "MyFCM"
	}
}