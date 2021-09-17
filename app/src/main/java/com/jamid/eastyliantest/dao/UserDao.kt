package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.User

@Dao
abstract class UserDao: BaseDao<User>() {
    // TODO("Class to transact user data")

    @Query("SELECT * FROM users LIMIT 1")
    abstract fun currentUser(): LiveData<User>

    @Query("DELETE FROM users")
    abstract suspend fun clearTable()

}