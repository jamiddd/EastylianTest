package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.SimpleNotification

class NotificationPagingAdapter: PagingDataAdapter<SimpleNotification, NotificationViewHolder>(
	notificationComparator) {
	companion object {
		val notificationComparator = object: DiffUtil.ItemCallback<SimpleNotification>() {
			override fun areItemsTheSame(
				oldItem: SimpleNotification,
				newItem: SimpleNotification
			): Boolean {
				return oldItem.notificationId == newItem.notificationId
			}

			override fun areContentsTheSame(
				oldItem: SimpleNotification,
				newItem: SimpleNotification
			): Boolean {
				return oldItem == newItem
			}

		}
	}

	override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
		return NotificationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false))
	}
}