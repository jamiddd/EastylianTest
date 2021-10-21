package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.AdminHomePagerAdapter
import com.jamid.eastyliantest.databinding.FragmentAdminBinding
import com.jamid.eastyliantest.utility.slideRightNavOptions

class AdminHomeFragment: Fragment(R.layout.fragment_admin) {

    private lateinit var binding: FragmentAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.admin_home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.admin_dash) {
            findNavController().navigate(R.id.action_adminHomeFragment_to_adminDashFragment, null, slideRightNavOptions())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAdminBinding.bind(view)

        binding.adminHomeViewPager.adapter = AdminHomePagerAdapter(requireActivity())
        val tabLayout = requireActivity().findViewById<TabLayout>(R.id.adminTabLayout)

        TabLayoutMediator(tabLayout, binding.adminHomeViewPager) { a, b ->
            when (b) {
                0 -> a.text = getString(R.string.requests)
                1 -> a.text = getString(R.string.preparing)
                2 -> a.text = getString(R.string.deliveries)
                3 -> a.text = getString(R.string.delivered)
            }
        }.attach()

    }

}