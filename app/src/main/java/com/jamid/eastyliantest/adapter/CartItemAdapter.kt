package com.jamid.eastyliantest.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.AmountCustomizeLayoutBinding
import com.jamid.eastyliantest.interfaces.CartItemClickListener
import com.jamid.eastyliantest.model.CartItem
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.model.Flavor.*

class CartItemAdapter: ListAdapter<CartItem, CartItemAdapter.CartItemViewHolder>(object : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem.cartItemId == newItem.cartItemId
    }

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
        return oldItem == newItem
    }
}) {

    inner class CartItemViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val cartItemClickListener = view.context as CartItemClickListener

        fun bind(cartItem: CartItem) {
            val cakeImage: SimpleDraweeView = view.findViewById(R.id.cakeImage)
            val cakeName: TextView = view.findViewById(R.id.cakeName)
            val cakeDesc: TextView = view.findViewById(R.id.cakeDescription)
            val customizerView = view.findViewById<View>(R.id.amountCustomizerContainer)

            val amountCustomizerBinding = AmountCustomizeLayoutBinding.bind(customizerView)

            val customizeBtn: Button = view.findViewById(R.id.customizeBtn)

            /*if (cartItem.cake.thumbnail != null) {

            } else {
                if (cartItem.cake.flavors.first() != VANILLA) {
                    cakeImage.setActualImageResource(view.context.getImageResourceBasedOnFlavor(cartItem.cake.flavors.first()))
                } else {
                    cakeImage.setActualImageResource(view.context.getImageResourceBasedOnBaseName(cartItem.cake.baseName))
                }
            }*/
            cakeImage.setImageURI(cartItem.cake.thumbnail)

            if (cartItem.cake.fancyName.isNotBlank()) {
                cakeName.text = cartItem.cake.fancyName
            } else {
                val newName = if (cartItem.cake.flavors.first() != VANILLA) {
                    cartItem.cake.baseName + " with " + view.context.getFlavorName(cartItem.cake.flavors.first()) + " flavor"
                } else {
                    cartItem.cake.baseName + " with Vanilla flavor"
                }
                cakeName.text = newName
            }


            cakeDesc.text = getFullDescriptionText(cartItem)

            updatePriceAndQuantity(cartItem, amountCustomizerBinding)

            amountCustomizerBinding.amountText.setTextColor(ContextCompat.getColor(view.context, R.color.primaryColor))

            amountCustomizerBinding.increaseAmount.setOnClickListener {
                cartItemClickListener.onAddItemClick(cartItem)
                updatePriceAndQuantity(cartItem, amountCustomizerBinding)
            }

            amountCustomizerBinding.decreaseAmount.setOnClickListener {
                cartItemClickListener.onRemoveItemClick(cartItem)
                updatePriceAndQuantity(cartItem, amountCustomizerBinding)
            }

            customizeBtn.setOnClickListener {
                cartItemClickListener.onCustomizeClick(cartItem)
            }
        }

        /*private fun getFlavorNamesFromList(list: List<String>): String {
            var fString = ""
            for (i in list.indices) {
                fString += if (i != list.size - 1) {
                    list[i] + ", "
                } else {
                    list[i]
                }
            }
            return fString
        }*/

        /*private fun Context.getFlavorNames(flavors: List<Flavor>): List<String> {
            val flavorsString = mutableListOf<String>()
            for (flavor in flavors) {
                flavorsString.add(getFlavorName(flavor))
            }
            return flavorsString
        }*/

        private fun Context.getFlavorName(flavor: Flavor): String {
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
        }

        private fun updatePriceAndQuantity(cartItem: CartItem, customizeLayoutBinding: AmountCustomizeLayoutBinding) {
            val cakePrice: TextView = view.findViewById(R.id.cakePrice)
            cartItem.totalPrice = cartItem.cake.price * cartItem.quantity
            val priceString = "₹ " + (cartItem.totalPrice/100).toString()
            cakePrice.text = priceString
            customizeLayoutBinding.amountText.text = cartItem.quantity.toString()
        }

        private fun getFullDescriptionText(cartItem: CartItem): String {
            return if (!cartItem.cake.additionalDescription.isNullOrBlank()) {
                "Flavors - " + getFlavorsString(cartItem.cake.flavors) +
                        "\nWeight - " + cartItem.cake.weightKg.toString() + " Kg" +
                        "\nOccasion - " + cartItem.cake.occasion +
                        "\nNote - " + cartItem.cake.additionalDescription
            } else {
                "Flavors - " + getFlavorsString(cartItem.cake.flavors) +
                        "\nWeight - " + cartItem.cake.weightKg.toString() + " Kg" +
                        "\nOccasion - " + cartItem.cake.occasion
            }
        }


        private fun getFlavorsString(flavors: List<Flavor>): String {
            var fString = ""
            for (i in flavors.indices) {
                fString += if (i != flavors.size - 1) {
                    view.context.getFlavorName(flavors[i]) + ", "
                } else {
                    view.context.getFlavorName(flavors[i])
                }
            }
            return fString
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        return CartItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false))
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}