package com.jamid.eastyliantest.db

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jamid.eastyliantest.model.Result
import com.jamid.eastyliantest.repo.MainRepository

private const val TAG = "FirebaseRemoteMediator"

@ExperimentalPagingApi
abstract class FirebaseRemoteMediator<K: Any, T: Any>(val query: Query, val repository: MainRepository): RemoteMediator<K, T>() {

	open var lastSnapshot: DocumentSnapshot? = null

	override suspend fun load(loadType: LoadType, state: PagingState<K, T>): MediatorResult {

		val itemQueryResult = when (loadType) {
			LoadType.REFRESH -> {
				onRefresh()
				repository.firebaseUtility.fetchItems(query)
			}
			LoadType.PREPEND -> {
				return MediatorResult.Success(true)
			}
			LoadType.APPEND -> {
				repository.firebaseUtility.fetchItems(query, lastSnapshot = lastSnapshot)
			}
		}

		return when (itemQueryResult) {
			is Result.Error -> {
				Log.e(TAG, "Error occurred while fetching items from fireStore -> " + itemQueryResult.exception.localizedMessage)
				MediatorResult.Error(itemQueryResult.exception)
			}
			is Result.Success -> {
				Log.d(TAG, "Fetching complete ..")
				val itemQuerySnapshot = itemQueryResult.data

				if (itemQuerySnapshot.isEmpty) {
					return MediatorResult.Success(true)
				}

				lastSnapshot = itemQuerySnapshot.lastOrNull()

				onLoadComplete(itemQuerySnapshot)

				return if (itemQuerySnapshot.size() > 10) {
					MediatorResult.Success(false)
				} else {
					MediatorResult.Success(true)
				}
			}
		}
	}

	abstract suspend fun onLoadComplete(items: QuerySnapshot)
	abstract suspend fun onRefresh()

}