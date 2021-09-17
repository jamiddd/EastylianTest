package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentCustomizeBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.CartItem
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.model.Flavor.*
import com.jamid.eastyliantest.utility.*
import java.util.*

class CustomizeFragment: Fragment(R.layout.fragment_customize) {

	private lateinit var binding: FragmentCustomizeBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var editMode = false
//	private val flavors: Queue<Flavor> = LinkedList(listOf(VANILLA))

	private var flavorPrice: Long = 0
	private var weightPrice: Long = 0
	private var basePrice: Long = 55000

	// price list for all items
	private val priceMap = mapOf<Any, Long>(
		BLACK_FOREST to 5000,
		VANILLA to 0,
		WHITE_FOREST to 5000,
		CHOCOLATE_FANTASY to 15000,
		RED_VELVET to 15000,
		HAZELNUT to 5000,
		MANGO to 5000,
		STRAWBERRY to 5000,
		KIWI to 5000,
		ORANGE to 5000,
		PINEAPPLE to 5000,
		BUTTERSCOTCH to 5000,
		0.5f to 0,
		1f to 40000
	)

	// main function to update the UI on start
	private fun presetCake(cake: Cake? = null, cartItem: CartItem? = null) {
		if (cake != null) {

			// set toolbar
			if (cake.fancyName.isBlank()) {
				binding.customizeFragmentToolbar.title = getFlavorName(cake.flavors.last()) + " Cake"
			} else {
				binding.customizeFragmentToolbar.title = cake.fancyName
			}

			// set image
			if (cake.thumbnail == null) {
				val flavor = cake.flavors.last()
				if (flavor == VANILLA) {
					binding.cakeImageVIew.setActualImageResource(getImageResourceBasedOnBaseName(cake.baseName))
				} else {
					binding.cakeImageVIew.setActualImageResource(getImageResourceBasedOnFlavor(flavor))
				}
			} else {
				binding.cakeImageVIew.setImageURI(cake.thumbnail)
			}

			// set short description
			if (cake.description != null) {
				binding.customFragmentContent.cakeDescText.text = cake.description
				binding.customFragmentContent.cakeDescText.show()
			} else {
				binding.customFragmentContent.cakeDescText.hide()
			}

			// set base
			binding.customFragmentContent.baseGroup.check(getBaseNameCheckId(cake.baseName))

			// set flavor
			binding.customFragmentContent.flavorGroup.check(getFlavourCheckId(cake.flavors.last()))

			// set weight
			binding.customFragmentContent.weightText.setText(cake.weightKg.toString())

			// set occasion
			binding.customFragmentContent.occasionGroup.check(getOccasionCheckId(cake.occasion))

			// set customer description
			binding.customFragmentContent.customDescriptionInputLayout.editText?.setText(cake.additionalDescription)

			updateUIBasedOnCustomize(cake)

			setPrimaryButton(cake)
		} else {
			if (cartItem != null) {
				presetCake(cartItem.cake)
			}
		}
	}

	// base on whether the cake can be customized or not
	private fun updateUIBasedOnCustomize(cake: Cake) {
		binding.customFragmentContent.apply {
			if (!cake.isCustomizable) {
				chooseBaseHeader.hide()
				baseDescriptionText.hide()
				flavorDescriptionText.hide()
				chooseBaseDivider.hide()
				chooseFlavorDivider.hide()
				baseGroup.hide()
				chooseFlavorHeader.hide()
				flavorGroup.hide()
				basePrice = cake.price
			}
		}

	}

	// set the primary button based on current mode of this fragment
	private fun setPrimaryButton(cake: Cake) {
		binding.addToCartBtn.apply {
			if (editMode) {
				binding.addToCartBtn.text = getString(R.string.update_item_text)
			} else {
				binding.addToCartBtn.text = getString(R.string.add_item_text)
			}

			setOnClickListener {
				val updatedCake = getFinalCake(cake)
				viewModel.updateCurrentCartOrder(updatedCake, change = CartItemChange.Update)
				findNavController().navigateUp()
			}
		}
	}

	// build the cake based on the previous cake
	private fun getFinalCake(oldCake: Cake): Cake {
		binding.customFragmentContent.apply {
			oldCake.baseName = getBaseName(baseGroup.checkedChipId)
			oldCake.flavors = listOf(getFlavor(flavorGroup.checkedChipId))
			oldCake.weightKg = weightText.text.toString().toFloat()
			oldCake.occasion = if (occasionGroup.checkedChipId == R.id.other) {
				customOccasionTextLayout.editText?.text.toString()
			} else {
				getOccasion(occasionGroup.checkedChipId)
			}
			oldCake.price += getCakeAdditionalPrice()
			oldCake.additionalDescription = customDescriptionInputLayout.editText?.text.toString()
		}
		return oldCake
	}

	private fun getCakeAdditionalPrice(): Long {
		return flavorPrice + weightPrice
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding = FragmentCustomizeBinding.bind(view)

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
			binding.customizeFragmentToolbar.updateLayout(marginTop = top)
			binding.customFragmentContent.customizeScroll.setPadding(0, 0, 0, convertDpToPx(120) + bottom)
		}

		binding.customizeFragmentToolbar.setNavigationOnClickListener {
			findNavController().navigateUp()
		}

		val cake2 = arguments?.getParcelable<Cake>(ARG_CAKE)
		val cartItem2 = arguments?.getParcelable<CartItem>(ARG_CART_ITEM)
		editMode = arguments?.getBoolean(ARG_IS_EDIT_MODE) ?: false
		presetCake(cake2, cartItem2)

		binding.customFragmentContent.apply {

			baseGroup.setOnCheckedChangeListener { _, checkedId ->
				val base = getBaseName(checkedId)
				binding.cakeImageVIew.setActualImageResource(getImageResourceBasedOnBaseName(base))
			}

			flavorGroup.setOnCheckedChangeListener { _, checkedId ->
				val flavor = getFlavor(checkedId)
				flavorPrice = priceMap[flavor]!!
				setPrice(cake2, cartItem2)

				binding.cakeImageVIew.setActualImageResource(getImageResourceBasedOnFlavor(flavor))

			}

			increaseWeightBtn.setOnClickListener {
				val weightText = binding.customFragmentContent.weightText.text
				if (weightText.isNullOrBlank()) {
					toast("Weight must be provided.")
					return@setOnClickListener
				} else {
					var weight = weightText.toString().toFloat()

					// because maximum is 10
					if (weight == 10f) {
						// do nothing
						return@setOnClickListener
					} else {
						weight += 0.5f
						binding.customFragmentContent.weightText.setText(weight.toString())
						weightPrice = (((weight - 0.5f)/0.5) * 40000).toLong()
						setPrice(cake2)
					}
				}
			}

			decreaseWeightBtn.setOnClickListener {
				val wt = weightText.text
				if (wt.isNullOrBlank()) {
					Toast.makeText(requireContext(), "Weight must be provided.", Toast.LENGTH_SHORT)
						.show()
					return@setOnClickListener
				} else {
					var weight = wt.toString().toFloat()
					// because maximum is 10
					if (weight == 0.5f) {
						// do nothing
						return@setOnClickListener
					} else {
						weight -= 0.5f
						weightText.setText(weight.toString())
						weightPrice = ((weight - 0.5f) * 20000).toLong()
						setPrice(cake2)
					}
				}
			}

			occasionGroup.setOnCheckedChangeListener { _, checkedId ->
				customOccasionTextLayout.isVisible = checkedId == R.id.other
				/*if (checkedId == R.id.other) {
					customOccasionTextLayout.show()
				} else {
					customOccasionTextLayout.hide()
				}*/
			}

		}


	}

	// set the prices
	private fun setPrice(cake: Cake? = null, cartItem: CartItem? = null) {
		if (cake != null) {
			if (!cake.isCustomizable) {
				flavorPrice = 0
			}
			updatePriceUI()
		} else {
			if (cartItem != null) {
				setPrice(cartItem.cake)
			}
		}
	}

	private fun updatePriceUI() {
		val additionalPrice = getCakeAdditionalPrice()
		val additionalPriceText = getString(R.string.currency_prefix) + (additionalPrice/100).toString()
		val priceText = getString(R.string.currency_prefix) + "${basePrice/100} + $additionalPriceText"
		binding.priceText.text = priceText
	}

	private fun getOccasionCheckId(occasion: String): Int {
		return when (occasion) {
			getString(R.string.birthday) -> R.id.birthday
			getString(R.string.wedding) -> R.id.wedding
			getString(R.string.wedding) -> R.id.anniversary
			getString(R.string.valentine) -> R.id.valentinesDay
			getString(R.string.mother) -> R.id.mothersDay
			getString(R.string.father) -> R.id.fathersDay
			else -> R.id.other
		}
	}

	private fun getFlavourCheckId(flavor: Flavor): Int {
		return when (flavor) {
			BLACK_FOREST -> R.id.blackForest
			WHITE_FOREST -> R.id.whiteForest
			VANILLA -> R.id.vanilla
			CHOCOLATE_FANTASY -> R.id.chocolateFantasy
			RED_VELVET -> R.id.redVelvet
			HAZELNUT -> R.id.hazelnut
			MANGO -> R.id.mango
			STRAWBERRY -> R.id.strawberry
			KIWI -> R.id.kiwi
			ORANGE -> R.id.orange
			PINEAPPLE -> R.id.pineapple
			BUTTERSCOTCH -> R.id.butterscotch
			NONE -> 0
		}
	}

	private fun getBaseNameCheckId(baseName: String): Int {
		return when (baseName) {
			getString(R.string.fondant) -> R.id.fondant
			getString(R.string.sponge) -> R.id.sponge
			else -> R.id.sponge
		}
	}

	private fun getFlavor(id: Int): Flavor {
		return when (id) {
			R.id.blackForest -> BLACK_FOREST
			R.id.whiteForest -> WHITE_FOREST
			R.id.vanilla -> VANILLA
			R.id.chocolateFantasy -> CHOCOLATE_FANTASY
			R.id.redVelvet -> RED_VELVET
			R.id.hazelnut -> HAZELNUT
			R.id.mango -> MANGO
			R.id.strawberry -> STRAWBERRY
			R.id.kiwi -> KIWI
			R.id.orange -> ORANGE
			R.id.pineapple -> PINEAPPLE
			R.id.butterscotch -> BUTTERSCOTCH
			else -> NONE
		}
	}

	private fun getBaseName(id: Int): String {
		return when (id) {
			R.id.sponge -> getString(R.string.sponge)
			R.id.fondant -> getString(R.string.fondant)
			else -> getString(R.string.sponge)
		}
	}

	private fun getOccasion(id: Int): String {
		return when (id) {
			R.id.birthday -> getString(R.string.birthday)
			R.id.wedding -> getString(R.string.wedding)
			R.id.anniversary -> getString(R.string.anniversary)
			R.id.valentinesDay -> getString(R.string.valentine)
			R.id.mothersDay -> getString(R.string.mother)
			R.id.fathersDay -> getString(R.string.father)
			R.id.other -> binding.customFragmentContent.customOccasionTextLayout.editText?.text.toString()
			else -> binding.customFragmentContent.customOccasionTextLayout.editText?.text.toString()
		}
	}

	companion object {
		/*private const val TAG = "CustomizeFragment"*/
		const val ARG_CART_ITEM = "ARG_CART_ITEM"
		const val ARG_CAKE = "ARG_CAKE"
		const val ARG_IS_EDIT_MODE = "ARG_IS_EDIT_MODE"
	}

}

/*binding.customFragmentContent.flavorGroup.children.forEach { child ->
			val chip = child as Chip
			chip.setOnClickListener {
				if (chip.isChecked) {
					if (flavors.size == 2) {
						val firstFlavor = flavors.first()
						val prevCheckedId = getFlavourCheckId(firstFlavor)
						val prevChip = binding.customFragmentContent.flavorGroup.findViewById<Chip>(prevCheckedId)
						prevChip.isChecked = false
						val p1 = priceMap[firstFlavor]!!
						flavorPrice -= p1
						flavors.remove()
						val newF = getFlavor(child.id)
						val p2 = priceMap[newF]!!
						flavorPrice += p2
						flavors.add(newF)
					} else {
						val flavor = getFlavor(child.id)
						if (!flavors.contains(flavor)) {
							val p = priceMap[flavor]!!
							flavorPrice += p
							flavors.add(flavor)
						} else {
							binding.customFragmentContent.flavorGroup.check(R.id.vanilla)
							flavors.add(VANILLA)
							val chip1 = binding.customFragmentContent.flavorGroup.findViewById<Chip>(child.id)
							chip1.isChecked = false
							flavors.remove()
							flavorPrice = 0
						}
					}
				} else {
					val flavor = getFlavor(child.id)
					val p = priceMap[flavor]!!
					if (flavors.contains(flavor)) {
						flavorPrice -= p
						flavors.remove(flavor)
					} else {
						flavorPrice += p
						flavors.add(flavor)
					}
				}
				setPrice()
			}
		}
*/

/*private fun getFlavorNames(flavors: List<Flavor>): List<String> {
		val flavorsString = mutableListOf<String>()
		for (flavor in flavors) {
			flavorsString.add(getFlavourName(flavor))
		}
		return flavorsString
	}*/

/*private fun getFlavourName(flavor: Flavor): String {
	return when (flavor) {
		BLACK_FOREST -> getString(R.string.black_forest)
		WHITE_FOREST -> getString(R.string.white_forest)
		VANILLA -> getString(R.string.vanilla)
		CHOCOLATE_FANTASY -> getString(R.string.chocolate_fantasy)
		RED_VELVET -> getString(R.string.red_velvet)
		HAZELNUT -> getString(R.string.hazelnut)
		MANGO -> getString(R.string.mango)
		STRAWBERRY -> getString(R.string.strawberry)
		KIWI -> getString(R.string.kiwi)
		ORANGE -> getString(R.string.orange)
		PINEAPPLE -> getString(R.string.pineapple)
		BUTTERSCOTCH -> getString(R.string.butterscotch)
		NONE -> ""
	}
}*/