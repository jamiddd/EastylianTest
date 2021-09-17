package com.jamid.eastyliantest.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.databinding.AddModeratorLayoutBinding
import com.jamid.eastyliantest.databinding.DeliveryExecutiveLayoutBinding
import com.jamid.eastyliantest.databinding.FragmentAdminDashBinding
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.*

class AdminDashFragment: Fragment(R.layout.fragment_admin_dash) {

	private val viewModel: MainViewModel by activityViewModels()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val binding = FragmentAdminDashBinding.bind(view)

		binding.apply {

			// dashboard UI
			viewModel.repo.restaurant.observe(viewLifecycleOwner) {
				if (it != null) {
					orderNumberText.text = it.totalOrderCount.toString()
					val priceString = "₹ ${getPriceString(it.totalSalesAmount)}"
					netSaleText.text = priceString
				}
			}

			// each section separated into their own functions
			setDeliveryExecutiveSection(addDeliveryExecutiveBtn, removeDeliveryExecutiveBtn)
			setModeratorSection(addModeratorBtn, deleteModeratorBtn)
			setCakeAndPayments(allRefundsBtn, stocksBtn)
			setCustomerRelationsSections(faqBtn, notificationsBtn, feedbacksBtn, editContacts)
			setAccountSection(logOutBtn)

			viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
				fragmentAdminDashToolbar.updateLayout(marginTop = top)
			}

			fragmentAdminDashToolbar.setNavigationOnClickListener {
				findNavController().navigateUp()
			}

		}

	}

	private fun setAccountSection(logOutBtn: View) {
		logOutBtn.setOnClickListener {
			showDialog(
				getString(R.string.log_out_title),
				getString(R.string.log_out_msg),
				getString(R.string.log_out),
				getString(R.string.cancel), {
					viewModel.signOut()
					toActivity(AuthActivity::class.java)
				}, {
					it.dismiss()
				})
		}
	}

	private fun setCustomerRelationsSections(faqBtn: View, notificationsBtn: View, feedbacksBtn: View, editContactsBtn: View) {
		notificationsBtn.setOnClickListener {
			val bundle = Bundle().apply {
				putBoolean(IS_ADMIN, true)
			}
			findNavController().navigate(R.id.action_adminDashFragment_to_notificationsFragment, bundle, slideRightNavOptions())
		}

		faqBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminDashFragment_to_answerFragment, null, slideRightNavOptions())
		}

		editContactsBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminDashFragment_to_editContactsFragment, null, slideRightNavOptions())
		}

		feedbacksBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminDashFragment_to_feedbackFragment, null, slideRightNavOptions())
		}
	}

	private fun setCakeAndPayments(allRefundsBtn: View, stocksBtn: View) {
		allRefundsBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminDashFragment_to_refundFragment2, Bundle().apply { putBoolean(
				IS_ADMIN, true) }, slideRightNavOptions())
		}

		stocksBtn.setOnClickListener {
			findNavController().navigate(R.id.action_adminDashFragment_to_organizeFragment, null, slideRightNavOptions())
		}
	}

	@SuppressLint("InflateParams")
	private fun setDeliveryExecutiveSection(addDeliveryExecutiveBtn: View, removeDeliveryExecutiveBtn: View) {

		addDeliveryExecutiveBtn.setOnClickListener {
			val deliveryExecutiveView = layoutInflater.inflate(R.layout.delivery_executive_layout, null, false)
			val deliveryExecutiveLayoutBinding = DeliveryExecutiveLayoutBinding.bind(deliveryExecutiveView)
			showDialog(
				getString(R.string.add_delivery_executive_title),
				extraView = deliveryExecutiveView,
				positiveBtn = getString(R.string.add),
				negativeBtn = getString(R.string.cancel),
				onPositiveBtnClick = {
					val phoneNumber = getString(R.string.phone_number_prefix) + deliveryExecutiveLayoutBinding.deliveryExecutiveNumberText.editText?.text.toString()
					val map = mapOf(PHONE to phoneNumber, STATE to true)

					viewModel.createOrDeleteDeliveryExecutive(map) {
						if (it.isSuccessful) {
							Toast.makeText(
								requireContext(),
								getString(R.string.add_delivery_executive_msg),
								Toast.LENGTH_SHORT
							).show()
						} else {
							it.exception?.let { it1 -> viewModel.setCurrentError(it1) }
						}
					}
			}, onNegativeBtnClick = {
				it.dismiss()
			})
		}

		removeDeliveryExecutiveBtn.setOnClickListener {
			val deliveryExecutiveView = layoutInflater.inflate(R.layout.delivery_executive_layout, null, false)
			val deliveryExecutiveLayoutBinding = DeliveryExecutiveLayoutBinding.bind(deliveryExecutiveView)

			showDialog(
				getString(R.string.remove_delivery_executive_title),
				extraView = deliveryExecutiveView,
				positiveBtn = getString(R.string.remove),
				negativeBtn = getString(R.string.cancel),
				onPositiveBtnClick = {
					val phoneNumber = getString(R.string.phone_number_prefix) + deliveryExecutiveLayoutBinding.deliveryExecutiveNumberText.editText?.text.toString()
					val map = mapOf(PHONE to phoneNumber, STATE to false)

					viewModel.createOrDeleteDeliveryExecutive(map) {
						if (it.isSuccessful) {
							Toast.makeText(
								requireContext(),
								getString(R.string.remove_delivery_executive_msg),
								Toast.LENGTH_SHORT
							).show()
						} else {
							it.exception?.let { it1 -> viewModel.setCurrentError(it1) }
						}
					}
			}, onNegativeBtnClick = {
				it.dismiss()
			})
		}
	}

	@SuppressLint("InflateParams")
	private fun setModeratorSection(addModeratorBtn: View, removeModeratorBtn: View) {
		addModeratorBtn.setOnClickListener {

			val dialogView = layoutInflater.inflate(R.layout.add_moderator_layout, null, false)
			val addModeratorLayoutBinding = AddModeratorLayoutBinding.bind(dialogView)

			val dialog = showDialog(extraView = dialogView)

			addModeratorLayoutBinding.addBtn.setOnClickListener {
				val phoneText = addModeratorLayoutBinding.pNumText.text
				if (!phoneText.isNullOrBlank()) {
					val num = getString(R.string.phone_number_prefix) + phoneText.toString()
					val map = mapOf(PHONE to num, STATE to true)

					viewModel.createOrDeleteModerator(map) {
						if (it.isSuccessful) {
							Toast.makeText(
								requireContext(),
								getString(R.string.add_admin_msg),
								Toast.LENGTH_SHORT
							).show()
						} else {
							it.exception?.let { it1 -> viewModel.setCurrentError(it1) }
						}
					}
				} else {
					Toast.makeText(
						requireContext(),
						"The phone number field is empty.",
						Toast.LENGTH_SHORT
					).show()
				}


				dialog.cancel()
			}

			addModeratorLayoutBinding.cancelBtn.setOnClickListener {
				dialog.cancel()
			}
		}

		removeModeratorBtn.setOnClickListener {
			val dialogView = layoutInflater.inflate(R.layout.add_moderator_layout, null, false)
			val addModeratorLayoutBinding = AddModeratorLayoutBinding.bind(dialogView)

			val dialog = showDialog(extraView = dialogView)

			addModeratorLayoutBinding.addModeratorHeading.text = getString(R.string.remove_moderator)
			addModeratorLayoutBinding.addBtn.text = getString(R.string.remove)
			addModeratorLayoutBinding.moderatorWarningText.hide()

			addModeratorLayoutBinding.addBtn.setOnClickListener {

				val phoneText = addModeratorLayoutBinding.pNumText.text
				if (!phoneText.isNullOrBlank()) {
					val num = getString(R.string.phone_number_prefix) + addModeratorLayoutBinding.pNumText.text.toString()
					val map = mapOf(PHONE to num, STATE to false)

					viewModel.createOrDeleteModerator(map) {
						if (it.isSuccessful) {
							Toast.makeText(
								requireContext(),
								getString(R.string.remove_admin_msg),
								Toast.LENGTH_SHORT
							).show()
						} else {
							it.exception?.let { it1 -> viewModel.setCurrentError(it1) }
						}
					}
				} else {
					Toast.makeText(
						requireContext(),
						"The phone number field is empty.",
						Toast.LENGTH_SHORT
					).show()
				}

				dialog.cancel()
			}

			addModeratorLayoutBinding.cancelBtn.setOnClickListener {
				dialog.cancel()
			}
		}

	}

	private fun getPriceString(price: Long): String {
		val priceString = price.toString()
		val length = priceString.length

		if (price == 0.toLong()) return "0.0"

		val first = if (length % 2 == 0) {
			"${priceString.take(1)}.${priceString.substring(1, 3)}"
		} else {
			"${priceString.take(2)}.${priceString[2]}"
		}

		val second = when {
			length < 6 -> return (price.toDouble()/100).toString()
			length < 8 -> 'K'
			length < 10 -> 'L'
			length < 12 -> 'C'
			else -> "A LOT"
		}

		return "$first $second"

	}

	/*fun removeZeroesFromEnd(s: String): String {
		val length = s.length
		var finalString = s

		for (i in (length - 1) downTo 0) {
			if (s[i] == '0') {
				finalString = finalString.dropLast(1)
			} else {
				break
			}
		}
		return finalString
	}*/

}