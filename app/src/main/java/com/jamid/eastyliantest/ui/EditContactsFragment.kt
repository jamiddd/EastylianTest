package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FieldValue
import com.jamid.eastyliantest.ADMIN_EMAIL_ADDRESSES
import com.jamid.eastyliantest.ADMIN_PHONE_NUMBERS
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.TableItemAdapter
import com.jamid.eastyliantest.adapter.TableItemClickListener
import com.jamid.eastyliantest.databinding.FragmentEditContactsBinding
import com.jamid.eastyliantest.model.Restaurant
import com.jamid.eastyliantest.utility.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditContactsFragment: Fragment(R.layout.fragment_edit_contacts), TableItemClickListener {

	private lateinit var binding: FragmentEditContactsBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentEditContactsBinding.bind(view)

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
			binding.editContactsToolbar.updateLayout(marginTop = top)
		}

		binding.editContactsToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		val phoneNumberAdapter = TableItemAdapter(this)
		phoneNumberAdapter.isAdmin = true
		val emailAddressAdapter = TableItemAdapter(this)
		emailAddressAdapter.isAdmin = true

		val phoneNumberRecycler = initPhoneNumbersLayout(phoneNumberAdapter)
		val emailAddressRecycler = initEmailAddressesLayout(emailAddressAdapter)

		viewModel.repo.restaurant.observe(viewLifecycleOwner) { restaurant ->
			if (restaurant != null) {

				initLocationLayout(restaurant)

 				val phoneNumbers = restaurant.adminPhoneNumbers
				if (phoneNumbers.isNotEmpty()) {
					phoneNumberAdapter.submitList(phoneNumbers)
					viewLifecycleOwner.lifecycleScope.launch {
						delay(300)
						toggleActions(phoneNumberRecycler, phoneNumbers.size == 1)
					}
				}

				val emailAddresses = restaurant.adminEmailAddresses
				if (emailAddresses.isNotEmpty()) {
					emailAddressAdapter.submitList(emailAddresses)
					viewLifecycleOwner.lifecycleScope.launch {
						delay(300)
						toggleActions(emailAddressRecycler, emailAddresses.size == 1)
					}
				}
 			}
		}

		phoneNumberAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
			override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
				super.onItemRangeChanged(positionStart, itemCount)
				toggleActions(phoneNumberRecycler, itemCount == 1)
			}
		})

		emailAddressAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
			override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
				super.onItemRangeChanged(positionStart, itemCount)
				toggleActions(emailAddressRecycler, itemCount == 1)
			}
		})

		viewModel.currentPlace.observe(viewLifecycleOwner) {
			if (it != null) {
				Log.d(TAG, it.address)
			} else {
				Log.d(TAG, "Address is null")
			}
		}
	}

	private fun toggleActions(recyclerView: RecyclerView, isOnlyItem: Boolean) {
		if (isOnlyItem) {
			recyclerView.getChildAt(0)?.findViewById<MaterialButton>(R.id.actionBtn)?.hide()
		} else {
			for (child in recyclerView.children) {
				child.findViewById<MaterialButton>(R.id.actionBtn)?.show()
			}
		}
	}

	private fun initPhoneNumbersLayout(phoneNumberAdapter: TableItemAdapter): RecyclerView {
		binding.phoneNumbersContainer.apply {
			tableHeader.text = "Phone numbers"
			tablePrimaryAction.isEnabled = false
			tablePrimaryAction.text = getString(R.string.add)

			addTableDataTextLayout.startIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_call_24)

			addTableDataTextLayout.editText?.hint = "Enter phone number"
			addTableDataTextLayout.editText?.inputType = InputType.TYPE_CLASS_NUMBER

			addTableDataTextLayout.editText?.filters = arrayOf<InputFilter>(
				InputFilter.LengthFilter(
					10
				)
			)

			addTableDataTextLayout.prefixText = "+91"

			addTableDataTextLayout.editText?.doAfterTextChanged {
				tablePrimaryAction.isEnabled = !it.isNullOrBlank() && it.length == 10
			}

			tableRecycler.apply {
				adapter = phoneNumberAdapter
				layoutManager = LinearLayoutManager(requireContext())
				addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
			}

			tablePrimaryAction.setOnClickListener {

				tablePrimaryAction.isEnabled = false

				val phoneNumber = addTableDataTextLayout.editText?.text.toString()
				viewModel.updateRestaurantData(mapOf(ADMIN_PHONE_NUMBERS to FieldValue.arrayUnion("+91 $phoneNumber"))) {

					tablePrimaryAction.isEnabled = true

					if (it.isSuccessful) {
						addTableDataTextLayout.editText?.text?.clear()
						toast("Phone number added.")
					} else {
						it.exception?.let { e ->
							viewModel.setCurrentError(e)
						}
					}
				}
			}
		}
		return binding.phoneNumbersContainer.tableRecycler
	}

	private fun initEmailAddressesLayout(emailAddressAdapter: TableItemAdapter): RecyclerView {
		binding.emailAddressesContainer.apply {
			tableHeader.text = "Email addresses"
			tablePrimaryAction.isEnabled = false
			tablePrimaryAction.text = getString(R.string.add)

			addTableDataTextLayout.startIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_email_24)

			addTableDataTextLayout.editText?.hint = "Enter email address"
			addTableDataTextLayout.editText?.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

			addTableDataTextLayout.editText?.doAfterTextChanged {
				tablePrimaryAction.isEnabled = !it.isNullOrBlank() && it.isValidEmail()
			}

			tableRecycler.apply {
				adapter = emailAddressAdapter
				layoutManager = LinearLayoutManager(requireContext())
				addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
			}

			tablePrimaryAction.setOnClickListener {

				tablePrimaryAction.isEnabled = false

				val email = addTableDataTextLayout.editText?.text.toString()
				viewModel.updateRestaurantData(mapOf(ADMIN_EMAIL_ADDRESSES to FieldValue.arrayUnion(
					email
				))) {

					tablePrimaryAction.isEnabled = true

					if (it.isSuccessful) {
						addTableDataTextLayout.editText?.text?.clear()
						toast("Email address added.")
					} else {
						it.exception?.let { e ->
							viewModel.setCurrentError(e)
						}
					}
				}
			}
		}
		return binding.emailAddressesContainer.tableRecycler
	}

	private fun initLocationLayout(restaurant: Restaurant) {
		binding.restaurantLocationContainer.apply {
			dialogHeader.text = "Update address"
			dialogPositiveBtn.isEnabled = false
			dialogHelperText.text = restaurant.locationAddress

			dialogHelperText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_round_location_on_24, 0, 0, 0)

			dialogInputLayout.editText?.doAfterTextChanged {
				dialogPositiveBtn.isEnabled = !it.isNullOrBlank() && it.length > 20
				dialogNegativeBtn.isEnabled = it.isNullOrBlank()
			}

			dialogInputLayout.editText?.hint = "Write address .. "

			dialogNegativeBtn.text = "Set current address"

			dialogNegativeBtn.setOnClickListener {
				val place = viewModel.currentPlace.value
				if (place != null) {
					dialogInputLayout.editText?.setText(place.address)
				} else {
					toast("Place is null")
				}
			}

			dialogPositiveBtn.setOnClickListener {
				dialogPositiveBtn.isEnabled = false
				val newAddress = dialogInputLayout.editText?.text.toString()

				viewModel.updateRestaurantData(mapOf("locationAddress" to newAddress)) {
					dialogPositiveBtn.isEnabled = false
					if (it.isSuccessful) {
						dialogInputLayout.editText?.text?.clear()
					} else {
						it.exception?.let { e ->
							viewModel.setCurrentError(e)
						}
					}
				}

			}
		}
	}

	override fun onPrimaryActionClick(item: String) {
		if (item.contains(".")) {
			// email
			showDialog("Removing email address ..", "Are you sure you want to remove this email address from admin contacts?", "Remove", "Cancel", {
				viewModel.updateRestaurantData(mapOf(ADMIN_EMAIL_ADDRESSES to FieldValue.arrayRemove(item))) {
					if (it.isSuccessful) {
						toast("Email address removed")
					} else {
						it.exception?.let { e ->
							viewModel.setCurrentError(e)
						}
					}
				}
			}, {
				it.dismiss()
			})
		} else {
			// phone
			showDialog("Removing phone number ..", "Are you sure you want to remove this phone number from admin contacts?", "Remove", "Cancel", {
				viewModel.updateRestaurantData(mapOf(ADMIN_PHONE_NUMBERS to FieldValue.arrayRemove(item))) {
					if (it.isSuccessful) {
						toast("Phone number removed")
					} else {
						it.exception?.let { e ->
							viewModel.setCurrentError(e)
						}
					}
				}
			}, {
				it.dismiss()
			})
		}
	}

	companion object {
		private const val TAG = "EditContacts"
	}

}