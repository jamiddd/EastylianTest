package com.jamid.eastyliantest.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.R

class AuthActivity : AppCompatActivity() {
    private lateinit var repository: MainRepository
    val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)
    }
}