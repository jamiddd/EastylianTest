package com.jamid.eastyliantest.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.NOTIFICATIONS
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentAddNotificationBinding
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.ui.AdminActivity
import com.jamid.eastyliantest.ui.MainViewModel
import com.jamid.eastyliantest.utility.*

class AddNotificationFragment: Fragment(R.layout.fragment_add_notification) {

	private lateinit var binding: FragmentAddNotificationBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var notificationImage: String? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAddNotificationBinding.bind(view)

		val existingNotification = arguments?.getParcelable<SimpleNotification>("notification")
		if (existingNotification != null) {
			binding.notificationTitleLayout.editText?.setText(existingNotification.title)
			binding.notificationContentLayout.editText?.setText(existingNotification.content)
			binding.createNotificationBtn.enable()
		}

		var justStarted = true

		viewModel.currentImage.observe(viewLifecycleOwner) { currentImageUri ->
			binding.notificationImageProgress.show()
			if (currentImageUri != null) {
				viewModel.uploadImage(currentImageUri) { downloadUrl ->
					onImageChanged(downloadUrl?.toString())
				}
			} else {
				onImageChanged(null)
				if (existingNotification != null && justStarted) {
					onImageChanged(existingNotification.image)
					justStarted = false
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

			viewModel.uploadNotification(notification) {
				if (it.isSuccessful) {
					toast("Sent notification to Topic: General.")
				} else {
					it.exception?.let {it1 ->
						viewModel.setCurrentError(it1)
					}
				}
			}
		}

	}

	private fun onImageChanged(image: String?) {
		notificationImage = image
		binding.notificationImageProgress.hide()
		binding.notificationImg.setImageURI(notificationImage)
		if (image != null) {
			binding.addNotificationImageBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_close_24)
			binding.addNotificationImageBtn.text = "Remove image"
			binding.notificationImg.show()

			binding.addNotificationImageBtn.setOnClickListener {
				notificationImage = null
				viewModel.setCurrentImage(null)
			}

		} else {
			binding.addNotificationImageBtn.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_add_photo_alternate_24)
			binding.addNotificationImageBtn.text = "Add image"
			binding.notificationImg.hide()

			binding.addNotificationImageBtn.setOnClickListener {
				(activity as AdminActivity?)?.selectImage()
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.setCurrentImage(null)
	}

}