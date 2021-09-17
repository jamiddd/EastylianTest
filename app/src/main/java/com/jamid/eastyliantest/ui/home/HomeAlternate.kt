package com.jamid.eastyliantest.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentHomeAlternateBinding

class HomeAlternate : Fragment(R.layout.fragment_home_alternate) {

    private lateinit var binding: FragmentHomeAlternateBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeAlternateBinding.bind(view)

        if (Firebase.auth.currentUser == null) {
            findNavController().navigate(R.id.auth_graph)
            return
        }

        binding.alternateHomePager.adapter = AlternateHomeAdapter(requireActivity())

        TabLayoutMediator(binding.alternateHomeTabLayout, binding.alternateHomePager) { a, b ->
            when (a.position) {
                0 -> a.text = "Requests"
                1 -> a.text = "Pending"
                2 -> a.text = "Delivered"
            }
        }.attach()

    }

    inner class AlternateHomeAdapter(fa: FragmentActivity): FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return 3
        }

        @ExperimentalPagingApi
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RequestsFragment.newInstance()
                1 -> PendingFragment.newInstance()
                2 -> DeliveredFragment.newInstance()
                else -> throw Exception("There cannot be any more position other than [0..2].")
            }
        }

    }

    companion object {

        @JvmStatic
        fun newInstance() =
            HomeAlternate()
    }
}