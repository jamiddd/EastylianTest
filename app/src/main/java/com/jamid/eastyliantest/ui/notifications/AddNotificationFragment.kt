package com.jamid.eastyliantest.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.NOTIFICATIONS
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentAddNotificationBinding
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.ui.AdminActivity
import com.jamid.eastyliantest.utility.*

class AddNotificationFragment: Fragment(R.layout.fragment_add_notification) {

	private lateinit var binding: FragmentAddNotificationBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var notificationImage: String? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAddNotificationBinding.bind(view)

		binding.fragmentAddNotificationToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
			binding.fragmentAddNotificationToolbar.updateLayout(marginTop = top)
		}

		viewModel.currentImage.observe(viewLifecycleOwner) { currentImageUri ->
			if (currentImageUri != null) {
				viewModel.uploadImage(currentImageUri) { downloadUrl ->
					binding.notificationImageProgress.show()
					notificationImage = downloadUrl?.toString()
					binding.notificationImg.setImageURI(notificationImage)
					binding.notificationImg.show()
					binding.notificationImageProgress.hide()
				}

				binding.addNotificationImageBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_close_24)
				binding.addNotificationImageBtn.text = "Remove image"
				binding.addNotificationImageBtn.setOnClickListener {
					notificationImage = null
					viewModel.setCurrentImage(null)
				}
			} else {

				binding.addNotificationImageBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_add_photo_alternate_24)
				binding.addNotificationImageBtn.text = "Add image"
				binding.notificationImageProgress.hide()
				binding.notificationImg.hide()
				notificationImage = null
				binding.notificationImg.hide()
				binding.notificationImg.setImageURI(notificationImage)

				binding.addNotificationImageBtn.setOnClickListener {
					(activity as AdminActivity?)?.selectImage()
				}

			}
		}

		binding.notificationContentLayout.editText?.doAfterTextChanged {
			if (!binding.notificationTitleLayout.editText?.text.isNullOrBlank() && !it.isNullOrBlank()) {
				binding.createNotificationBtn.enable()
			} else {
				binding.createNotificationBtn.disable()
			}
		}

		binding.notificationTitleLayout.editText?.doAfterTextChanged {
			if (!binding.notificationContentLayout.editText?.text.isNullOrBlank() && !it.isNullOrBlank()) {
				binding.createNotificationBtn.enable()
			} else {
				binding.createNotificationBtn.disable()
			}
		}

		binding.createNotificationBtn.setOnClickListener {
			val titleText = binding.notificationTitleLayout.editText?.text
			if (titleText.isNullOrBlank()) {
				return@setOnClickListener
			}

			val contentText = binding.notificationContentLayout.editText?.text
			if (contentText.isNullOrBlank()) {
				return@setOnClickListener
			}

			val newNotificationRef = Firebase.firestore.collection(NOTIFICATIONS).document()

			val notification = SimpleNotification(newNotificationRef.id, contentText.toString(), titleText.toString(), notificationImage, System.currentTimeMillis())

			newNotificationRef.set(notification)
				.addOnSuccessListener {
					Toast.makeText(
						requireContext(),
						"Sent notification to Topic: General.",
						Toast.LENGTH_SHORT
					).show()
					findNavController().navigateUp()
				}.addOnFailureListener {
					viewModel.setCurrentError(it)
				}
		}

	}

	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.setCurrentImage(null)
	}

}