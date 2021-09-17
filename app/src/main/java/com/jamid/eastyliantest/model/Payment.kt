package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "payments")
@Parcelize
data class Payment(
    @PrimaryKey
    var paymentId: String,
    var orderId: String,
    var status: String,
    var razorpayOrderId: String,
    var paymentSignature: String?,
    var payment_method: String?,
    var amount: Long
): Parcelable {

    constructor(): this("", "", "", "", "", "", 0)

    override fun toString(): String {
        return "[$paymentId, $payment_method, $status]"
    }

}