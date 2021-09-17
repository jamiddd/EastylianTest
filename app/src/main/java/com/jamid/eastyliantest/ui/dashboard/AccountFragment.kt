package com.jamid.eastyliantest.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.OrderAdapter
import com.jamid.eastyliantest.databinding.FragmentAccountBinding
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.*

class AccountFragment : Fragment(R.layout.fragment_account) {

	private lateinit var binding: FragmentAccountBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAccountBinding.bind(view)

		val orderAdapter = OrderAdapter()
		val orderAdapter1 = OrderAdapter()

		binding.pastOrdersRecycler.apply {
			adapter = orderAdapter
			layoutManager = LinearLayoutManager(requireContext())
		}

		binding.currentOrdersRecycler.apply {
			adapter = orderAdapter1
			layoutManager = LinearLayoutManager(requireContext())
		}

		viewModel.repo.allOrders.observe(viewLifecycleOwner) {
			if (it != null) {

				Log.d(TAG, it.toString())

				val orders = it.map { it1 ->
					it1.order.items = it1.cartItems
					it1.order
				}

				if (orders.isNullOrEmpty()) {
					binding.noOrdersText.show()
				} else {
					binding.noOrdersText.hide()
				}


				val pastOrders = orders.filter { it1 ->
					it1.status[0] == OrderStatus.Delivered || it1.status[0] == OrderStatus.Cancelled
				}

				val currentOrders = orders.filter { it1 ->
					it1.status[0] == OrderStatus.Paid || it1.status[0] == OrderStatus.Due || it1.status[0] == OrderStatus.Preparing || it1.status[0] == OrderStatus.Delivering
				}

				for (order in pastOrders) {
					Log.d(TAG, "Past orders - " + order.status.toString())
				}

				if (pastOrders.isEmpty()) {
					binding.pastOrdersRecycler.hide()
					binding.pastOrdersHeader.hide()
					binding.seeAllPastOrdersBtn.hide()
				} else {

					if (pastOrders.size < 2) {
						binding.seeAllPastOrdersBtn.hide()
					}

					binding.pastOrdersRecycler.show()
					binding.pastOrdersHeader.show()
					binding.seeAllPastOrdersBtn.show()

					orderAdapter.submitList(pastOrders.take(2).sortedByDescending { it1 ->
						it1.createdAt
					})
				}

				if (currentOrders.isEmpty()) {
					binding.currentOrdersRecycler.hide()
					binding.currentOrdersHeader.hide()

				} else {
					binding.currentOrdersHeader.show()
					binding.currentOrdersRecycler.show()

					orderAdapter1.submitList(currentOrders.sortedByDescending { it1 ->
						it1.createdAt
					})

					/*requireActivity().setCurrentOrderListeners(currentOrders, viewModel)*/
				}

			} else {

				binding.pastOrdersRecycler.hide()
				binding.pastOrdersHeader.hide()
				binding.noOrdersText.show()
			}
		}

		binding.refundBtn.setOnClickListener {
			findNavController().navigate(
				R.id.action_dashboardFragment_to_refundFragment,
				null,
				slideRightNavOptions()
			)
		}

		viewModel.repo.currentUser.observe(viewLifecycleOwner) {
			if (it != null) {
				binding.accountToolbar.title = it.name
				binding.accountToolbar.subtitle = "${it.phoneNo} • ${it.email}"
			}
		}

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
			binding.accountToolbar.updateLayout(marginTop = top)
			binding.dashboardScroller.setPadding(
				0,
				convertDpToPx(8),
				0,
				bottom + convertDpToPx(100)
			)
		}

		binding.changeAddressBtn.setOnClickListener {
			findNavController().navigate(R.id.addressFragment, null, slideRightNavOptions())
		}

		binding.favoritesBtn.setOnClickListener {
			findNavController().navigate(R.id.favoritesFragment, null, slideRightNavOptions())
		}

		binding.helpBtn.setOnClickListener {
			findNavController().navigate(R.id.helpFragment, null, slideRightNavOptions())
		}

		binding.pastOrdersHeader.setOnClickListener {
			if (binding.pastOrdersRecycler.isVisible) {
				binding.pastOrdersHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(
					0,
					0,
					R.drawable.ic_round_keyboard_arrow_down_24,
					0
				)
				binding.pastOrdersRecycler.hide()
				binding.seeAllPastOrdersBtn.hide()
			} else {
				binding.pastOrdersHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(
					0,
					0,
					R.drawable.ic_round_keyboard_arrow_up_24,
					0
				)
				binding.pastOrdersRecycler.show()
				binding.seeAllPastOrdersBtn.show()
			}
		}

		binding.seeAllPastOrdersBtn.setOnClickListener {
			findNavController().navigate(
				R.id.action_dashboardFragment_to_pastOrdersFragment,
				null,
				slideRightNavOptions()
			)
		}

		binding.logOutBtn.setOnClickListener {
			MaterialAlertDialogBuilder(requireContext())
				.setTitle("Logging Out ...")
				.setMessage("Are you sure you want to log out?")
				.setPositiveButton("Log out") { _, _ ->
					viewModel.signOut()
					val intent = Intent(requireContext(), AuthActivity::class.java)
					intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
					startActivity(intent)
					requireActivity().finish()
				}.setNegativeButton("Cancel") { a, _ ->
					a.dismiss()
				}.show()
		}

	}

	companion object {

		private const val TAG = "Dashboard"

		@JvmStatic
		fun newInstance() = AccountFragment()
	}
}