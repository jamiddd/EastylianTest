package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.CakeMiniListener
import com.jamid.eastyliantest.model.Cake

class CakeMiniAdapter: ListAdapter<Cake, CakeMiniAdapter.CakeMiniViewHolder>(cakeComparator) {

	companion object {
		val cakeComparator = object: DiffUtil.ItemCallback<Cake>() {
			override fun areItemsTheSame(oldItem: Cake, newItem: Cake): Boolean {
				return oldItem.id == newItem.id
			}

			override fun areContentsTheSame(oldItem: Cake, newItem: Cake): Boolean {
				return oldItem == newItem
			}

		}
	}

	inner class CakeMiniViewHolder(val view: View): RecyclerView.ViewHolder(view) {

		private val cakeMiniListener: CakeMiniListener = view.context as CakeMiniListener

		fun bind(cake: Cake) {
			val image: SimpleDraweeView = view.findViewById(R.id.cakePreview)
			val cakeName: TextView = view.findViewById(R.id.cakeNameText)
			val cakePrice: TextView = view.findViewById(R.id.cakePriceText)
			val inStockSwitch: SwitchCompat = view.findViewById(R.id.inStockSwitch)
			val cakeDesc: TextView = view.findViewById(R.id.cakeDescText)

			val pricePrefix = "₹ "
			image.setImageURI(cake.thumbnail)
			cakeName.text = cake.fancyName
			val price = pricePrefix + (cake.price/100).toString()
			cakePrice.text = price
			cakeDesc.text = cake.description

			inStockSwitch.isChecked = cake.available

			inStockSwitch.setOnCheckedChangeListener { _, isChecked ->
				cake.available = isChecked
				cakeMiniListener.onCakeAvailabilityChange(cake)
			}

			view.setOnLongClickListener {
				cakeMiniListener.onContextActionMode(this, cake)
				true
			}

			view.setOnClickListener {
				cakeMiniListener.onClick(this, cake)
			}
		}

		fun activeBackground() {
			view.isSelected = true
//			view.setBackgroundColor(ContextCompat.getColor(view.context, R.color.blueLightBackground))
		}

		fun inactiveBackground() {
			view.isSelected = false
//			view.setBackgroundColor(Color.parseColor("#F1F1F1"))
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CakeMiniViewHolder {
		return CakeMiniViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.mini_cake_item, parent, false))
	}

	override fun onBindViewHolder(holder: CakeMiniViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
}