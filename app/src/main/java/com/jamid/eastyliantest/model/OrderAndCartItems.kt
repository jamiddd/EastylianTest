package com.jamid.eastyliantest.model

import androidx.room.Embedded
import androidx.room.Relation

data class OrderAndCartItems(
    @Embedded
    val order: Order,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val cartItems: List<CartItem>
)