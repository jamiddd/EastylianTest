package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
@Entity(tableName = "restaurant")
data class Restaurant(
	@PrimaryKey
	var name: String,
	var totalOrderCount: Long,
	var totalRefundAmount: Long,
	var totalRefundCount: Long,
	var totalSalesAmount: Long,
	var locationAddress: String,
	var adminEmailAddresses: List<String>,
	var adminPhoneNumbers: List<String>,
	@Ignore
	var location: @RawValue GeoPoint,
): Parcelable {
	constructor() : this("", 0, 0, 0, 0, "", emptyList(), emptyList(), GeoPoint(0.0, 0.0))
}