package com.jamid.eastyliantest.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.jamid.eastyliantest.utility.randomId
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "cakes")
data class Cake(
    @PrimaryKey
    var id: String,
    var baseName: String,
    var fancyName: String,
    var flavors: List<Flavor>,
    var occasion: String,
    var weightKg: Float,
    var price: Long,
    var isEdiblePrintAttached: Boolean,
    var description: String? = null,
    var ediblePrintImage: String? = null,
    var additionalDescription: String? = null,
    var thumbnail: String? = null,
    var available: Boolean = true,
    @Exclude @set: Exclude @get: Exclude
    var isFavorite: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isCustomizable: Boolean = true,
    @Exclude @set: Exclude @get: Exclude
    var isAddedToCart: Boolean = false
): Parcelable {
    constructor(): this(randomId(), "Sponge Cream Cake", "", listOf(Flavor.VANILLA), "Birthday", 0.5f, 55000, false, null, null, null)

    companion object {
        fun newInstance(cakeName: String, flavors: List<Flavor>, cakeDesc: String, weightKg: Float, price: Long, thumbnail: String?): Cake {
            return Cake(randomId(), "Sponge Cream Cake", cakeName, flavors, "none", weightKg, price, false, cakeDesc, thumbnail=thumbnail)
        }

        fun newInstance(baseName: String): Cake {
            val cake = Cake()
            cake.baseName = baseName
            return cake
        }

        fun newFlavorInstance(flavors: List<Flavor>): Cake {
            val cake = Cake()
            cake.flavors = flavors
            return cake
        }

    }

}