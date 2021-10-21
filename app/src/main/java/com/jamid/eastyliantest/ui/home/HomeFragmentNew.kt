package com.jamid.eastyliantest.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.BaseCakeTypeAdapter
import com.jamid.eastyliantest.adapter.CakeAdapter
import com.jamid.eastyliantest.databinding.FragmentHomeNewBinding
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.utility.slideRightNavOptions

class HomeFragmentNew: Fragment() {

    private lateinit var binding: FragmentHomeNewBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeNewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val snapHelper1 = LinearSnapHelper()

        val cakeAdapter = CakeAdapter()

        binding.topCakesRecycler.apply {
            adapter = cakeAdapter
            itemAnimator = null
            onFlingListener = null
            snapHelper1.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.repo.customCakes.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                Log.d(TAG, "Got the cakes")
                cakeAdapter.submitList(it)
            }
        }

        val baseCakeTypeAdapter = BaseCakeTypeAdapter()
        binding.mainCategoryRecycler.apply {
            adapter = baseCakeTypeAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        val snapHelper = LinearSnapHelper()
        val baseCakeTypeAdapter1 = BaseCakeTypeAdapter()

        binding.flavoursRecycler.apply {
            adapter = baseCakeTypeAdapter1
            onFlingListener = null
            snapHelper.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        viewModel.baseMenuItems.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                baseCakeTypeAdapter.submitList(it)
            }
        }

        viewModel.flavorMenuItems.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                baseCakeTypeAdapter1.submitList(it)
            }
        }

        binding.customCakeLayout.contactUsBtn.setOnClickListener {
            findNavController().navigate(R.id.action_containerFragment_to_contactFragment3, null, slideRightNavOptions())
        }

    }

    companion object {
        private const val TAG = "HomeFragmentNew"
    }

}