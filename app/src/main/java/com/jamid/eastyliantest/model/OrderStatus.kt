package com.jamid.eastyliantest.model

/*
    There are three possibilities
    1. There is only one order status and it's Due
    2. There is only one order status and it's not due
    3. The second is always Due
*/

enum class OrderStatus {
    Created, Paid, Preparing, Delivering, Delivered, Cancelled, Rejected, Due
}