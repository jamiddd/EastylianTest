package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.jamid.eastyliantest.COUNTER
import com.jamid.eastyliantest.DELIVERY_PRICE
import com.jamid.eastyliantest.ONLINE
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    var orderId: String,
    var receiptId: String,
    var createdAt: Long,
    var deliveryAt: Long,
    @Embedded(prefix = "order_prices_")
    var prices: CartPrices,
    var status: List<OrderStatus>,
    var currency: String,
    var razorpayOrderId: String,
    @Ignore
    var items: List<CartItem>,
    var delivery: Boolean,
    var priority: OrderPriority,
    var timeSetByUser: Boolean,
    var senderId: String,
    var paymentId: String,
    @Embedded(prefix = "order_place_")
    var place: SimplePlace,
    var deliveryExecutive: String? = null,
    var paymentMethod: String = "$ONLINE|$COUNTER",
    @Exclude @set: Exclude @get: Exclude
    var isCurrentOrder: Boolean = false
): Parcelable {

    constructor(): this(randomId(), randomId(), System.currentTimeMillis(), System.currentTimeMillis() + minimumTime, CartPrices.newInstance(), listOf(OrderStatus.Created), nationalCurrency, "", emptyList(), true, OrderPriority.Low, false, "", "", SimplePlace())

    companion object {

        private const val minHours = 20
        private const val minimumTime = minHours * 60 * 60 * 1000
        private const val nationalCurrency = "INR"

        fun normalOrder(cartItems: List<CartItem>, sender: String): Order {
            return Order().apply {
                items = cartItems
                prices = CartPrices.newInstance(getAmount(cartItems), DELIVERY_PRICE)
                senderId = sender
                isCurrentOrder = true
            }
        }

        private fun getAmount(items: List<CartItem>): Long {
            var totalPrice: Long = 0
            for (item in items) {
                totalPrice += item.totalPrice
            }
            return totalPrice
        }

    }

}