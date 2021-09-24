package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.SimpleNotification

interface NotificationClickListener {

    fun onNotificationResend(notification: SimpleNotification)
    fun onNotificationClick(notification: SimpleNotification)

}