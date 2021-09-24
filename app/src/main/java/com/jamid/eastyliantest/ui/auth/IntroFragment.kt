package com.jamid.eastyliantest.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.jamid.eastyliantest.EMAIL
import com.jamid.eastyliantest.PHONE
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentIntroBinding
import com.jamid.eastyliantest.utility.startActivityBasedOnAuth
import com.jamid.eastyliantest.utility.toast

class IntroFragment: Fragment(R.layout.fragment_intro) {

    private lateinit var binding: FragmentIntroBinding
    private val viewModel: AuthViewModel by activityViewModels()
    private val formState = MutableLiveData<Pair<Boolean, Boolean>>().apply{ value = Pair(first = false, second = false)}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentIntroBinding.bind(view)

        val phoneNumber = arguments?.getString(PHONE)
        val email = arguments?.getString(EMAIL)

        binding.emailInputLayout.editText?.setText(email)

        binding.userNameLayout.editText?.doAfterTextChanged {
            formState.postValue(Pair(
                (!it.isNullOrBlank() && it.length > 3),
                (!binding.emailInputLayout.editText!!.text.isNullOrBlank() &&
                        binding.emailInputLayout.editText!!.text!!.toString().isValidEmail())
                )
            )
        }

        binding.emailInputLayout.editText?.doAfterTextChanged {
            formState.postValue(Pair(
                (!it.isNullOrBlank() && it.toString().isValidEmail()),
                (!binding.userNameLayout.editText!!.text.isNullOrBlank() &&
                        binding.userNameLayout.editText!!.text!!.length > 3)))
        }

        formState.observe(viewLifecycleOwner) {
            binding.finalizeSetupBtn.isEnabled = it.first == true && it.second == true
        }

        binding.finalizeSetupBtn.setOnClickListener {
            binding.finalizeSetupBtn.visibility = View.INVISIBLE
            binding.introFragmentProgress.visibility = View.VISIBLE
            val emailText = binding.emailInputLayout.editText!!.text.toString()
            val name = binding.userNameLayout.editText!!.text.toString()
            viewModel.updateFirebaseUser(mapOf("email" to emailText, "fullName" to name))
        }

        viewModel.repository.firebaseUtility.currentFirebaseUserLive.observe(viewLifecycleOwner) {
            if (it != null && it.displayName != null) {
                val emailText = binding.emailInputLayout.editText!!.text.toString()
                val name = binding.userNameLayout.editText!!.text.toString()
                viewModel.uploadNewUser(name, phoneNumber, emailText)
            }
        }

        viewModel.repository.currentUser.observe(viewLifecycleOwner) {
            if (it != null) {
                val currentFirebaseUser = viewModel.repository.firebaseUtility.currentFirebaseUserLive.value
                currentFirebaseUser?.getIdToken(false)?.addOnCompleteListener { it1 ->
                    if (it1.isSuccessful) {
                        startActivityBasedOnAuth(it1.result)
                    } else {
                        toast("Something went wrong.")
                    }
                }
            }
        }

        viewModel.repository.firebaseUtility.networkErrors.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.introFragmentProgress.visibility = View.GONE
                binding.finalizeSetupBtn.visibility = View.VISIBLE
            }
        }

    }

    private fun CharSequence?.isValidEmail() =
        !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

}