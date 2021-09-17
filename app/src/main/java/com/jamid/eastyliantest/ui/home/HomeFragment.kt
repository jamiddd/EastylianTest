package com.jamid.eastyliantest.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.google.android.material.appbar.AppBarLayout
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.BaseCakeTypeAdapter
import com.jamid.eastyliantest.adapter.CakeAdapter
import com.jamid.eastyliantest.databinding.FragmentHomeBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.utility.convertDpToPx
import com.jamid.eastyliantest.utility.slideRightNavOptions
import com.jamid.eastyliantest.utility.updateLayout

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: MainViewModel by activityViewModels()

    init {
        Log.d(TAG, "Home fragment running.")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        binding.homeAppBar.setExpanded(viewModel.isHomeAppBarExpanded)

        binding.homeAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.isHomeAppBarExpanded = verticalOffset == 0
        })

        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.name.isNotBlank()) {
                    binding.currentLocationText.text = it.name
                } else {
                    binding.currentLocationText.text = getString(R.string.no_location_text)
                }
            } else {
                binding.currentLocationText.text = getString(R.string.no_location_text)
            }
        }

        binding.currentLocationText.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_locationFragment)
        }

        val snapHelper1 = LinearSnapHelper()
        val flavors = Flavor.values()

        val cakeAdapter = CakeAdapter()
        cakeAdapter.addedCakeList = viewModel.addedCakeList

        binding.topCakesRecycler.apply {
            adapter = cakeAdapter
            onFlingListener = null
            snapHelper1.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.repo.customCakes.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                Log.d(TAG, "${it.size} Cakes")
                cakeAdapter.submitList(it)
            } else {
                Log.d(TAG, "No cakes")
            }
        }

        val c1 = Cake.newInstance(getString(R.string.fondant))
        val c2 = Cake.newInstance(getString(R.string.sponge))
        c1.isCustomizable = true
        c2.isCustomizable = true

        val cakes1 = listOf(c1, c2)

        val baseCakeTypeAdapter = BaseCakeTypeAdapter(cakes1)
        binding.mainCategoryRecycler.apply {
            adapter = baseCakeTypeAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        val cakes2 = mutableListOf<Cake>()
        for (flavor in flavors) {
            if (flavor != Flavor.NONE && flavor != Flavor.VANILLA) {
                val cake = Cake.newFlavorInstance(listOf(Flavor.VANILLA, flavor))
                cake.isCustomizable = true
                cakes2.add(cake)
            }
        }

        val snapHelper = LinearSnapHelper()
        val baseCakeTypeAdapter1 = BaseCakeTypeAdapter(cakes2)

        binding.flavoursRecycler.apply {
            adapter = baseCakeTypeAdapter1
            onFlingListener = null
            snapHelper.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.homeToolbar.updateLayout(marginTop = top)
            binding.homeParentScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))
        }

        binding.customCakeLayout.contactUsBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactFragment, null, slideRightNavOptions())
        }

    }

    companion object {
        private const val TAG = "HomeFragment"

        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}