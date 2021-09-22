package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.OrderImageClickListener
import com.jamid.eastyliantest.model.CartItem
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.utility.getFlavorName

class OrderItemAdapter(private val cakes: List<CartItem>): RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder>(){

    inner class OrderItemViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val orderImageClickListener = view.context as OrderImageClickListener

        fun bind(cartItem: CartItem) {
            val cake = cartItem.cake
            val image = view.findViewById<SimpleDraweeView>(R.id.orderItemImage)
            val name = view.findViewById<TextView>(R.id.orderItemName)
            val desc = view.findViewById<TextView>(R.id.orderItemDesc)
            val quantity = view.findViewById<TextView>(R.id.orderItemQuantity)

            image.setImageURI(cake.thumbnail)

            if (cartItem.cake.fancyName.isNotBlank()) {
                name.text = cartItem.cake.fancyName
            } else {
                val newName = if (cartItem.cake.flavors.last() != Flavor.VANILLA) {
                    cartItem.cake.baseName + " with " + view.context.getFlavorName(cartItem.cake.flavors.last()) + " flavor"
                } else {
                    cartItem.cake.baseName + " with Vanilla flavor"
                }
                name.text = newName
            }

            desc.text = cake.additionalDescription
            if (cake.additionalDescription != null && cake.additionalDescription!!.isNotBlank()) {
                desc.visibility = View.VISIBLE
            } else {
                desc.visibility = View.GONE
            }

            val quantityText = "Qty. ${cartItem.quantity}"
            quantity.text = quantityText

            image.setOnClickListener {
                orderImageClickListener.onImageClick(image, cartItem.cake.thumbnail!!)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        return OrderItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.order_item_cake, parent, false))
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        holder.bind(cakes[position])
    }

    override fun getItemCount(): Int {
        return cakes.size
    }

}