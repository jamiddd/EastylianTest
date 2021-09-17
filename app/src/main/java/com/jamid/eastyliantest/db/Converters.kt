package com.jamid.eastyliantest.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.model.Flavor
import com.jamid.eastyliantest.model.OrderPriority
import com.jamid.eastyliantest.model.OrderStatus
import java.lang.reflect.Type

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null){
            return null
        }
        val listType: Type = object : TypeToken<List<String>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromBooleanToInt(state: Boolean): Int {
        return if (state) {
            1
        } else {
            0
        }
    }

    @TypeConverter
    fun fromIntToBoolean(state: Int): Boolean {
        return state == 1
    }

   /* @TypeConverter
    fun fromStringX(value: String?): ArrayList<String>? {
        if (value == null){
            return null
        }
        val listType: Type = object : TypeToken<ArrayList<String>?>() {}.type
        return Gson().fromJson(value, listType)
    }*/

   /* @TypeConverter
    fun fromArrayList(list: ArrayList<String>?): String? {
        if (list == null) {
            return null
        }
        val gson = Gson()
        return gson.toJson(list)
    }*/

    /*@TypeConverter
    fun fromIntArray(list: ArrayList<Int>?): String? {
        if (list == null) {
            return null
        }
        val a = arrayListOf<String>()
        for (l in list) {
            a.add(l.toString())
        }
        return fromArrayList(a)
    }*/

    /*@TypeConverter
    fun fromStringToNumArray(s: String?): ArrayList<Int>? {
        val list1 = fromStringX(s) ?: return null
        val a = arrayListOf<Int>()

        for (l in list1) {
            a.add(l.toInt())
        }

        return a
    }*/

    /*@TypeConverter
    fun fromFlavorToString(flavor: Flavor): String {
        return flavor.toString()
    }*/

    @TypeConverter
    fun fromStringToFlavor(flavor: String): Flavor {
        return when (flavor) {
            "BLACK_FOREST" -> Flavor.BLACK_FOREST
            "WHITE_FOREST" -> Flavor.WHITE_FOREST
            "VANILLA" -> Flavor.VANILLA
            "CHOCOLATE_FANTASY" -> Flavor.CHOCOLATE_FANTASY
            "RED_VELVET" -> Flavor.RED_VELVET
            "HAZELNUT" -> Flavor.HAZELNUT
            "MANGO" -> Flavor.MANGO
            "STRAWBERRY" -> Flavor.STRAWBERRY
            "KIWI" -> Flavor.KIWI
            "ORANGE" -> Flavor.ORANGE
            "PINEAPPLE" -> Flavor.PINEAPPLE
            "BUTTERSCOTCH" -> Flavor.BUTTERSCOTCH
            else -> Flavor.NONE
        }
    }

    @TypeConverter
    fun fromFlavorsToString(flavors: List<Flavor>): String {
        var fString = ""
        for (i in flavors.indices) {
            fString += if (i != flavors.size - 1) {
                flavors[i].toString() + "-"
            } else {
                flavors[i].toString()
            }
        }
        return fString
    }

    @TypeConverter
    fun fromStringToFlavors(flavorString: String): List<Flavor> {
        val flavorsString = flavorString.split("-")
        val flavors = mutableListOf<Flavor>()
        for (flavor in flavorsString) {
            flavors.add(fromStringToFlavor(flavor))
        }
        return flavors
    }


    @TypeConverter
    fun fromOrderStatusToString(status: OrderStatus): String {
        return status.toString()
    }

    @TypeConverter
    fun fromStringToOrderStatus(status: String): OrderStatus {
        return when (status) {
            CREATED -> OrderStatus.Created
            PAID -> OrderStatus.Paid
            PREPARING -> OrderStatus.Preparing
            DELIVERED -> OrderStatus.Delivered
            DELIVERING -> OrderStatus.Delivering
            CANCELLED -> OrderStatus.Cancelled
            DUE -> OrderStatus.Due
            else -> OrderStatus.Cancelled
        }
    }

    @TypeConverter
    fun fromStringToOrderStatusList(s: String): List<OrderStatus> {
        val statuses = mutableListOf<OrderStatus>()
        for (status in s.split("|")) {
            statuses.add(fromStringToOrderStatus(status))
        }
        return statuses
    }

    @TypeConverter
    fun fromOrderStatusListToString(statuses: List<OrderStatus>): String {
        var f = ""
        for (i in statuses.indices) {
            f += if (i != statuses.size - 1) {
                fromOrderStatusToString(statuses[i]) + "|"
            } else {
                fromOrderStatusToString(statuses[i])
            }
        }
        return f
    }

    @TypeConverter
    fun fromOrderPriorityToString(priority: OrderPriority): String {
        return priority.toString()
    }

    @TypeConverter
    fun fromStringToOrderPriority(priority: String): OrderPriority {
        return when (priority) {
            LOW -> OrderPriority.Low
            MEDIUM -> OrderPriority.Medium
            HIGH -> OrderPriority.High
            else -> OrderPriority.Low
        }
    }

}