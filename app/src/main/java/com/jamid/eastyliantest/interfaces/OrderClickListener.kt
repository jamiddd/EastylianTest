package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.adapter.OrderViewHolder
import com.jamid.eastyliantest.model.Order

interface OrderClickListener {
    fun onPrimaryActionClick(vh: OrderViewHolder, order: Order)
    fun onSecondaryActionClick(vh: OrderViewHolder, order: Order)
    fun onSelectDirection(order: Order)
}