package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.Refund
import com.jamid.eastyliantest.model.User

interface RefundClickListener {
    fun onUpdateBtnClick(refund: Refund, user: User)
}