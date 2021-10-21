package com.jamid.eastyliantest

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.libraries.places.api.Places

class EastylianTest: Application() {
    override fun onCreate() {
        super.onCreate()

        /*PaymentConfiguration.init(
            applicationContext,
            "pk_test_51BTUDGJAJfZb9HEBwDg86TN1KNprHjkfipXmEDMb0gSCassK5T3ZfxsAbcgKVmAIXF7oZ6ItlZZbXO6idTHE67IM007EwQ4uN3"
        )*/

//        PhonePe.init(this)

        // Initialize the SDK
        Places.initialize(applicationContext, BuildConfig.GOOGLE_MAPS_API_KEY)
        // Create a new PlacesClient instance
        Fresco.initialize(this)
    }
}