package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.OrderAndCartItems

class OrderPagingAdapter: PagingDataAdapter<OrderAndCartItems, OrderViewHolder>(OrderAndCartItemsComparator()){

	val randomIcons = mutableListOf<String>()
	var isAdmin = false
	var isDeliveryExecutive = false

	override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
		val orderAndCartItems = getItem(position)
		val order = orderAndCartItems?.order
		order?.items = orderAndCartItems?.cartItems ?: emptyList()
		holder.bind(order)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
		val orderVH = OrderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.order_item, parent, false))
		orderVH.isAdmin = isAdmin
		orderVH.isDeliveryExecutive = isDeliveryExecutive
		orderVH.randomIcons.addAll(randomIcons)
		return orderVH
	}

}

class OrderAndCartItemsComparator: DiffUtil.ItemCallback<OrderAndCartItems>() {
	override fun areItemsTheSame(oldItem: OrderAndCartItems, newItem: OrderAndCartItems): Boolean {
		return oldItem.order.orderId == newItem.order.orderId
	}

	override fun areContentsTheSame(oldItem: OrderAndCartItems, newItem: OrderAndCartItems): Boolean {
		return oldItem == newItem
	}
}
