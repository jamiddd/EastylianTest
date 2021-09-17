package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.adapter.CakeMiniAdapter
import com.jamid.eastyliantest.model.Cake

interface CakeMiniListener {
	fun onCakeAvailabilityChange(cake: Cake)
	fun onCakeDelete(cake: Cake)
	fun onCakeUpdate(cake: Cake)
	fun onContextActionMode(vh: CakeMiniAdapter.CakeMiniViewHolder, cake: Cake)
	fun onClick(vh: CakeMiniAdapter.CakeMiniViewHolder, cake: Cake)
}