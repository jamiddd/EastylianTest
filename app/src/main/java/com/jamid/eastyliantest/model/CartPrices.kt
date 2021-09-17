package com.jamid.eastyliantest.model

import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

// multiply by 100 to normalize for currency standard
@Parcelize
@Entity(tableName = "cart_prices")
data class CartPrices(
    @PrimaryKey
    @Exclude @set: Exclude @get: Exclude
    var id: String,
    val cgst: Long,
    val sgst: Long,
    val subTotal: Long,
    val total: Long,
    val deliveryPrice: Long
): Parcelable {

    constructor(): this(randomId(), 0, 0, 0, 0, 0)

    val cgstString: String
        get() {
            val price: Float = cgst.toFloat() / 100
            return prefix + price
        }

    val sgstString: String
        get() {
            val price: Float = sgst.toFloat() / 100
            return prefix + price
        }

    val subTotalString: String
        get() {
            val price: Float = subTotal.toFloat() / 100
            return prefix + price
        }

    val totalString: String
        get() {
            val price: Float = total.toFloat() / 100
            return prefix + price
        }

    val deliveryPriceString: String
        get() {
            val price: Float = deliveryPrice.toFloat() / 100
            return prefix + price
        }

    companion object {

        private const val TAG = "CartPricesTAG"
        private const val prefix = "₹ "

        fun newInstance(basePrice: Long = 0, deliveryPrice: Long = 0): CartPrices {
            val cgst = (basePrice * 0.025).toLong()
            val sgst = (basePrice * 0.025).toLong()
            Log.d(TAG, "CGST - $cgst, SGST - $sgst")
            val subTotal = basePrice + cgst + sgst
            val total = subTotal + deliveryPrice
            return CartPrices(randomId(), cgst, sgst, subTotal, total, deliveryPrice)
        }


    }
}
