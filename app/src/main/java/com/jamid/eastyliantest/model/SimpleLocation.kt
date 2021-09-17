package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "simple_location")
data class SimpleLocation(
    val latitude: Double,
    val longitude: Double,
    val place: String,
    @PrimaryKey(autoGenerate = false)
    val id: Int = 0
): Parcelable {
    constructor(): this(0.0, 0.0, "")
}
