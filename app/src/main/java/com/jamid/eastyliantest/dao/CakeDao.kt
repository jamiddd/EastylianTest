package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.Cake

@Dao
abstract class CakeDao: BaseDao<Cake>() {
	//
	@Query("SELECT * FROM cakes WHERE isFavorite = 1")
	abstract fun favoriteCakes(): LiveData<List<Cake>>

	@Query("SELECT * FROM cakes WHERE isCustomizable = 0")
	abstract fun customCakes(): LiveData<List<Cake>>

	@Query("DELETE FROM cakes")
	abstract suspend fun clearTable()
}