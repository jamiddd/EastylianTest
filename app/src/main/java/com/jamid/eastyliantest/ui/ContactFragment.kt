package com.jamid.eastyliantest.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.TableItemAdapter
import com.jamid.eastyliantest.adapter.TableItemClickListener
import com.jamid.eastyliantest.databinding.FragmentContactBinding
import com.jamid.eastyliantest.utility.composeEmail
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.toast

class ContactFragment: Fragment(R.layout.fragment_contact), TableItemClickListener {

	private lateinit var binding: FragmentContactBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentContactBinding.bind(view)
		val activity = requireActivity()

		val phoneNumberAdapter = TableItemAdapter(this)
		phoneNumberAdapter.isAdmin = false
		val emailAddressAdapter = TableItemAdapter(this)
		emailAddressAdapter.isAdmin = false

		initPhoneNumbersLayout(phoneNumberAdapter)
		initEmailAddressesLayout(emailAddressAdapter)


		viewModel.repo.restaurant.observe(viewLifecycleOwner) { restaurant ->
			if (restaurant != null) {

				phoneNumberAdapter.icons.addAll(restaurant.randomUserIcons)
				emailAddressAdapter.icons.addAll(restaurant.randomUserIcons)

				val phoneNumbers = restaurant.adminPhoneNumbers
				if (phoneNumbers.isNotEmpty()) {
					phoneNumberAdapter.submitList(phoneNumbers)
				}

				val emailAddresses = restaurant.adminEmailAddresses
				if (emailAddresses.isNotEmpty()) {
					emailAddressAdapter.submitList(emailAddresses)
				}

				binding.copyAddressBtn.text = restaurant.locationAddress

				binding.copyAddressBtn.setOnClickListener {
					val clipboard = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
					val clip = ClipData.newPlainText("label", binding.copyAddressBtn.text.toString())
					clipboard?.setPrimaryClip(clip)
					toast("Text copied")
				}

				binding.startNavigationBtn.setOnClickListener {
					val gmmIntentUri =
						Uri.parse("google.navigation:q=${restaurant.location.latitude},${restaurant.location.longitude}&mode=l")
					val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
					mapIntent.setPackage("com.google.android.apps.maps")
					startActivity(mapIntent)
				}

			}
		}

	}

	private fun initPhoneNumbersLayout(phoneNumberAdapter: TableItemAdapter) {
		binding.phoneNumberContainer.apply {
			tableHeader.text = "Phone numbers"

			tableRecycler.apply {
				adapter = phoneNumberAdapter
				layoutManager = LinearLayoutManager(requireContext())
			}

			tableEditLayout.hide()
		}
	}

	private fun initEmailAddressesLayout(emailAddressAdapter: TableItemAdapter) {
		binding.emailAddressContainer.apply {
			tableHeader.text = "Email addresses"

			tableRecycler.apply {
				adapter = emailAddressAdapter
				layoutManager = LinearLayoutManager(requireContext())
			}

			tableEditLayout.hide()
		}
	}

	override fun onPrimaryActionClick(item: String) {
		if (item.contains(".")) {
			activity?.composeEmail(getString(R.string.email_subject_alt), item)
		} else {
			val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.split(" ").last()}"))
			startActivity(intent)
		}
	}

}