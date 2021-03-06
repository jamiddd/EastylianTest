package com.jamid.eastyliantest.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.OrderAdapter
import com.jamid.eastyliantest.databinding.FragmentAccountBinding
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.ui.MainActivity
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.*

class AccountFragment : Fragment(R.layout.fragment_account) {

	private lateinit var binding: FragmentAccountBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var currentImage: String? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAccountBinding.bind(view)

		val orderAdapter = OrderAdapter(viewLifecycleOwner.lifecycleScope)
		val orderAdapter1 = OrderAdapter(viewLifecycleOwner.lifecycleScope)

		binding.pastOrdersRecycler.apply {
			adapter = orderAdapter
			layoutManager = LinearLayoutManager(requireContext())
		}

		binding.currentOrdersRecycler.apply {
			adapter = orderAdapter1
			layoutManager = LinearLayoutManager(requireContext())
		}

		viewModel.repo.currentUser.observe(viewLifecycleOwner) {
			if (it != null) {
				if (it.photoUrl != null) {
					binding.customerImage.setImageURI(it.photoUrl)
				} else {
					val restaurant = viewModel.repo.restaurant.value
					if (restaurant != null) {
						val images = restaurant.randomUserIcons
						if (images.isNotEmpty()) {
							binding.customerImage.setImageURI(images.random())
						}
					} else {
						Log.d(TAG, "Restaurant is null.")
					}
				}

				binding.customerNameText.setText(it.name)
				binding.customerNameTextView.text = it.name

				binding.accountToolbar.title = it.name
				binding.accountToolbar.subtitle = "${it.phoneNo} ??? ${it.email}"


			}
		}

		binding.customerNameText.doAfterTextChanged {
			binding.editBtn.isEnabled = !it.isNullOrBlank() && it.length > 3
		}

		binding.editBtn.setOnClickListener {

			if (binding.editBtn.text == "Edit") {

				binding.customerNameText.show()
				binding.customerNameTextView.hide()

				binding.editBtn.text = getString(R.string.done)

			} else {
				binding.customerNameText.hide()
				binding.customerNameTextView.show()

				val name = binding.customerNameText.text.toString()
				val changes = mapOf("fullName" to name)

				viewModel.updateFirebaseUser(changes) {
					val changes1 = mapOf("name" to name)
					viewModel.updateUser(changes1)
				}

				binding.editBtn.text = getString(R.string.edit)
				binding.editBtn.enable()

			}

		}

		var justStarted = true

		binding.customerImage.setOnClickListener {
			val popupMenu = PopupMenu(requireContext(), it)

			popupMenu.inflate(R.menu.image_menu)

			popupMenu.setOnMenuItemClickListener { it1 ->
				justStarted = false
				when (it1.itemId) {
					R.id.change_image -> {
						(activity as MainActivity?)?.selectImage()
					}
					R.id.remove_image -> {
						viewModel.setCurrentImage(null)
					}
				}
				return@setOnMenuItemClickListener true
			}

			popupMenu.show()
		}



		viewModel.currentImage.observe(viewLifecycleOwner) {
			binding.profilePhotoUploadProgress.show()
			if (it != null) {
				viewModel.uploadImage(it) { downloadUri ->
					if (downloadUri != null) {
						viewModel.updateFirebaseUser(mapOf("photoUrl" to downloadUri.toString())) { it1 ->
							if (it1.isSuccessful) {
								binding.profilePhotoUploadProgress.hide()
								val changes1 = mapOf("photoUrl" to downloadUri.toString())
								viewModel.updateUser(changes1)
							} else {
								toast("Something went wrong while updating user data.")
							}
						}
					}
				}
			} else {
				binding.profilePhotoUploadProgress.hide()
				currentImage = null
				if (!justStarted) {
					viewModel.updateFirebaseUser(mapOf("photoUrl" to null)) {
						val changes1 = mapOf("photoUrl" to null)
						viewModel.updateUser(changes1)
					}
				}
			}
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
					it1.status.first() == OrderStatus.Delivered || it1.status.first() == OrderStatus.Cancelled
				}.sortedByDescending { it1 ->
					it1.deliveryAt
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