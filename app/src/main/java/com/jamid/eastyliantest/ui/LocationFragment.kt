package com.jamid.eastyliantest.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentLocationBinding
import com.jamid.eastyliantest.interfaces.OnPlaceClickListener
import com.jamid.eastyliantest.model.SimplePlace
import com.jamid.eastyliantest.utility.convertDpToPx
import com.jamid.eastyliantest.utility.hideKeyboard
import com.jamid.eastyliantest.utility.updateLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LocationFragment: Fragment(R.layout.fragment_location), OnMapReadyCallback, OnPlaceClickListener {

    private lateinit var binding: FragmentLocationBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var placesClient: PlacesClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<CardView>
    private lateinit var mapBoundary: LatLngBounds
    private lateinit var geocoder: Geocoder
    private var currentMarker: Marker? = null
    private var mGoogleMap: GoogleMap? = null
    private lateinit var mContext: Context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLocationBinding.bind(view)
        mContext = requireContext()
        placesClient = Places.createClient(mContext)
        geocoder = Geocoder(mContext)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.confirmLocationCard)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val mapFragment = childFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment

        mapFragment?.getMapAsync(this)

        val minMargin = convertDpToPx(8)

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.searchLayout.updateLayout(marginTop = top + minMargin, marginLeft = minMargin, marginRight = minMargin, marginBottom = minMargin)
        }

        binding.mapFragmentBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.currentLocationBtn.setOnClickListener {
            val currentGPSLocation = viewModel.gpsLocation.value
            if (currentGPSLocation != null) {
                val currentPlace = SimplePlace()
                currentPlace.latitude = currentGPSLocation.latitude
                currentPlace.longitude = currentGPSLocation.longitude
                viewModel.setCurrentPlace(currentPlace)
            }
        }

        binding.searchPlaceText.doAfterTextChanged {
            if (!it.isNullOrBlank() && it.length > 2) {
                getSearchPredictions(it.toString())
            } else {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    getNearbyPlaces()
                }
            }
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

    }

    private fun getSearchPredictions(query: String) {
        val token = AutocompleteSessionToken.newInstance()
        val currentLocation = viewModel.gpsLocation.value!!
        val bottomBoundary = currentLocation.latitude - .05
        val leftBoundary = currentLocation.longitude - .05
        val topBoundary = currentLocation.latitude + .05
        val rightBoundary = currentLocation.longitude + .05

        mapBoundary = LatLngBounds(LatLng(bottomBoundary, leftBoundary), LatLng(topBoundary, rightBoundary))
        val bounds = RectangularBounds.newInstance(mapBoundary)

        val request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
//                .setLocationBias(bounds)
                .setLocationRestriction(bounds)
                .setOrigin(LatLng(currentLocation.latitude, currentLocation.longitude))
                .setCountries("IN")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(token)
                .setQuery(query)
                .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                binding.nearByPlaces.visibility = View.VISIBLE
                binding.divider5.visibility = View.VISIBLE
                binding.nearByPlaces.apply {
                    adapter = LocationAdapter(response.autocompletePredictions.take(minOf(5, response.autocompletePredictions.size)))
                    layoutManager = LinearLayoutManager(mContext)
                }
            }.addOnFailureListener { exception: Exception? ->
                binding.nearByPlaces.visibility = View.GONE
                binding.divider5.visibility = View.GONE
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: " + exception.statusCode)
                }
            }
    }

    private suspend fun getPlaceWithPlaceId(id: String): Place? {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(id, placeFields)

        return try {
            val task = placesClient.fetchPlace(request)
            val result = task.await()
            result.place
        } catch (e: Exception) {
            viewModel.setCurrentError(e)
            if (e is ApiException) {
                val statusCode = e.statusCode
                Log.e(TAG, "Place not found: ${e.message} --- $statusCode")
            }
            null
        }
    }

    private fun setCameraView(place: SimplePlace) {
        val latitude = place.latitude
        val longitude = place.longitude
        val bottomBoundary = latitude - .05
        val leftBoundary = longitude - .05
        val topBoundary = latitude + .05
        val rightBoundary = longitude + .05

        mapBoundary = LatLngBounds(LatLng(bottomBoundary, leftBoundary), LatLng(topBoundary, rightBoundary))
        val coordinate = LatLng(latitude, longitude)
        val targetLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 17f)
        mGoogleMap?.animateCamera(targetLocation)
        currentMarker?.showInfoWindow()
    }

    /*private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer
        val clusterManager = ClusterManager<Place>(requireContext(), googleMap)
        clusterManager.renderer =
            PlaceRenderer(
                requireContext(),
                googleMap,
                clusterManager
            )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))

        // Add the places to the ClusterManager
        clusterManager.addItems(places)
        clusterManager.cluster()

        // Show polygon
        clusterManager.setOnClusterItemClickListener { item ->
            addCircle(googleMap, item)
            return@setOnClusterItemClickListener false
        }

        // When the camera starts moving, change the alpha value of the marker to translucent
        googleMap.setOnCameraMoveStartedListener {
            clusterManager.markerCollection.markers.forEach { it.alpha = 0.3f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 0.3f }
        }

        googleMap.setOnCameraIdleListener {
            // When the camera stops moving, change the alpha value back to opaque
            clusterManager.markerCollection.markers.forEach { it.alpha = 1.0f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 1.0f }

            // Call clusterManager.onCameraIdle() when the camera stops moving so that re-clustering
            // can be performed when the camera stops moving
            clusterManager.onCameraIdle()
        }
    }*/
    //    private var circle: Circle? = null
    // [START maps_android_add_map_codelab_ktx_add_circle]
    /**
     */
    /*private fun addCircle(googleMap: GoogleMap, item: Place) {
        circle?.remove()
        circle = googleMap.addCircle {
            center(item.latLng)
            radius(1000.0)
            fillColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryTranslucent))
            strokeColor(ContextCompat.getColor(requireContext(), R.color.primaryColor))
        }
    }*/
    // [END maps_android_add_map_codelab_ktx_add_circle]

   /* private val bicycleIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.ic_round_directions_bike_24, color)
    }*/

    // [START maps_android_add_map_codelab_ktx_add_markers]
    /**
     * Adds markers to the map. These markers won't be clustered.
     */
   /* private fun addMarkers(googleMap: GoogleMap) {
        places.forEach { place ->
            val marker = googleMap.addMarker {
                title(place.name)
                position(place.latLng)
                icon(bicycleIcon)
            }
            // Set place as the tag on the marker object so it can be referenced within
            // MarkerInfoWindowAdapter
            marker.tag = place
        }
    }*/

    override fun onMapReady(p0: GoogleMap) {
        mGoogleMap = p0
        p0.setMaxZoomPreference(20f)

        p0.setOnMapClickListener { latLang ->
            if (binding.nearByPlaces.isVisible) {
                binding.nearByPlaces.visibility = View.GONE
                binding.divider5.visibility = View.GONE
            }
            val place = SimplePlace()
            place.latitude = latLang.latitude
            place.longitude = latLang.longitude
            viewModel.setCurrentPlace(place)
        }

        viewModel.currentSelectedPlace.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {

                    val address = addresses.first()
                    val name = address.featureName ?: address.locality ?: address.subLocality ?: "Unnamed Place"
                    val formattedAddress = address.getAddressLine(0)

                    val place = Place.builder()
                        .setLatLng(LatLng(location.latitude, location.longitude))
                        .setName(name)
                        .setAddress(formattedAddress)
                        .build()

                    onPlaceClick(place)
                } else {
                    // Do something here
                }
            } else {
                binding.confirmLocationBtn.isEnabled = false
                binding.currentLocationProgress.visibility = View.VISIBLE
                binding.currentPlaceText.visibility = View.INVISIBLE
                binding.currentPlaceAddressText.visibility = View.INVISIBLE

                val gps = viewModel.gpsLocation.value
                if (gps != null) {
                    val newPlace = SimplePlace()
                    newPlace.latitude = gps.latitude
                    newPlace.longitude = gps.longitude
                    viewModel.setCurrentPlace(newPlace)
                } else {
                    getNearbyPlaces()
                }
            }
        }

        p0.setOnCameraMoveStartedListener {
            binding.nearByPlaces.visibility = View.GONE
            binding.divider5.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        p0.setOnCameraIdleListener {
            if (viewModel.windowInsets.value != null && viewModel.windowInsets.value!!.second > convertDpToPx(100)) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

    }

    // Must have location permission for calling this function
    @SuppressLint("MissingPermission")
    fun getNearbyPlaces() {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = FindCurrentPlaceRequest.newInstance(placeFields)
        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                binding.divider5.visibility = View.VISIBLE
                binding.nearByPlaces.visibility = View.VISIBLE
                binding.nearByPlaces.apply {
                    adapter = LocationAdapter(response?.placeLikelihoods?.take(minOf(response.placeLikelihoods.size, 5)) ?: emptyList())
                    layoutManager = LinearLayoutManager(mContext)
                }
            } else {
                binding.divider5.visibility = View.GONE
                binding.nearByPlaces.visibility = View.GONE
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e(TAG, "Place not found: ${exception.statusCode}")
                }
            }
        }

    }

    inner class LocationAdapter <T: Any> (private val places: List<T>): RecyclerView.Adapter<LocationAdapter<T>.LocationViewHolder>() {
        inner class LocationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

            fun bind(item: T) {
                fun setPlaceView(place: Place) {
                    val primary = view.findViewById<TextView>(R.id.location_item)
                    primary.text = place.name
                    val secondary = view.findViewById<TextView>(R.id.location_address)
                    secondary.text = place.address
                    view.setOnClickListener {
                        onPlaceClick(place)
                    }
                }

                when (item) {
                    is Place -> {
                        setPlaceView(item as Place)
                    }
                    is PlaceLikelihood -> {
                        setPlaceView((item as PlaceLikelihood).place)
                    }
                    is AutocompletePrediction -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val place = getPlaceWithPlaceId((item as AutocompletePrediction).placeId)
                            if (place != null) {
                                setPlaceView(place)
                            } else {
                                view.visibility = View.GONE
                            }
                        }
                    }
                    else -> {
                        Log.d(TAG, item::class.java.simpleName)
                    }
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            return LocationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.address_item, parent, false))
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            holder.bind(places[position])
        }

        override fun getItemCount(): Int {
            return places.size
        }
    }

    override fun onPlaceClick(place: Place) {
        hideKeyboard()

        binding.confirmLocationBtn.isEnabled = true
        binding.currentLocationProgress.visibility = View.GONE
        binding.currentPlaceText.visibility = View.VISIBLE
        binding.currentPlaceAddressText.visibility = View.VISIBLE

        binding.divider5.visibility = View.GONE
        binding.nearByPlaces.visibility = View.GONE

        currentMarker?.remove()
        currentMarker = mGoogleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(place.latLng!!.latitude, place.latLng!!.longitude))
                .title("Choose this location")
        )

        val isGpsAccurate = ((place.latLng!!.latitude - viewModel.gpsLocation.value!!.latitude < 0.01) && (place.latLng!!.longitude - viewModel.gpsLocation.value!!.longitude < 0.01))
        val currentSimplePlace = SimplePlace.newInstance(place)
            .setIsGPSAccurate(isGpsAccurate)
            .setIsUserAccurate(true)

        setCameraView(currentSimplePlace)

        binding.currentPlaceText.text = place.name
        binding.currentPlaceAddressText.text = place.address
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED


        binding.confirmLocationBtn.setOnClickListener {
            viewModel.confirmPlace(currentSimplePlace)
            findNavController().navigateUp()
        }

    }

    companion object {
        private const val TAG = "LocationFragment"
    }

}