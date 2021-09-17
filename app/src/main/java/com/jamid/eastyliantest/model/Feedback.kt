package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "feedback")
data class Feedback(
	// id is the order id of the order it belongs to
	@PrimaryKey
	var id: String,
	var rating: Float,
	var content: String,
	var sender: String,
	var createdAt: Long
): Parcelable {
	constructor(): this("", 0f, "", "", 0)
}
