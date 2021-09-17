package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

@Entity(tableName = "places")
@Parcelize
@IgnoreExtraProperties
data class SimplePlace(
    @PrimaryKey
    var id: String,
    var name: String,
    var latitude: Double,
    var longitude: Double,
    var address: String,
    @Exclude @set: Exclude @get: Exclude
    var isGPSAccurate: Boolean,
    @Exclude @set: Exclude @get: Exclude
    var isUserAccurate: Boolean,
): Parcelable {
    constructor(): this("", "", 0.0, 0.0, "", false, false)

    fun setIsUserAccurate(isUserAccurate: Boolean): SimplePlace {
        this.isUserAccurate = isUserAccurate
        return this
    }

    fun setIsGPSAccurate(isGPSAccurate: Boolean): SimplePlace {
        this.isGPSAccurate = isGPSAccurate
        return this
    }

    companion object {
        fun newInstance(place: Place) = SimplePlace(randomId(), place.name.orEmpty(), place.latLng?.latitude ?: 0.0, place.latLng?.longitude ?: 0.0, place.address.orEmpty(),
            isGPSAccurate = false,
            isUserAccurate = false
        )
    }
}