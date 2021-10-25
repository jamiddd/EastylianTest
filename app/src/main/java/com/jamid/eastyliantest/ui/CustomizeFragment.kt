package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.ChoiceExtraBinding
import com.jamid.eastyliantest.databinding.FragmentCustomizeNewBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.CartItem
import com.jamid.eastyliantest.model.Flavor.*
import com.jamid.eastyliantest.utility.*
import java.util.*

class CustomizeFragment: Fragment() {

	private lateinit var binding: FragmentCustomizeNewBinding
	private val viewModel: MainViewModel by activityViewModels()
	private var editMode = false

	private var flavorPrice: Long = 0
	private var weightPrice: Long = 0
	private var basePrice: Long = 55000

	private lateinit var choicesAdapter: ChoicesAdapter
	private var ediblePrintImage: String? = null
	private lateinit var finalCake: Cake

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentCustomizeNewBinding.inflate(inflater)
		return binding.root
	}

	private fun initiateRecycler() {
		choicesAdapter = ChoicesAdapter()
		choicesAdapter.isEditMode = editMode

		val flavorList = viewModel.flavorMenuItems.value?.map {
			it.title
		}

		val choice = Choice(0, "Choose Base", "The base of the cake", listOf("Sponge Cream Cake", "Fondant Cake", "Edible Print"))
		val choice1 = Choice(1, "Choose Flavor", "The flavor of the cake", flavorList ?: emptyList())
		val choice2 = Choice(2, "Size/Weight", "Additional Rs 400 for every half Kg.")
		val choice3 = Choice(3, "Occasion", "Getting to know the occasion helps us to better design the cake.", listOf("Birthday", "Wedding", "Anniversary", "Valentine\'s Day", "Mother\'s Day"))
		val choice4 = Choice(4, "Additional", "Additional info such as name on the cake or any specifics.")

		binding.choicesRecycler.apply {
			adapter = choicesAdapter
			layoutManager = LinearLayoutManager(requireContext())
		}

		if (finalCake.isCustomizable) {
			choicesAdapter.submitList(listOf(choice, choice1, choice2, choice3, choice4))
		} else {
			choicesAdapter.submitList(listOf(choice2, choice3, choice4))
		}

	}

	private fun getTitle(cake: Cake): String {
		return if (cake.fancyName.isNotBlank()) {
			cake.fancyName
		} else {
			if (cake.flavors.first() != VANILLA) {
				cake.baseName + " with " + getFlavorName(cake.flavors.first()) + " flavor"
			} else {
				cake.baseName + " with Vanilla flavor"
			}
		}

	}

	private fun setImage(image: String?) {
		requireActivity().findViewById<SimpleDraweeView>(R.id.main_image)?.setImageURI(image)
	}

	// main function to update the UI on start
	private fun presetCake(cake: Cake? = null, cartItem: CartItem? = null) {
		if (cake != null) {
//			setToolbar(cake)
			setImage(cake.thumbnail)

			// set short description
			if (cake.description != null) {
				binding.cakeDescText.text = cake.description
				binding.cakeDescText.show()
			} else {
				binding.cakeDescText.hide()
			}

			setPrimaryButton(cake)

			finalCake = cake.copy()

			initiateRecycler()

		} else {
			if (cartItem != null) {
				presetCake(cartItem.cake)
			}
		}
	}

	// set the primary button based on current mode of this fragment
	private fun setPrimaryButton(cake: Cake) {

		requireActivity().findViewById<Button>(R.id.addToCartBtn)?.let {
			it.apply {
				if (editMode) {
					it.text = getString(R.string.update_item_text)
				} else {
					it.text = getString(R.string.add_item_text)
				}

				if (!cake.isCustomizable && cake.isAddedToCart) {
					it.text = "Add more"
				}

				setOnClickListener {
					if (finalCake.isEdiblePrintAttached) {
						finalCake.thumbnail = finalCake.ediblePrintImage
					}

					viewModel.shouldUpdateCart = true
					if (!finalCake.isCustomizable && finalCake.isAddedToCart) {
						viewModel.updateCurrentCartOrder(finalCake, change = CartItemChange.Increment)
					} else {
						viewModel.updateCurrentCartOrder(finalCake, change = CartItemChange.Update)
					}

					findNavController().navigateUp()
				}
			}
		}
	}

	private fun getCakeAdditionalPrice(): Long {
		return flavorPrice + weightPrice
	}

	private fun onBaseNameChange(newBaseName: String) {
		finalCake.baseName = newBaseName

		if (newBaseName == "Edible Print") {
			val currentImage = viewModel.currentImage.value
			if (currentImage == null) {
				(activity as MainActivity2?)?.selectImage()
			} else {
				TODO("When the image is already selected")
			}
		} else {
			viewModel.setCurrentImage(null)
		}


		val existingBaseMenuList = viewModel.baseMenuItems.value
		if (existingBaseMenuList != null) {
			val newBaseItem = existingBaseMenuList.find {
				it.title == newBaseName
			}

			if (!finalCake.isEdiblePrintAttached) {
				setImage(newBaseItem?.image)
			}

		}
	}

	private fun findFlavorPrice(flavor: String): Long {
		val flavorItems = viewModel.flavorMenuItems.value
		return if (!flavorItems.isNullOrEmpty()) {
			var price: Long = 0
			for (item in flavorItems) {
				if (item.title == flavor) {
					price = item.price
				}
			}
			price
		} else {
			0
		}
	}

	private fun onFlavorChange(newFlavorName: String) {
		finalCake.flavors = listOf(getFlavorFromName(newFlavorName))
		flavorPrice = findFlavorPrice(newFlavorName)
		setPrice(finalCake)


		val existingFlavorMenuList = viewModel.flavorMenuItems.value
		if (existingFlavorMenuList != null) {
			val newFlavorItem = existingFlavorMenuList.find {
				it.title == newFlavorName
			}

			if (!finalCake.isEdiblePrintAttached) {
				finalCake.thumbnail = newFlavorItem?.image
				setImage(newFlavorItem?.image)
			}

		}

	}

	private fun onSizeChange(newSize: Float) {
		finalCake.weightKg = newSize
		weightPrice = (((newSize - 0.5f)/0.5) * 40000).toLong()
		setPrice(finalCake)
	}

	private fun onOccasionChange(newOccasion: String) {
		finalCake.occasion = newOccasion
	}

	private fun onAdditionalInfoChanged(newAdditionalInfo: String) {
		finalCake.additionalDescription = newAdditionalInfo
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
			binding.customizeScroll.setPadding(0, 0, 0, convertDpToPx(120) + bottom)
		}

		val cake2 = arguments?.getParcelable<Cake>(ARG_CAKE)
		val cartItem2 = arguments?.getParcelable<CartItem>(ARG_CART_ITEM)
		editMode = arguments?.getBoolean(ARG_IS_EDIT_MODE) ?: false

		presetCake(cake2, cartItem2)

		viewModel.currentImage.observe(viewLifecycleOwner) {
//			binding.imageUploadProgressBar.show()
			if (it != null) {
				viewModel.uploadImage(it) { downloadUrl ->
//					binding.imageUploadProgressBar.hide()
					if (downloadUrl != null) {
						finalCake.thumbnail = null
						ediblePrintImage = downloadUrl.toString()
						finalCake.ediblePrintImage = ediblePrintImage
						finalCake.isEdiblePrintAttached = true
						setImage(ediblePrintImage)
					} else {
						ediblePrintImage = null
					}
				}
			} else {
				val vg = binding.choicesRecycler.getChildAt(0) as ViewGroup?
				if (vg != null) {
					for (ch in vg.children) {
						if (ch is ViewGroup && ch is ChipGroup) {
							if ((ch.getChildAt(2) as Chip).isChecked) {
								ch.check(ch.getChildAt(0).id)
							}
						}
					}
				}


				finalCake.isEdiblePrintAttached = false
				finalCake.ediblePrintImage = null
//				binding.imageUploadProgressBar.hide()
				ediblePrintImage = null
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
		requireActivity().findViewById<TextView>(R.id.priceText)?.let {
			val additionalPrice = getCakeAdditionalPrice()
			finalCake.price = basePrice + additionalPrice
			val additionalPriceText = getString(R.string.currency_prefix) + (additionalPrice/100).toString()
			val basePriceText = getString(R.string.currency_prefix) + "${basePrice/100}"
			val priceText = "$basePriceText + $additionalPriceText (Additional)"
			val sp = SpannableString(priceText)
			sp.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.primaryColor)), 0, basePriceText.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
			it.text = sp
		}
	}

	companion object {
		private const val TAG = "CustomizeFragment"
		const val ARG_CART_ITEM = "ARG_CART_ITEM"
		const val ARG_CAKE = "ARG_CAKE"
		const val ARG_IS_EDIT_MODE = "ARG_IS_EDIT_MODE"
	}

	data class Choice(val id: Int = 0, val header: String = "", val description: String = "", val choices: List<String> = emptyList())

	val choicesComparator = object: DiffUtil.ItemCallback<Choice>() {
		override fun areItemsTheSame(oldItem: Choice, newItem: Choice): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: Choice, newItem: Choice): Boolean {
			return oldItem == newItem
		}
	}

	private inner class ChoicesAdapter: ListAdapter<Choice, ChoicesAdapter.ChoicesViewHolder>(choicesComparator) {

		var isEditMode = false

		inner class ChoicesViewHolder(view: View): RecyclerView.ViewHolder(view) {

			private val heading: TextView = view.findViewById(R.id.choiceHeading)
			private val description: TextView = view.findViewById(R.id.choiceDescription)
			private val choicesContainer: ChipGroup = view.findViewById(R.id.choiceGroup)
			private val stub: ViewStub = view.findViewById(R.id.choiceExtraStub)

			fun selectBase(baseName: String) {
				for (child in choicesContainer.children) {
					if ((child as Chip).text == baseName) {
						choicesContainer.check(child.id)
					}
				}
			}

			fun selectFlavor(flavor: String) {
				for (child in choicesContainer.children) {
					val f = (child as Chip).text
					if (f == flavor) {
						choicesContainer.check(child.id)
						// setting initial price
						if (f != "Vanilla") {
							onFlavorChange(f.toString())
						}
					}
				}
			}

			fun setWeight(view: TextView, weight: Float) {
				view.text = weight.toString()
				onSizeChange(weight)
			}

			fun bind(choice: Choice) {

				heading.text = choice.header
				description.text = choice.description

				for (ch in choice.choices) {
					addChoice(ch)
				}

				when (choice.id) {
					0 -> {
						// base
						choicesContainer.setOnCheckedChangeListener { _, _ ->
							val chip = choicesContainer.findViewById<Chip>(choicesContainer.checkedChipId)
							val baseName = chip.text.toString()
							onBaseNameChange(baseName)
						}

						selectBase(finalCake.baseName)

					}
					1 -> {
						// flavor
						choicesContainer.setOnCheckedChangeListener { _, _ ->
							val chip = choicesContainer.findViewById<Chip>(choicesContainer.checkedChipId)
							onFlavorChange(chip.text.toString())
						}

						selectFlavor(getFlavorName(finalCake.flavors.first()))

					}
					2 -> {
						// weight
						choicesContainer.hide()

						val extraView = stub.inflate()
						val choiceExtraBinding = ChoiceExtraBinding.bind(extraView)

						choiceExtraBinding.extraTextLayout.hide()

						choiceExtraBinding.extraAmountCustomize.apply {

							increaseAmount.setOnClickListener {
								var weight: Float
								val weightText = amountText.text.toString()
								if (weightText.isBlank()) {
									toast("Weight must be provided.")
									return@setOnClickListener
								} else {
									weight = weightText.toFloat()
									// because maximum is 10
									if (weight == 10f) {
										// do nothing
										return@setOnClickListener
									} else {
										weight += 0.5f
										amountText.text = weight.toString()
									}
								}

								onSizeChange(weight)
							}

							decreaseAmount.setOnClickListener {
								var weight: Float
								val wt = amountText.text.toString()
								if (wt.isBlank()) {
									toast("Weight must be provided")
									return@setOnClickListener
								} else {
									weight = wt.toFloat()
									// because maximum is 10
									if (weight == 0.5f) {
										// do nothing
										return@setOnClickListener
									} else {
										weight -= 0.5f
										amountText.text = weight.toString()
										weightPrice = (((weight - 0.5f)/0.5) * 40000).toLong()
									}
								}

								onSizeChange(weight)
							}

							if (isEditMode) {
								setWeight(amountText, finalCake.weightKg)
							} else {
								amountText.text = 0.5f.toString()
							}

						}


					}
					3 -> {
						// occasion
						choicesContainer.setOnCheckedChangeListener { _, _ ->
							val chip = choicesContainer.findViewById<Chip>(choicesContainer.checkedChipId)
							onOccasionChange(chip.text.toString())
						}

						choicesContainer.check(choicesContainer.getChildAt(0).id)

					}
					4 -> {
						// Additional
						choicesContainer.hide()

						val extraView = stub.inflate()
						val choiceExtraBinding = ChoiceExtraBinding.bind(extraView)

						choiceExtraBinding.extraAmountCustomize.root.hide()
						choiceExtraBinding.extraTextLayout.hint = "Description (Custom)"

						choiceExtraBinding.extraTextLayout.editText?.doAfterTextChanged {
							onAdditionalInfoChanged(choiceExtraBinding.extraTextLayout.editText?.text.toString())
						}
					}
				}
			}

			private fun addChoice(choice: String) {
				val chip = layoutInflater.inflate(R.layout.custom_chip, choicesContainer, false) as Chip
				chip.text = choice
				choicesContainer.addView(chip)
			}

		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoicesViewHolder {
			return ChoicesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.choice_layout, parent, false))
		}

		override fun onBindViewHolder(holder: ChoicesViewHolder, position: Int) {
			holder.bind(getItem(position))
		}
	}


	override fun onDestroyView() {
		super.onDestroyView()
		viewModel.setCurrentImage(null)
	}
}