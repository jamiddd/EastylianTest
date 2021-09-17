package com.jamid.eastyliantest.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentSignInBinding
import com.jamid.eastyliantest.db.EastylianDatabase
import com.jamid.eastyliantest.repo.MainRepository
import com.jamid.eastyliantest.utility.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SignInFragment : Fragment(R.layout.fragment_sign_in), View.OnClickListener {

    private lateinit var binding: FragmentSignInBinding
    private val auth = Firebase.auth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private lateinit var viewModel: AuthViewModel
    private lateinit var userPhoneNumber: String

    enum class ButtonMode {
        VERIFY_OTP,
        SEND_OTP
    }

    private var currentButtonMode: ButtonMode = ButtonMode.SEND_OTP

    private var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                Log.w(TAG, "Invalid request for OTP")
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                Log.d(TAG, "The SMS quota for the project has been exceeded")
                // The SMS quota for the project has been exceeded
            }

            toast("Something went wrong. Try again with different number!!")

            // Show a message and update the UI
            updateUIOnFailure()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            updateUIOnSentOTP()
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)

        viewModel = ViewModelProviders.of(requireActivity(), AuthViewModelFactory(
			MainRepository.newInstance(
            EastylianDatabase.getInstance(requireContext(), lifecycleScope)))).get(AuthViewModel::class.java)

        binding.signInBottomContent.phoneInput.doAfterTextChanged {
            binding.signInBottomContent.phoneInputLayout.error = null
            binding.signInBottomContent.signInButton.isEnabled = !it.isNullOrBlank() && it.length == 10
        }

        requireActivity().let {
            it.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                it.finish()
            }
        }

        binding.signInBottomContent.signInButton.setOnClickListener(this)

        binding.signInBottomContent.otpLayout.editText?.doAfterTextChanged {
            binding.signInBottomContent.signInButton.isEnabled = !it.isNullOrBlank() && it.length == 6
        }

        viewModel.repository.firebaseUtility.currentFirebaseUserLive.observe(viewLifecycleOwner) {
            if (it != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val isUserRegistered = viewModel.checkIfUserRegistered(it.uid)
                    if (isUserRegistered != null) {
                        if (isUserRegistered) {
                            it.getIdToken(false).addOnCompleteListener { it1 ->
                                if (it1.isSuccessful) {
                                    startActivityBasedOnAuth(it1.result)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Something went wrong while fetching user data.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            findNavController().navigate(R.id.introFragment, Bundle().apply { putString("phone", userPhoneNumber) })
                        }
                    } else {
                        toast("Something went wrong. Please try again.")
                        updateUIOnFailure()
                    }
                }
            } else {
                //
            }
        }

        binding.signInBottomContent.resendBtn.setOnClickListener {
            sendOtp(userPhoneNumber.split(" ").last())
            binding.signInBottomContent.resendBtn.disable()
        }

    }

    private fun sendOtp(phoneNumber: String) {
        updateUIOnStartVerification()
        val formattedNumber = "+91 $phoneNumber"
        userPhoneNumber = formattedNumber
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)       // Phone number to verify
            .setTimeout(20L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())     // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val firebaseUser = task.result.user
                    viewModel.repository.firebaseUtility.currentFirebaseUserLive.postValue(firebaseUser)
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        toast("The code you entered was wrong. Try again.")
                        // The verification code entered was invalid
                    }

                    // Update UI
                    updateUIOnFailure()
                }
            }
    }


    private fun updateUIOnFailure() {
        currentButtonMode = ButtonMode.SEND_OTP
        binding.signInBottomContent.apply {
            phoneInputLayout.enable()
            phoneInputLayout.show()
            phoneInputLayout.editText?.text?.clear()
            otpLayout.editText?.text?.clear()
            signInButton.text = getString(R.string.send_otp_text)
            signInButton.enable()
            otpLayout.hide()
            signInProgressBar.disappear()
        }
    }

    private fun updateUIOnVerification() {
        binding.signInBottomContent.apply {
            phoneInputLayout.hide()
            otpLayout.disable()
            signInProgressBar.show()
            signInButton.disable()
            otpLayout.hide()
        }
    }

    private fun updateUIOnSentOTP() {
        currentButtonMode = ButtonMode.VERIFY_OTP
        binding.signInBottomContent.apply {
            phoneInputLayout.hide()
            phoneInputLayout.editText?.text?.clear()
            otpLayout.editText?.text?.clear()
            signInButton.text = getString(R.string.verify_text)
            signInButton.disable()
            otpLayout.show()
            signInProgressBar.disappear()
        }
    }

    private fun updateUIOnStartVerification() {
        binding.signInBottomContent.apply {
            phoneInputLayout.hide()
            phoneInputLayout.editText?.text?.clear()
            otpLayout.editText?.text?.clear()
            signInButton.text = getString(R.string.verify_text)
            signInButton.disable()
            otpLayout.enable()
            signInProgressBar.show()
        }
    }


    companion object {

        private const val TAG = "SignInFragment"

        @JvmStatic
        fun newInstance() = SignInFragment()

    }

    override fun onClick(v: View?) {
        if (v?.id == binding.signInBottomContent.signInButton.id) {
            when (currentButtonMode) {
                ButtonMode.VERIFY_OTP -> {

                    updateUIOnVerification()

                    binding.signInBottomContent.otpLayout.editText?.text?.let {
                        verifyOTP(it.toString())
                    }

                }
                ButtonMode.SEND_OTP -> {

                    binding.signInBottomContent.phoneInputLayout.editText?.text?.let {
                        sendOtp(it.toString())
                    }

                    binding.signInBottomContent.resendBtn.show()
                    binding.signInBottomContent.resendBtn.disable()

                    viewLifecycleOwner.lifecycleScope.launch {
                        for (i in 20 downTo 1) {
                            delay(1000)
                            binding.signInBottomContent.resendBtn.text = "Resend ($i s)"
                        }

                        binding.signInBottomContent.resendBtn.text = "Resend"

                        binding.signInBottomContent.resendBtn.enable()
                    }

                }
            }
        }
    }

    private fun verifyOTP(otp: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }
}