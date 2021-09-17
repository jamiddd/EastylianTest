package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.Refund

class RefundPagingAdapter: PagingDataAdapter<Refund, RefundViewHolder>(refundComparator) {

	companion object {
		val refundComparator = object: DiffUtil.ItemCallback<Refund>() {
			override fun areItemsTheSame(oldItem: Refund, newItem: Refund): Boolean {
				return oldItem.refundId == newItem.refundId
			}

			override fun areContentsTheSame(oldItem: Refund, newItem: Refund): Boolean {
				return oldItem == newItem
			}
		}
	}

	override fun onBindViewHolder(holder: RefundViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int
	): RefundViewHolder {
		return RefundViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.refund_item, parent, false))
	}
}