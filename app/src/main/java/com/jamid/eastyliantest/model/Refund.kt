package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "refunds")
data class Refund(
	@PrimaryKey
	var refundId: String,
	var orderId: String,
	var receiverId: String,
	var paymentId: String,
	var amount: Long,
	var status: String,
	var createdAt: Long = System.currentTimeMillis()
): Parcelable {
	constructor(): this("", "", "", "", 0, "", 0)
}