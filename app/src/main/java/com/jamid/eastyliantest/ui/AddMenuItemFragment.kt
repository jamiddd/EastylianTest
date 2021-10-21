package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.EASTYLIAN
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.RESTAURANT
import com.jamid.eastyliantest.databinding.FragmentAddMenuItemBinding
import com.jamid.eastyliantest.model.CakeMenuItem
import com.jamid.eastyliantest.utility.*

class AddMenuItemFragment: Fragment(R.layout.fragment_add_menu_item) {

	private lateinit var binding: FragmentAddMenuItemBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var uploadedImage: String? = null
	private var hasImageChanged = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentAddMenuItemBinding.bind(view)

		val cakeMenuItem = arguments?.getParcelable<CakeMenuItem>("cakeMenuItem")

		if (cakeMenuItem != null) {

			binding.menuItemTextLayout.editText?.setText(cakeMenuItem.title)

			if (cakeMenuItem.category == "flavor") {
				binding.radioGroup.check(R.id.category_flavor)
			} else {
				binding.radioGroup.check(R.id.category_base)
			}

			binding.additemBtn.text = "Update item"
		}


		binding.menuItemImage.setColorFilter(ContextCompat.getColor(requireContext(), R.color.semi_transparent_dark))

		viewModel.currentImage.observe(viewLifecycleOwner) {
			if (it != null) {
				hasImageChanged = true
				viewModel.uploadImage(it) { it1 ->

					binding.addMenuItemProgress.hide()
					binding.selectImageBtn.show()

					if (it1 != null) {
						binding.selectImageBtn.text = "Remove image"
						binding.selectImageBtn.setOnClickListener {
							viewModel.setCurrentImage(null)
						}
						uploadedImage = it1.toString()
						binding.menuItemImage.setImageURI(uploadedImage)
					} else {
						toast("Something went wrong while uploading image.")
						// something went wrong
					}
				}
			} else {
				binding.selectImageBtn.text = "Select image"
				binding.selectImageBtn.setOnClickListener {
					binding.selectImageBtn.hide()
					binding.addMenuItemProgress.show()
					(activity as AdminActivity?)?.selectImage()
				}
				uploadedImage = null
				binding.menuItemImage.setImageURI(uploadedImage)

				if (!hasImageChanged && cakeMenuItem != null) {
					uploadedImage = cakeMenuItem.image
					binding.menuItemImage.setImageURI(uploadedImage)
					binding.selectImageBtn.text = "Change image"
				}
			}
		}

		binding.additemBtn.setOnClickListener {

			val title = binding.menuItemTextLayout.editText?.text
			if (title.isNullOrBlank()) {
				toast("Title cannot be empty.")
				return@setOnClickListener
			}

			val price = binding.priceTextLayout.editText?.text
			if (price.isNullOrBlank()) {
				toast("Price must be provided.")
				return@setOnClickListener
			}

			if (uploadedImage == null) {
				toast("Image must be uploaded.")
				return@setOnClickListener
			}

			val cakeMenuItem1 = if (cakeMenuItem != null) {
				CakeMenuItem(cakeMenuItem.id, title.toString(), uploadedImage!!, category, price.toString().toLong())
			} else {
				CakeMenuItem(randomId(), title.toString(), uploadedImage!!, category, price.toString().toLong())
			}

			// TODO("Remove this from here and put it in firebase utility")
			Firebase.firestore.collection(RESTAURANT)
				.document(EASTYLIAN)
				.collection("menuItems")
				.document(cakeMenuItem1.id)
				.set(cakeMenuItem1)
				.addOnSuccessListener {
					if (cakeMenuItem != null) {
						toast("Updated menu item.")
					} else {
						toast("Added menu item.")
					}
					findNavController().navigateUp()
				}.addOnFailureListener {
					toast("Something went wrong. ${it.localizedMessage}")
				}

		}

	}

	private val category: String
		get() = when (binding.radioGroup.checkedRadioButtonId) {
			R.id.category_base -> "base"
			R.id.category_flavor -> "flavor"
			else -> "flavor"
		}

	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.setCurrentImage(null)
	}

}