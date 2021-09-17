package com.jamid.eastyliantest.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.CakeAdapter
import com.jamid.eastyliantest.databinding.FragmentFavoritesBinding
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.updateLayout

class FavoritesFragment: Fragment(R.layout.fragment_favorites) {

	private lateinit var binding: FragmentFavoritesBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentFavoritesBinding.bind(view)

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
			binding.fragmentFavoritesToolbar.updateLayout(marginTop = top)
		}

//		val manager = GridLayoutManager(requireContext(), 2)

		val cakeAdapter = CakeAdapter()
		cakeAdapter.isVertical = true
		binding.favoriteCakesRecycler.apply {
			layoutManager = LinearLayoutManager(requireContext())
			adapter = cakeAdapter
		}

		viewModel.favoriteCakes.observe(viewLifecycleOwner) {
			if (!it.isNullOrEmpty()) {
				binding.noFavoritesText.hide()
				binding.favoriteCakesRecycler.show()
				cakeAdapter.submitList(it)
			} else {
				binding.noFavoritesText.show()
				binding.favoriteCakesRecycler.hide()
			}
		}

		binding.fragmentFavoritesToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

	}

}