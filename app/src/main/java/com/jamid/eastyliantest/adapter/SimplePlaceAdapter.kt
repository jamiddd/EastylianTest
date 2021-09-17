package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.OnAddressClickListener
import com.jamid.eastyliantest.model.SimplePlace
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class SimplePlaceAdapter(private val onAddressClickListener: OnAddressClickListener): ListAdapter<SimplePlace, SimplePlaceAdapter.SimplePlaceViewHolder>(
	simplePlaceComparator){

	companion object {
		val simplePlaceComparator = object: DiffUtil.ItemCallback<SimplePlace>() {
			override fun areItemsTheSame(oldItem: SimplePlace, newItem: SimplePlace): Boolean {
				return oldItem.id == newItem.id
			}

			override fun areContentsTheSame(oldItem: SimplePlace, newItem: SimplePlace): Boolean {
				return oldItem == newItem
			}
		}
	}

	inner class SimplePlaceViewHolder(val view: View): RecyclerView.ViewHolder(view) {

		private var selectBtn: Button? = null

		fun bind(place: SimplePlace) {
			val locationItem = view.findViewById<TextView>(R.id.location_item)
			val locationAddress = view.findViewById<TextView>(R.id.location_address)
			selectBtn = view.findViewById(R.id.setCurrentAddressBtn)

			locationItem.text = place.name
			locationAddress.text = place.address

			view.setOnClickListener {
				onAddressClickListener.onAddressItemClick(this)
			}

			selectBtn?.setOnClickListener {
				onAddressClickListener.onCurrentAddressSelected(this, place)
			}
		}

		fun hideButton() {
			selectBtn?.hide()
		}

		fun showButton() {
			selectBtn?.show()
		}

	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimplePlaceViewHolder {
		return SimplePlaceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.address_item, parent, false))
	}

	override fun onBindViewHolder(holder: SimplePlaceViewHolder, position: Int) {
		holder.bind(getItem(position))
	}
}