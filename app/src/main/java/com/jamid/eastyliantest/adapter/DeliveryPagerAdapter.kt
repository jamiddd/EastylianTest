package com.jamid.eastyliantest.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.eastyliantest.ui.home.ActiveDeliveryFragment
import com.jamid.eastyliantest.ui.home.PendingFragment

class DeliveryPagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {

	override fun getItemCount(): Int {
		return 2
	}

	// setting isDeliveryExecutive to true by hardcoding
	// one can make it dynamic in the future
	@ExperimentalPagingApi
	override fun createFragment(position: Int): Fragment {
		return when (position) {
			0 -> PendingFragment.newInstance(true)
			1 -> ActiveDeliveryFragment.newInstance(true)
			else -> throw Exception("There cannot be any more position other than [0..1].")
		}
	}

}