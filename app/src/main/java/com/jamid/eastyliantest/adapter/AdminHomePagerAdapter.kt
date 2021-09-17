package com.jamid.eastyliantest.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jamid.eastyliantest.ui.home.ActiveDeliveryFragment
import com.jamid.eastyliantest.ui.home.DeliveredFragment
import com.jamid.eastyliantest.ui.home.PendingFragment
import com.jamid.eastyliantest.ui.home.RequestsFragment

class AdminHomePagerAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {

    override fun getItemCount(): Int {
        return 4
    }

    @ExperimentalPagingApi
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RequestsFragment.newInstance()
            1 -> PendingFragment.newInstance()
            2 -> ActiveDeliveryFragment.newInstance()
            3 -> DeliveredFragment.newInstance()
            else -> throw Exception("There cannot be any more position other than [0..2].")
        }
    }

}