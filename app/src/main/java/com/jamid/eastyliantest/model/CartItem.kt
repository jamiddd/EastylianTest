package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey
    var cartItemId: String,
    var quantity: Long,
    @Embedded(prefix = "cart_cake_")
    var cake: Cake,
    var totalPrice: Long,
    var orderId: String
): Parcelable {

    constructor(): this(randomId(), 0, Cake(), 0, randomId())

    companion object {
        fun newInstance(cake: Cake, orderId: String): CartItem {
            return CartItem(randomId(), 1, cake, cake.price, orderId)
        }
    }

}