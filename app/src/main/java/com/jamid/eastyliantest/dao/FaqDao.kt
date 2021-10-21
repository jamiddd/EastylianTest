package com.jamid.eastyliantest.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.eastyliantest.model.Faq

@Dao
abstract class FaqDao: BaseDao<Faq>() {

	@Query("SELECT * FROM faqs")
	abstract fun allFaqs(): LiveData<List<Faq>>

	@Query("DELETE FROM faqs")
	abstract suspend fun clearTable()

	@Query("SELECT * FROM faqs WHERE answered = 0 ORDER BY createdAt DESC")
	abstract fun pagedQuestions(): PagingSource<Int, Faq>
}