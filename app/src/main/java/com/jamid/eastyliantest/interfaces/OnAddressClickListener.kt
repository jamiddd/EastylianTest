package com.jamid.eastyliantest.interfaces

import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.model.SimplePlace

interface OnAddressClickListener {
	fun onCurrentAddressSelected(viewHolder: RecyclerView.ViewHolder, place: SimplePlace)
	fun onAddressItemClick(viewHolder: RecyclerView.ViewHolder)
}