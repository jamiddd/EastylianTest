package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jamid.eastyliantest.CAKE
import com.jamid.eastyliantest.IS_EDIT_MODE
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentBuildCakeBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.updateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BuildCakeFragment: Fragment(R.layout.fragment_build_cake) {

	private lateinit var binding: FragmentBuildCakeBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var currentImage: String? = null
	private var previousCake: Cake? = null
	private var isEditMode = false

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentBuildCakeBinding.bind(view)

		isEditMode = arguments?.getBoolean(IS_EDIT_MODE) ?: false
		previousCake = arguments?.getParcelable(CAKE)

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
			binding.fragmentAddCakeToolbar.updateLayout(marginTop = top)
		}

		binding.fragmentAddCakeToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		binding.cakeNameLayout.editText?.doAfterTextChanged {
			binding.cakeNameLayout.error = null
			binding.cakeNameLayout.isErrorEnabled = false
		}

		binding.cakeAmountLayout.editText?.doAfterTextChanged {
			binding.cakeAmountLayout.error = null
			binding.cakeAmountLayout.isErrorEnabled = false
		}

		binding.shortDescriptionLayout.editText?.doAfterTextChanged {
			binding.shortDescriptionLayout.error = null
			binding.shortDescriptionLayout.isErrorEnabled = false
		}

		binding.addCake.setOnClickListener {
			val nameText = binding.cakeNameLayout.editText?.text
			if (nameText.isNullOrBlank()) {
				binding.cakeNameLayout.isErrorEnabled = true
				binding.cakeNameLayout.error = getString(R.string.cake_name_error)
				return@setOnClickListener
			}

			val shortDescriptionText = binding.shortDescriptionLayout.editText?.text
			if (shortDescriptionText.isNullOrBlank()) {
				binding.shortDescriptionLayout.isErrorEnabled = true
				binding.shortDescriptionLayout.error = getString(R.string.cake_description_error)
				return@setOnClickListener
			}

			val amountText = binding.cakeAmountLayout.editText?.text
			if (amountText.isNullOrBlank()) {
				binding.cakeNameLayout.isErrorEnabled = true
				binding.cakeNameLayout.error = getString(R.string.amount_error)
				return@setOnClickListener
			}

			if (currentImage == null && !isEditMode) {
				Toast.makeText(
					requireContext(),
					getString(R.string.image_not_uploaded_error),
					Toast.LENGTH_SHORT
				).show()
				return@setOnClickListener
			}

			val cakeName = nameText.toString()
			val cakeDesc = shortDescriptionText.toString()
			val cakeAmount = amountText.toString().toLong()
			val cakeFlavors = getFlavors(binding.flavorGroup.checkedChipIds)

			val cake = Cake.newInstance(cakeName, cakeFlavors, cakeDesc, 0.5f, cakeAmount, currentImage)

			if (isEditMode && previousCake != null) {
				previousCake?.apply {
					fancyName = cakeName
					description = cakeDesc
					price = cakeAmount
					flavors = cakeFlavors
					if (currentImage != null) {
						thumbnail = currentImage
					}
				}

				viewModel.updateCakeInDatabase(previousCake!!)
			} else {
				viewModel.addCakeToDatabase(cake)
			}


			findNavController().navigateUp()
		}

		binding.addImageBtn.setOnClickListener {
			(activity as AdminActivity?)?.selectImage()
		}

		viewModel.currentImage.observe(viewLifecycleOwner) {
			if (it != null) {
				binding.addImageBtn.hide()
				binding.imageUploadProgressBar.show()
				viewModel.uploadImage(it) { it1 ->
					binding.imageUploadProgressBar.hide()
					currentImage = it1?.toString()
					binding.cakeImageView.setImageURI(currentImage)
				}
			} else {
				binding.addImageBtn.show()
				currentImage = null
				binding.cakeImageView.setImageURI(currentImage)
			}

		}

		binding.cakeImageView.setOnClickListener {
			if (currentImage != null) {
				val popUp = PopupMenu(requireContext(), it)
				popUp.gravity = Gravity.BOTTOM
				popUp.inflate(R.menu.image_menu)
				popUp.setOnMenuItemClickListener { it1 ->
					when (it1.itemId) {
						R.id.change_image -> {
							(activity as AdminActivity?)?.selectImage()
						}
						R.id.remove_image -> {
							viewModel.setCurrentImage(null)
						}
					}
					true
				}
				popUp.show()
			}
		}

		if (isEditMode && previousCake != null) {
			binding.fragmentAddCakeToolbar.title = getString(R.string.update_cake_details)
			setUpFromPreviousCake()
		} else {
			binding.fragmentAddCakeToolbar.title = getString(R.string.add_cake)
		}

	}

	private fun setUpFromPreviousCake() {

		viewLifecycleOwner.lifecycleScope.launch {
			delay(1000)
			binding.addImageBtn.hide()
			binding.cakeImageView.setImageURI(previousCake?.thumbnail)
		}

		binding.cakeNameLayout.editText?.setText(previousCake!!.fancyName)

		for (flavor in previousCake!!.flavors) {
			binding.flavorGroup.check(getFlavourCheckId(flavor))
		}

		binding.shortDescriptionLayout.editText?.setText(previousCake!!.description)

		binding.cakeAmountLayout.editText?.setText(previousCake!!.price.toString())

		currentImage = previousCake?.thumbnail

	}

	private fun getFlavors(ids: List<Int>) = ids.map {
		getFlavor(it)
	}

	private fun getFlavor(id: Int): Flavor {
		return when (id) {
			R.id.blackForest -> Flavor.BLACK_FOREST
			R.id.whiteForest -> Flavor.WHITE_FOREST
			R.id.vanilla -> Flavor.VANILLA
			R.id.chocolateFantasy -> Flavor.CHOCOLATE_FANTASY
			R.id.redVelvet -> Flavor.RED_VELVET
			R.id.hazelnut -> Flavor.HAZELNUT
			R.id.mango -> Flavor.MANGO
			R.id.strawberry -> Flavor.STRAWBERRY
			R.id.kiwi -> Flavor.KIWI
			R.id.orange -> Flavor.ORANGE
			R.id.pineapple -> Flavor.PINEAPPLE
			R.id.butterscotch -> Flavor.BUTTERSCOTCH
			else -> Flavor.NONE
		}
	}

	private fun getFlavourCheckId(flavor: Flavor): Int {
		return when (flavor) {
			Flavor.BLACK_FOREST -> R.id.blackForest
			Flavor.WHITE_FOREST -> R.id.whiteForest
			Flavor.VANILLA -> R.id.vanilla
			Flavor.CHOCOLATE_FANTASY -> R.id.chocolateFantasy
			Flavor.RED_VELVET -> R.id.redVelvet
			Flavor.HAZELNUT -> R.id.hazelnut
			Flavor.MANGO -> R.id.mango
			Flavor.STRAWBERRY -> R.id.strawberry
			Flavor.KIWI -> R.id.kiwi
			Flavor.ORANGE -> R.id.orange
			Flavor.PINEAPPLE -> R.id.pineapple
			Flavor.BUTTERSCOTCH -> R.id.butterscotch
			Flavor.NONE -> 0
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.setCurrentImage(null)
	}

}