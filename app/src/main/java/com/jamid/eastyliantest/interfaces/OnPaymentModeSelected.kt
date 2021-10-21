package com.jamid.eastyliantest.interfaces

interface OnPaymentModeSelected {
    fun onCashOnDeliverySelected()
    fun onUpiSelected(amount: String)
}