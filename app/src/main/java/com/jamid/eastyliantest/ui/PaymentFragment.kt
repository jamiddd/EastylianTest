package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentPaymentBinding
import com.jamid.eastyliantest.interfaces.OnPaymentModeSelected
import com.jamid.eastyliantest.utility.convertDpToPx
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

        binding.upiBtn.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Checking phone number ... ")
                .setMessage("Is the number that you've signed in, same as the one linked to your UPI account?")
                .setCancelable(false)
                .setPositiveButton("Yes") { a, _ ->
                    a.dismiss()
                }.setNegativeButton("No") { _, _ ->
                    val editText = EditText(requireContext())
                    editText.setPadding(convertDpToPx(12))

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Enter the phone number linked with your upi id")
                        .setCancelable(false)
                        .setView(editText)
                        .setPositiveButton("Done") { _, _ ->
                            if (!editText.text.isNullOrBlank()) {
                                val upiNumber = editText.text.toString()
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