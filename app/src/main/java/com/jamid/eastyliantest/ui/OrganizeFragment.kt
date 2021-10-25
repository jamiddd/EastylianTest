package com.jamid.eastyliantest.ui

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.CAKES
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.CakeMiniAdapter
import com.jamid.eastyliantest.databinding.FragmentOrganizeBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.utility.*

class OrganizeFragment: Fragment(R.layout.fragment_organize) {

	private lateinit var binding: FragmentOrganizeBinding
	private val viewModel: MainViewModel by activityViewModels()
	private lateinit var inStockAdapter: CakeMiniAdapter
	private lateinit var outOfStockAdapter: CakeMiniAdapter
	private val cakeList = mutableListOf<Cake>()
	private val arrowUp: Drawable by lazy { ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_keyboard_arrow_up_24) ?: throw NullPointerException("No drawable found") }
	private val arrowDown: Drawable by lazy { ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_keyboard_arrow_down_24) ?: throw NullPointerException("No drawable found") }


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.organize_menu, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == R.id.add_organize_item) {
			findNavController().navigate(R.id.action_organizeFragment_to_addCakeFragment, null, slideRightNavOptions())
		}
		return super.onOptionsItemSelected(item)
	}


	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentOrganizeBinding.bind(view)

		inStockAdapter = CakeMiniAdapter()
		outOfStockAdapter = CakeMiniAdapter()

		binding.inStockItems.apply {
			adapter = inStockAdapter
			addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
			layoutManager = LinearLayoutManager(requireContext())
		}

		binding.outOfStockRecycler.apply {
			adapter = outOfStockAdapter
			addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
			layoutManager = LinearLayoutManager(requireContext())
		}

		Firebase.firestore.collection(CAKES)
			.addSnapshotListener { value, error ->
				if (error != null) {
					viewModel.setCurrentError(error)
					return@addSnapshotListener
				}

				if (value != null && !value.isEmpty) {
					val cakes = value.toObjects(Cake::class.java)
					cakeList.addAll(cakes)
					val inStockCakes = cakes.filter { cake ->
						cake.available
					}

					if (inStockCakes.isEmpty()) {
						hideInStock()
						binding.inStockNoItems.show()
					} else {
						expandInStock()
						binding.inStockNoItems.hide()
					}

					val outOfStockCakes = cakes.filter { cake ->
						!cake.available
					}

					if (outOfStockCakes.isEmpty()) {
						hideOutOfStock()
						binding.outOfStockNoItems.show()
					} else {
						expandOutOfStock()
						binding.outOfStockNoItems.hide()
					}

					inStockAdapter.submitList(inStockCakes)
					outOfStockAdapter.submitList(outOfStockCakes)
				}
			}
		/*.get()
			.addOnSuccessListener {
				if (!it.isEmpty) {
					val cakes = it.toObjects(Cake::class.java)
					val inStockCakes = cakes.filter { cake ->
						cake.isAvailable
					}

					val outOfStockCakes = cakes.filter { cake ->
						!cake.isAvailable
					}

					inStockAdapter.submitList(inStockCakes)
					outOfStockAdapter.submitList(outOfStockCakes)
				}
			}.addOnFailureListener {
				viewModel.setCurrentError(it)
			}*/

		binding.inStockHeader.setOnClickListener {
			if (binding.inStockItems.isVisible) {
				hideInStock()
			} else {
				expandInStock()
			}
		}

		binding.outOfStockHeader.setOnClickListener {
			if (binding.outOfStockRecycler.isVisible) {
				hideOutOfStock()
			} else {
				expandOutOfStock()
			}
		}

	}



	private fun expandOutOfStock() {
		binding.outOfStockRecycler.show()
		binding.outOfStockHeader.icon = arrowDown
	}

	private fun hideOutOfStock() {
		binding.outOfStockRecycler.hide()
		binding.outOfStockHeader.icon = arrowUp
	}

	private fun expandInStock() {
		binding.inStockItems.show()
		binding.inStockHeader.icon = arrowDown
	}

	private fun hideInStock() {
		binding.inStockItems.hide()
		binding.inStockHeader.icon = arrowUp
	}

	/*companion object {
		private const val TAG = "OrganizeFragment"
	}*/

}