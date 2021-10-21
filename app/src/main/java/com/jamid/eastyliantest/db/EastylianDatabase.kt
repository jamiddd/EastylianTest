package com.jamid.eastyliantest.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jamid.eastyliantest.EASTYLIAN_DATABASE
import com.jamid.eastyliantest.dao.*
import com.jamid.eastyliantest.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CakeMenuItem::class, Restaurant::class, SimpleNotification::class, Refund::class, Feedback::class, Faq::class, Cake::class, User::class, Payment::class, SimpleLocation::class, SimplePlace::class, CartItem::class, Order::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class EastylianDatabase: RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun placeDao(): PlaceDao
    abstract fun cakeDao(): CakeDao
    abstract fun orderDao(): OrderDao
    abstract fun cartDao(): CartDao
    abstract fun faqDao(): FaqDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun refundDao(): RefundDao
    abstract fun notificationDao(): NotificationDao
    abstract fun restaurantDao(): RestaurantDao
    abstract fun cakeMenuItemDao(): CakeMenuItemDao

    companion object {

//        private const val TAG = "EastylianDatabase"

        @Volatile private var instance: EastylianDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): EastylianDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context, scope)
            }
        }

        private fun createDatabase(applicationContext: Context, scope: CoroutineScope) : EastylianDatabase {
            return Room.databaseBuilder(applicationContext, EastylianDatabase::class.java, EASTYLIAN_DATABASE)
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    private class DatabaseCallback(val scope: CoroutineScope): RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch (Dispatchers.IO) {
                instance?.apply {
                    orderDao().clearTable()

                    cakeDao().removeCakesFromCartOrder()

                }
            }
        }
    }

}