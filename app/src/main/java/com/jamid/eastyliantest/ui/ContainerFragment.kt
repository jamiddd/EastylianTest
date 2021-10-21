package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentContainerBinding
import com.jamid.eastyliantest.ui.home.HomeFragmentNew
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class ContainerFragment: Fragment() {

    private lateinit var binding: FragmentContainerBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContainerBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        binding.containerViewPager.adapter = ContainerPagerAdapter(activity)

        binding.containerViewPager.isUserInputEnabled = false

        val tabLayout = activity.findViewById<TabLayout>(R.id.main_navigation)
        val appbar = activity.findViewById<AppBarLayout>(R.id.main_appbar)

        TabLayoutMediator(tabLayout, binding.containerViewPager) { tab, pos ->
            when (pos) {
                0 -> {
                    tab.setIcon(R.drawable.ic_home_black_24dp)
                    tab.text = "Home"
                }
                1 -> {
                    tab.setIcon(R.drawable.ic_round_shopping_cart_24)
                    tab.text = "Cart"
                }
                2 -> {
                    tab.setIcon(R.drawable.ic_round_account_circle_24)
                    tab.text = "Account"
                }
            }
        }.attach()

        binding.containerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        // Don't do anything
                        activity.findViewById<CardView>(R.id.bottomCartAction2)?.hide()
//                        TODO("Check for changes in the items in the current order and update the buttons in cake adapter.")

                    }
                    1 -> {
                        activity.findViewById<NestedScrollView>(R.id.fragmentCartScroll)?.isNestedScrollingEnabled = false
                        val currentOrder = viewModel.currentCartOrder.value
                        if (currentOrder != null) {
                            activity.findViewById<CardView>(R.id.bottomCartAction2)?.show()
                        } else {
                            activity.findViewById<CardView>(R.id.bottomCartAction2)?.hide()
                        }
                        appbar.setExpanded(false, false)
                    }
                    2 -> {
                        activity.findViewById<NestedScrollView>(R.id.accountScroll)?.isNestedScrollingEnabled = false
                        activity.findViewById<CardView>(R.id.bottomCartAction2)?.hide()
                        appbar.setExpanded(false, false)
                    }
                }
            }
        })

    }

    private fun checkForAdapterChanges() {
        val currentOrder = viewModel.currentCartOrder.value
        if (currentOrder != null) {
            val items = currentOrder.items
            for (item in items) {
                val cakeId = item.cake.id

            }
        }
    }

    private inner class ContainerPagerAdapter(activity: FragmentActivity): FragmentStateAdapter(activity) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragmentNew()
                1 -> CartFragmentNew()
                2 -> AccountFragmentNew()
                else -> throw IllegalStateException("There cannot be a fragment for position: $position")
            }
        }

    }

}