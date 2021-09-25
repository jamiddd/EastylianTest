package com.jamid.eastyliantest.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.jamid.eastyliantest.PHONE
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentIntroBinding
import com.jamid.eastyliantest.utility.*

class IntroFragment: Fragment(R.layout.fragment_intro) {

    private lateinit var binding: FragmentIntroBinding
    private val viewModel: AuthViewModel by activityViewModels()
    private val formState = MutableLiveData<Pair<Boolean, Boolean>>().apply{ value = Pair(first = false, second = false)}
    private var userPhoto: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentIntroBinding.bind(view)

        val phoneNumber = arguments?.getString(PHONE) ?: return

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

        viewModel.currentImage.observe(viewLifecycleOwner) {
            binding.uploadProgress.show()
            if (it != null) {
                viewModel.uploadImage(it) { it1 ->
                    if (it1 != null) {
                        userPhoto = it1.toString()
                        binding.userImage.setImageURI(userPhoto)
                        binding.uploadProgress.hide()
                    }
                }
            } else {
                userPhoto = null
                binding.userImage.setImageURI(userPhoto)
                binding.uploadProgress.hide()
            }
        }

        formState.observe(viewLifecycleOwner) {
            binding.finalizeSetupBtn.isEnabled = it.first == true && it.second == true
        }

        binding.finalizeSetupBtn.setOnClickListener {

            binding.finalizeSetupBtn.disappear()
            binding.introFragmentProgress.show()

            val emailText = binding.emailInputLayout.editText?.text
            val name = binding.userNameLayout.editText?.text

            if (name.isNullOrBlank()) {
                toast("Please enter a name")
                return@setOnClickListener
            }

            if (emailText.isNullOrBlank()) {
                toast("Please provide an email")
                return@setOnClickListener
            }

            viewModel.updateFirebaseUser(mapOf(
                "email" to emailText.toString(),
                "fullName" to name.toString(),
                "photoUrl" to userPhoto
            )) {
                if (it.isSuccessful) {
                     viewModel.uploadNewUser(name.toString(), phoneNumber, emailText.toString(), userPhoto)
                } else {
                    binding.introFragmentProgress.hide()
                    binding.finalizeSetupBtn.show()
                    binding.finalizeSetupBtn.enable()
                    toast("Something went wrong while uploading user data.")
                }
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

        binding.userImage.setOnClickListener {
            showMenu(it)
        }

    }

    private fun showMenu(anchor: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)

        popupMenu.inflate(R.menu.image_menu)

        popupMenu.setOnMenuItemClickListener { it1 ->
            when (it1.itemId) {
                R.id.change_image -> {
                    (activity as AuthActivity?)?.selectImage()
                }
                R.id.remove_image -> {
                    viewModel.setCurrentImage(null)
                }
            }

            return@setOnMenuItemClickListener true
        }

        popupMenu.show()
    }

    private fun CharSequence?.isValidEmail() =
        !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

}