package com.jamid.eastyliantest.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.ui.auth.AuthActivity
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.startActivityBasedOnAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : AppCompatActivity() {

    val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(3000)
            findViewById<ProgressBar>(R.id.splashProgressBar)?.show()
            checkAuth()
        }
    }

    private fun checkAuth() {
        val user = auth.currentUser
        if (user != null) {
            try {
                user.getIdToken(false).addOnSuccessListener {
                    startActivityBasedOnAuth(it)
                }.addOnFailureListener {
                    Log.e(TAG, it.localizedMessage!!)
                }
            } catch (e: Exception) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}