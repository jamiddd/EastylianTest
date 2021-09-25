package com.jamid.eastyliantest.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.repo.MainRepository

class AuthActivity : AppCompatActivity() {
    private lateinit var repository: MainRepository
    val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(repository) }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val image = it.data?.data
            viewModel.setCurrentImage(image)
        } else {
            viewModel.setCurrentImage(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        val database = EastylianDatabase.getInstance(applicationContext, lifecycleScope)
        repository = MainRepository.newInstance(database)
    }

    fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        selectImageLauncher.launch(intent)
    }
}