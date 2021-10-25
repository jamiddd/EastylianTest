package com.jamid.eastyliantest.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.DeliveryDashboardContentBinding
import com.jamid.eastyliantest.databinding.SimpleTextDialogLayoutBinding
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.toast

class DeliveryDashboardFragment: BottomSheetDialogFragment() {

	private lateinit var binding: DeliveryDashboardContentBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var currentImage: String? = null
	private var justStarted = true

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
				binding.accountProfileImage.setImageURI(currentUser.photoUrl)
			}
		}

		viewModel.repo.restaurant.observe(viewLifecycleOwner) {
			if (it != null) {
				binding.restaurantName.text = it.name
				binding.appVersion.text = it.version
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

		viewModel.currentImage.observe(viewLifecycleOwner) {
			binding.deliveryExecutivePhotoProgress.show()
			if (it != null) {
				viewModel.uploadImage(it) { downloadUri ->
					if (downloadUri != null) {
						viewModel.updateFirebaseUser(mapOf("photoUrl" to downloadUri.toString())) { it1 ->
							if (it1.isSuccessful) {
								binding.deliveryExecutivePhotoProgress.hide()
								val changes1 = mapOf("photoUrl" to downloadUri.toString())
								viewModel.updateUser(changes1)
							} else {
								toast("Something went wrong while updating user data.")
							}
						}
					}
				}
			} else {
				binding.deliveryExecutivePhotoProgress.hide()
				currentImage = null
				if (!justStarted) {
					viewModel.updateFirebaseUser(mapOf("photoUrl" to null)) {
						val changes1 = mapOf("photoUrl" to null)
						viewModel.updateUser(changes1)
					}
				}
			}
		}

		binding.accountProfileImage.setOnClickListener {

			val choices = arrayOf("Change Image", "Remove")

			MaterialAlertDialogBuilder(requireContext())
				.setItems(choices) { a, index ->
					if (index == 0) {
						(activity as DeliveryActivity?)?.selectImage()
					} else {
						viewModel.setCurrentImage(null)
					}
				}
				.show()
		}

		binding.changeNameBtn.setOnClickListener {
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

	override fun onDestroy() {
		super.onDestroy()
		viewModel.setCurrentImage(null)
	}

	companion object {

		const val TAG = "DeliveryDashboard"

		@JvmStatic
		fun newInstance() = DeliveryDashboardFragment()

	}

}