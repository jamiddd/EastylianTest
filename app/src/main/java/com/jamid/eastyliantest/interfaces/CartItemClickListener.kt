package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.CartItem

interface CartItemClickListener {
    fun onAddItemClick(cartItem: CartItem)
    fun onRemoveItemClick(cartItem: CartItem)
    fun onCustomizeClick(cartItem: CartItem)
}