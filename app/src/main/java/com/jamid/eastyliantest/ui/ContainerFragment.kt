package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentContainerBinding
import com.jamid.eastyliantest.utility.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ContainerFragment: Fragment() {

    private lateinit var binding: FragmentContainerBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var count = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContainerBinding.inflate(inflater)
        return binding.root
    }

    private fun setParentHostCoOrdinatedBehavior(behavior: CoordinatorLayout.Behavior<View>?) {
        val parentHost = requireActivity().findViewById<FragmentContainerView>(R.id.nav_host_fragment)
        val params = parentHost.layoutParams as CoordinatorLayout.LayoutParams
        if (params.behavior != behavior) {
            params.behavior = behavior
            parentHost.layoutParams = params
        }
    }

    private fun setViewCartPosition(viewCartCard: View) {
        if (viewModel.currentCartOrder.value != null) {
            viewCartCard.slideReset()
        } else {
            viewCartCard.slideDown(convertDpToPx(150).toFloat())
        }
    }

    private fun setCheckoutCard(checkoutCard: View) {
        if (viewModel.currentCartOrder.value != null) {
            checkoutCard.slideReset()
        } else {
            checkoutCard.slideDown(convertDpToPx(150).toFloat())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()

        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_home_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val viewCartCard = activity.findViewById<CardView>(R.id.viewCartCard)
        val checkoutCard = activity.findViewById<CardView>(R.id.bottomCartAction2)

        val bottomNavigationView = activity.findViewById<BottomNavigationView>(R.id.main_navigation)
        bottomNavigationView.setupWithNavController(navController)

        val mainAppBar = activity.findViewById<AppBarLayout>(R.id.main_appbar)

        val behavior = AppBarLayout.ScrollingViewBehavior()
        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {
                R.id.homeFragmentNew -> {
                    setViewCartPosition(viewCartCard)
                    mainAppBar.show()
                    setParentHostCoOrdinatedBehavior(behavior)
                    checkoutCard.hide()
                }
                R.id.cartFragmentNew -> {
                    viewCartCard.slideDown(convertDpToPx(150).toFloat())
                    setParentHostCoOrdinatedBehavior(null)
                    mainAppBar.hide()
                    setCheckoutCard(checkoutCard)
                }
                R.id.accountFragmentNew -> {
                    setViewCartPosition(viewCartCard)
                    setParentHostCoOrdinatedBehavior(behavior)
                    mainAppBar.show()
                    checkoutCard.hide()
                }
            }
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (count != 1) {
                toast("Press back again to exit.")
                count++
            } else {
                activity.finish()
            }
            viewLifecycleOwner.lifecycleScope.launch {
                delay(5000)
                count = 0
            }
        }

    }
}