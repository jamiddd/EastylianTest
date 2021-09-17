package com.jamid.eastyliantest.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.databinding.DeliveryDashboardContentBinding
import com.jamid.eastyliantest.ui.auth.AuthActivity

class DeliveryDashboardFragment: BottomSheetDialogFragment() {

	private lateinit var binding: DeliveryDashboardContentBinding
	private val viewModel: MainViewModel by activityViewModels()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = DeliveryDashboardContentBinding.inflate(inflater)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewModel.repo.currentUser.observe(viewLifecycleOwner) { currentUser ->
			if (currentUser != null) {
				binding.accountName.text = currentUser.name
				binding.accountPhoneNumber.text = currentUser.phoneNo
			}
		}

		binding.logOutBtn.setOnClickListener {
			MaterialAlertDialogBuilder(requireContext())
				.setTitle("Logging out")
				.setMessage("Are you sure you want to log out?")
				.setPositiveButton("Log out") {_, _ ->
					viewModel.signOut()
					val intent = Intent(requireContext(), AuthActivity::class.java)
					intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
					startActivity(intent)
					requireActivity().finish()
				}.setNegativeButton("Cancel") {a, _ ->
					a.cancel()
				}.show()
		}

	}

	companion object {

		const val TAG = "DeliveryDashboard"

		@JvmStatic
		fun newInstance() = DeliveryDashboardFragment()

	}

}