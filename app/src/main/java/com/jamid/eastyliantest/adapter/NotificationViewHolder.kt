package com.jamid.eastyliantest.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.utility.getTextForTime
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show

class NotificationViewHolder(val view: View): RecyclerView.ViewHolder(view) {

	fun bind(notification: SimpleNotification?) {
		if (notification != null) {
			val title: TextView = view.findViewById(R.id.notificationTitle)
			val content: TextView = view.findViewById(R.id.notificationContent)
			val image: SimpleDraweeView = view.findViewById(R.id.notificationImage)
			val time: TextView = view.findViewById(R.id.notificationTime)

			title.text = notification.title
			content.text = notification.content
			if (notification.image != null) {
				image.show()
				image.setImageURI(notification.image)
			} else {
				image.hide()
			}

			time.text = getTextForTime(notification.createdAt)

		}
	}

}