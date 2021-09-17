package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.SimplePlace

@Dao
abstract class PlaceDao: BaseDao<SimplePlace>() {

    @Query("SELECT * FROM places WHERE isUserAccurate = 1 LIMIT 1")
    abstract fun getCurrentPlace(): LiveData<SimplePlace>

    @Query("UPDATE places SET isUserAccurate = 0 WHERE isUserAccurate = 1")
    abstract suspend fun updatePastLocations()

    @Query("SELECT * FROM places WHERE isUserAccurate = 0 LIMIT 10")
    abstract fun getPastLocations(): LiveData<List<SimplePlace>>

    @Query("DELETE FROM places")
    abstract suspend fun clearTable()

}