package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "cakeMenuItems")
data class CakeMenuItem(
	@PrimaryKey
	val id: String,
	val title: String,
	val image: String,
	val category: String,
	val price: Long
): Parcelable {
	constructor(): this("", "", "", "", 0)
}