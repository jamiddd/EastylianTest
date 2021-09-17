package com.jamid.eastyliantest.interfaces

import com.google.android.libraries.places.api.model.Place

interface OnPlaceClickListener {
    fun onPlaceClick(place: Place)
}