package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "users")
@IgnoreExtraProperties
data class User(
    @PrimaryKey
    var userId: String,
    var name: String,
    var phoneNo: String,
    var upiPhoneNo: String,
    var balance: Long,
    var email: String,
    var photoUrl: String?,
    var registrationToken: String? = null,
    @Exclude @get: Exclude @set: Exclude
    var isAdmin: Boolean = false
): Parcelable {

    constructor(): this("", "", "", "", 0, "", null, null, false)

    override fun toString(): String {
        return "[$userId, $name, $phoneNo, $balance, $email, $photoUrl]"
    }
}