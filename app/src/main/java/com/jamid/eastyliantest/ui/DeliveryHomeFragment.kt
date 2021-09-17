package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentDeliveryHomeBinding

class DeliveryHomeFragment : Fragment(R.layout.fragment_delivery_home) {

	private lateinit var binding: FragmentDeliveryHomeBinding

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentDeliveryHomeBinding.bind(view)



	}

	companion object {

		@JvmStatic
		fun newInstance() = DeliveryHomeFragment()
	}
}