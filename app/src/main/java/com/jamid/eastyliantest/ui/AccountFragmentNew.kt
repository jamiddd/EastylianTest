package com.jamid.eastyliantest.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.OrderAdapter
import com.jamid.eastyliantest.databinding.FragmentAccountNewBinding
import com.jamid.eastyliantest.databinding.SimpleTextDialogLayoutBinding
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.*

class AccountFragmentNew: Fragment() {

    private lateinit var binding: FragmentAccountNewBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var currentImage: String? = null
    private var justStarted = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountNewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderAdapter = OrderAdapter(viewLifecycleOwner.lifecycleScope)
        val orderAdapter1 = OrderAdapter(viewLifecycleOwner.lifecycleScope)

        binding.pastOrdersRecycler.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.currentOrdersRecycler.apply {
            adapter = orderAdapter1
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.repo.currentUser.observe(viewLifecycleOwner) {
            if (it != null) {
                setUserLayout(it)
            }
        }


        viewModel.currentImage.observe(viewLifecycleOwner) {
            binding.customerLayout.uploadProgress.show()
            if (it != null) {
                viewModel.uploadImage(it) { downloadUri ->
                    if (downloadUri != null) {
                        viewModel.updateFirebaseUser(mapOf("photoUrl" to downloadUri.toString())) { it1 ->
                            if (it1.isSuccessful) {
                                binding.customerLayout.uploadProgress.hide()
                                val changes1 = mapOf("photoUrl" to downloadUri.toString())
                                viewModel.updateUser(changes1)
                            } else {
                                toast("Something went wrong while updating user data.")
                            }
                        }
                    }
                }
            } else {
                binding.customerLayout.uploadProgress.hide()
                currentImage = null
                if (!justStarted) {
                    viewModel.updateFirebaseUser(mapOf("photoUrl" to null)) {
                        val changes1 = mapOf("photoUrl" to null)
                        viewModel.updateUser(changes1)
                    }
                }
            }
        }

        viewModel.repo.allOrders.observe(viewLifecycleOwner) {
            if (it != null) {

                Log.d(TAG, it.toString())

                val orders = it.map { it1 ->
                    it1.order.items = it1.cartItems
                    it1.order
                }

                if (orders.isNullOrEmpty()) {
                    binding.noOrdersText.show()
                } else {
                    binding.noOrdersText.hide()
                }


                val pastOrders = orders.filter { it1 ->
                    it1.status.first() == OrderStatus.Delivered || it1.status.first() == OrderStatus.Cancelled
                }.sortedByDescending { it1 ->
                    it1.deliveryAt
                }

                val currentOrders = orders.filter { it1 ->
                    it1.status[0] == OrderStatus.Paid || it1.status[0] == OrderStatus.Due || it1.status[0] == OrderStatus.Preparing || it1.status[0] == OrderStatus.Delivering
                }

                for (order in pastOrders) {
                    Log.d(TAG, "Past orders - " + order.status.toString())
                }

                if (pastOrders.isEmpty()) {
                    binding.pastOrdersRecycler.hide()
                    binding.pastOrdersHeader.hide()
                    binding.seeAllPastOrdersBtn.hide()
                } else {

                    if (pastOrders.size < 2) {
                        binding.seeAllPastOrdersBtn.hide()
                    }

                    binding.pastOrdersRecycler.show()
                    binding.pastOrdersHeader.show()
                    binding.seeAllPastOrdersBtn.show()

                    orderAdapter.submitList(pastOrders.take(2).sortedByDescending { it1 ->
                        it1.createdAt
                    })
                }

                if (currentOrders.isEmpty()) {
                    binding.currentOrdersRecycler.hide()
                    binding.currentOrdersHeader.hide()

                } else {
                    binding.currentOrdersHeader.show()
                    binding.currentOrdersRecycler.show()

                    orderAdapter1.submitList(currentOrders.sortedByDescending { it1 ->
                        it1.createdAt
                    })

                    /*requireActivity().setCurrentOrderListeners(currentOrders, viewModel)*/
                }

            } else {

                binding.pastOrdersRecycler.hide()
                binding.pastOrdersHeader.hide()
                binding.noOrdersText.show()
            }
        }

        binding.refundBtn.setOnClickListener {
            activity?.findNavController(R.id.nav_host_fragment)?.navigate(
                R.id.action_containerFragment_to_refundFragment3,
                null,
                slideRightNavOptions()
            )
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            binding.accountScroll.setPadding(
                0,
                convertDpToPx(8),
                0,
                bottom + convertDpToPx(100)
            )
        }

        binding.changeAddressBtn.setOnClickListener {
            activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_containerFragment_to_addressFragment2, null, slideRightNavOptions())
        }

        binding.favoritesBtn.setOnClickListener {
            activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_containerFragment_to_favoritesFragment2, null, slideRightNavOptions())
        }

        binding.helpBtn.setOnClickListener {
            activity?.findNavController(R.id.nav_host_fragment)?.navigate(R.id.action_containerFragment_to_helpFragment2, null, slideRightNavOptions())
        }

        binding.pastOrdersHeader.setOnClickListener {
            if (binding.pastOrdersRecycler.isVisible) {
                binding.pastOrdersHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_round_keyboard_arrow_down_24,
                    0
                )
                binding.pastOrdersRecycler.hide()
                binding.seeAllPastOrdersBtn.hide()
            } else {
                binding.pastOrdersHeader.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_round_keyboard_arrow_up_24,
                    0
                )
                binding.pastOrdersRecycler.show()
                binding.seeAllPastOrdersBtn.show()
            }
        }

        binding.seeAllPastOrdersBtn.setOnClickListener {
            activity?.findNavController(R.id.nav_host_fragment)?.navigate(
                R.id.action_containerFragment_to_pastOrdersFragment2,
                null,
                slideRightNavOptions()
            )
        }

        binding.logOutBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logging Out ...")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out") { _, _ ->
                    viewModel.signOut()
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }.setNegativeButton("Cancel") { a, _ ->
                    a.dismiss()
                }.show()
        }

    }

    private fun setUserLayout(currentUser: User) {
        binding.customerLayout.apply {

            customerName.text = currentUser.name

            customerMeta.text = "${currentUser.phoneNo} • ${currentUser.email}"

            if (currentUser.photoUrl != null) {
                customerImg.setImageURI(currentUser.photoUrl)
            } else {
                val restaurant = viewModel.repo.restaurant.value
                if (restaurant != null) {
                    val images = restaurant.randomUserIcons
                    if (images.isNotEmpty()) {
                        customerImg.setImageURI(images.random())
                    }
                } else {
                    Log.d(TAG, "Restaurant is null.")
                }
            }

            customerImg.setOnClickListener {
                val popupMenu = PopupMenu(requireContext(), it)

                popupMenu.inflate(R.menu.image_menu)

                popupMenu.setOnMenuItemClickListener { it1 ->
                    justStarted = false
                    when (it1.itemId) {
                        R.id.change_image -> {
                            (activity as MainActivity2?)?.selectImage()
                        }
                        R.id.remove_image -> {
                            viewModel.setCurrentImage(null)
                        }
                    }
                    return@setOnMenuItemClickListener true
                }

                popupMenu.show()
            }

            customerName.text = currentUser.name

            customerEditBtn.setOnClickListener {

                val layout = layoutInflater.inflate(R.layout.simple_text_dialog_layout, null, false)
                val inputLayoutBinding = SimpleTextDialogLayoutBinding.bind(layout)

                inputLayoutBinding.dialogHeader.text = "Change your name"
                inputLayoutBinding.dialogHelperText.hide()

                inputLayoutBinding.dialogInputLayout.editText?.hint = "Write your name ... "

                val alertDialog = MaterialAlertDialogBuilder(requireContext())
                    .setView(inputLayoutBinding.root)
                    .show()

                inputLayoutBinding.dialogPositiveBtn.setOnClickListener {

                    val nameText = inputLayoutBinding.dialogInputLayout.editText?.text
                    if (!nameText.isNullOrBlank()) {
                        val name = nameText.toString()
                        val changes = mapOf("fullName" to name)

                        viewModel.updateFirebaseUser(changes) {
                            val changes1 = mapOf("name" to name)
                            viewModel.updateUser(changes1)
                            alertDialog.dismiss()
                        }
                        alertDialog.dismiss()
                    } else {
                        alertDialog.dismiss()
                    }

                }

                inputLayoutBinding.dialogNegativeBtn.setOnClickListener {
                    alertDialog.dismiss()
                }

            }

        }
    }

    companion object {
        private const val TAG = "AccountFragment"
    }

}