package com.jamid.eastyliantest.adapter

import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.NotificationClickListener
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.utility.getTextForTime
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class NotificationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

	private val title: TextView = view.findViewById(R.id.notificationTitle)
	private val content: TextView = view.findViewById(R.id.notificationContent)
	private val image: SimpleDraweeView = view.findViewById(R.id.notificationImage)
	private val time: TextView = view.findViewById(R.id.notificationTime)
	private val optionBtn: Button = view.findViewById(R.id.notificationOptionBtn)

	private val notificationClickListener = view.context as NotificationClickListener

	fun bind(notification: SimpleNotification?) {
		if (notification != null) {

			title.text = notification.title
			content.text = notification.content

			if (notification.image != null) {
				image.show()
				image.setImageURI(notification.image)
			} else {
				image.hide()
			}

			time.text = getTextForTime(notification.createdAt)

			optionBtn.setOnClickListener {
				showMenu(it, notification)
			}

			view.setOnClickListener {
				notificationClickListener.onNotificationClick(notification)
			}

			view.setOnLongClickListener {
				showMenu(optionBtn, notification)
				true
			}

		}
	}

	private fun showMenu(anchor: View, notification: SimpleNotification) {
		val popupMenu = PopupMenu(view.context, anchor)

		popupMenu.inflate(R.menu.notification_menu)

		popupMenu.setOnMenuItemClickListener { it1 ->

			when (it1.itemId) {
				R.id.notification_resend -> {
					notificationClickListener.onNotificationResend(notification)
				}
				R.id.notification_edit -> {
					notificationClickListener.onNotificationClick(notification)
				}
			}

			return@setOnMenuItemClickListener true
		}

		popupMenu.show()
	}

}