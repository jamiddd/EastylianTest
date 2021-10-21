package com.jamid.eastyliantest.ui

import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentPaymentBinding
import com.jamid.eastyliantest.databinding.InputLayoutBinding
import com.jamid.eastyliantest.interfaces.OnPaymentModeSelected
import com.jamid.eastyliantest.utility.toast

class PaymentFragment: BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPaymentBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentBinding.inflate(inflater)
        return binding.root
    }

    private val option: Int
        get() {
            return when (binding.paymentOptionGroup.checkedRadioButtonId) {
                R.id.upiBtn -> 0
                R.id.cashOnDeliveryBtn -> 1
                else -> 0
            }
        }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amount = arguments?.getString(ARG_AMOUNT) ?: return

        val paymentModeSelectListener = requireActivity() as OnPaymentModeSelected

        when (requireActivity().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.paymentBtn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryColorVariant))
            }
            Configuration.UI_MODE_NIGHT_NO -> {

            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {

            }
        }

        binding.upiBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Checking phone number ... ")
                .setMessage("Is the number that you've signed in, same as the one linked to your UPI account?")
                .setCancelable(false)
                .setPositiveButton("Yes") { a, _ ->
                    a.dismiss()
                }.setNegativeButton("No") { _, _ ->

                    val v = layoutInflater.inflate(R.layout.input_layout, null, false)
                    val inputLayoutBinding = InputLayoutBinding.bind(v)

                    inputLayoutBinding.inputLayoutText.hint = "Phone number"
                    inputLayoutBinding.inputLayoutText.editText?.inputType = InputType.TYPE_CLASS_NUMBER
                    inputLayoutBinding.inputLayoutText.prefixText = "+91"

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Enter the phone number linked with your upi id")
                        .setCancelable(false)
                        .setView(inputLayoutBinding.root)
                        .setPositiveButton("Done") { _, _ ->

                            if (!inputLayoutBinding.inputLayoutText.editText?.text.isNullOrBlank()) {
                                val upiNumber = inputLayoutBinding.inputLayoutText.editText?.text.toString()
                                viewModel.setCurrentUserUpiNumber(upiNumber) {
                                    if (!it.isSuccessful) {
                                        toast("Something went wrong while trying to change your UPI number.")
                                        binding.paymentOptionGroup.check(R.id.cashOnDeliveryBtn)
                                        Log.d(TAG, it.exception?.localizedMessage.orEmpty())
                                    }
                                }
                            }
                        }.setNegativeButton("Cancel") { _, _ ->
                            binding.paymentOptionGroup.check(R.id.cashOnDeliveryBtn)
                        }.show()

                }.show()
        }

        binding.paymentBtn.setOnClickListener {
            if (option == 0) {
                paymentModeSelectListener.onUpiSelected(amount)
            } else {
                paymentModeSelectListener.onCashOnDeliverySelected()
            }
            dismiss()
        }

    }

    companion object {

        private const val TAG = "PaymentFragment"
        private const val ARG_AMOUNT = "ARG_AMOUNT"

        fun newInstance(amount: String) =
            PaymentFragment().apply {
                arguments = bundleOf(ARG_AMOUNT to amount)
            }
    }

}