package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.EASTYLIAN
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.RESTAURANT
import com.jamid.eastyliantest.adapter.BaseCakeTypeAdapter
import com.jamid.eastyliantest.databinding.FragmentChangeMenuBinding
import com.jamid.eastyliantest.model.CakeMenuItem
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.slideRightNavOptions

class ChangeMenuFragment: Fragment(R.layout.fragment_change_menu) {

	private lateinit var binding: FragmentChangeMenuBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.change_menu_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == R.id.create_new_cake) {
			findNavController().navigate(R.id.action_changeMenuFragment_to_addMenuItemFragment, null, slideRightNavOptions())
		}
		return super.onOptionsItemSelected(item)
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentChangeMenuBinding.bind(view)

		val baseCakeAdapter1 = BaseCakeTypeAdapter().apply { isAdmin = true }
		val baseCakeAdapter2 = BaseCakeTypeAdapter().apply { isAdmin = true }

		binding.cakeBaseRecycler.apply {
			adapter = baseCakeAdapter1
			layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
		}

		binding.cakeFlavorRecycler.apply {
			adapter = baseCakeAdapter2
			layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
		}

		Firebase.firestore.collection(RESTAURANT).document(EASTYLIAN)
			.collection("menuItems")
			.addSnapshotListener { value, error ->
				if (error != null) {
					Log.d("Something", error.localizedMessage!!)
					return@addSnapshotListener
				}

				if (value != null) {
					val cakeMenuItems = value.toObjects(CakeMenuItem::class.java)

					val first = cakeMenuItems.filter { it1 ->
						it1.category != "flavor"
					}

					if (first.isEmpty()) {
						binding.cakeBaseRecycler.hide()
						binding.noBaseCakes.show()
					} else {
						binding.cakeBaseRecycler.show()
						binding.noBaseCakes.hide()
					}

					val second = cakeMenuItems.filter { it1 ->
						it1.category == "flavor"
					}

					if (second.isEmpty()) {
						binding.cakeFlavorRecycler.hide()
						binding.noFlavorCakes.show()
					} else {
						binding.cakeFlavorRecycler.show()
						binding.noFlavorCakes.hide()
					}

					baseCakeAdapter1.submitList(first)
					baseCakeAdapter2.submitList(second)
				}
			}

	}

}