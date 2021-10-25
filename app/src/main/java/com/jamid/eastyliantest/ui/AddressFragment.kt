package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.SimplePlaceAdapter
import com.jamid.eastyliantest.databinding.FragmentAddressBinding
import com.jamid.eastyliantest.interfaces.OnAddressClickListener
import com.jamid.eastyliantest.model.SimplePlace


class AddressFragment: Fragment(R.layout.fragment_address), OnAddressClickListener {

	private lateinit var binding: FragmentAddressBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var previousViewHolder: RecyclerView.ViewHolder? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAddressBinding.bind(view)

		val white = ContextCompat.getColor(requireContext(), R.color.white)

		viewModel.currentPlace.observe(viewLifecycleOwner) {
			if (it != null) {
				binding.currentAddressItem.locationItem.setTextColor(white)
				binding.currentAddressItem.locationAddress.setTextColor(white)

				binding.currentAddressItem.locationItem.text = it.name
				binding.currentAddressItem.locationAddress.text = it.address
			}
		}

		val simplePlaceAdapter = SimplePlaceAdapter(this as OnAddressClickListener)

		binding.addressRecycler.apply {
			adapter = simplePlaceAdapter
			layoutManager = LinearLayoutManager(requireContext())
			addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
		}

		viewModel.repo.pastPlaces.observe(viewLifecycleOwner) {
			if (!it.isNullOrEmpty()) {
				it.filter { it1 ->
					it1.name.isNotBlank()
				}.apply {
					simplePlaceAdapter.submitList(this)
				}
			}
		}

		binding.changeLocationBtn.setOnClickListener {
			activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_addressFragment2_to_locationFragment4)
		}

	}

	override fun onCurrentAddressSelected(viewHolder: RecyclerView.ViewHolder, place: SimplePlace) {
		if (viewHolder is SimplePlaceAdapter.SimplePlaceViewHolder) {
			viewHolder.hideButton()
			previousViewHolder = viewHolder
		}
		place.isUserAccurate = true
		viewModel.confirmPlace(place)
	}

	override fun onAddressItemClick(viewHolder: RecyclerView.ViewHolder) {
		if (previousViewHolder is SimplePlaceAdapter.SimplePlaceViewHolder) {
			(previousViewHolder as SimplePlaceAdapter.SimplePlaceViewHolder).hideButton()
		}

		if (viewHolder is SimplePlaceAdapter.SimplePlaceViewHolder) {
			if (viewHolder == previousViewHolder) {
				previousViewHolder = null
				viewHolder.hideButton()
			} else {
				viewHolder.showButton()
				previousViewHolder = viewHolder
			}
		}
	}

}