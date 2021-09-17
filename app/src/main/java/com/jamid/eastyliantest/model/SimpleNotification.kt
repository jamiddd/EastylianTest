package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "notifications")
data class SimpleNotification(
	@PrimaryKey
	var notificationId: String,
	var content: String,
	var title: String,
	var image: String?,
	var createdAt: Long
): Parcelable {
	constructor(): this("", "", "", "", 0)
}