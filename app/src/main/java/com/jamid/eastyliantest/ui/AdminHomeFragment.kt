package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.AdminHomePagerAdapter
import com.jamid.eastyliantest.databinding.FragmentAdminBinding
import com.jamid.eastyliantest.utility.slideRightNavOptions
import com.jamid.eastyliantest.utility.updateLayout

class AdminHomeFragment: Fragment(R.layout.fragment_admin) {

    private lateinit var binding: FragmentAdminBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAdminBinding.bind(view)

        binding.adminHomeViewPager.adapter = AdminHomePagerAdapter(requireActivity())

        TabLayoutMediator(binding.adminHomeTabLayout, binding.adminHomeViewPager) { a, b ->
            when (b) {
                0 -> a.text = getString(R.string.requests)
                1 -> a.text = getString(R.string.pending)
                2 -> a.text = getString(R.string.deliveries)
                3 -> a.text = getString(R.string.delivered)
            }
        }.attach()

        (activity as AdminActivity).viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.adminHomeToolbar.updateLayout(marginTop = top)
        }


        binding.adminHomeToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.admin_dash -> findNavController().navigate(R.id.action_adminHomeFragment_to_adminDashFragment, null, slideRightNavOptions())
            }
            true
        }

    }

}