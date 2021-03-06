package com.jamid.eastyliantest.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.databinding.FragmentPaymentResultBinding
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.Result
import com.jamid.eastyliantest.utility.convertDpToPx
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentResultFragment: Fragment(R.layout.fragment_payment_result) {

    private lateinit var binding: FragmentPaymentResultBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPaymentResultBinding.bind(view)

        viewModel.currentPaymentResult.observe(viewLifecycleOwner) { result ->
            viewLifecycleOwner.lifecycleScope.launch {
                delay(2000)
                binding.paymentAnimationLoading.hide()

                if (result != null) {
                    when (result) {
                        is Result.Error -> {
                            // payment unsuccessful
                            setAnimation(false)

                            binding.paymentResultMessage.show()

                            val msg = getString(R.string.failure_payment_text)

                            displayMessage(msg, false)

                            viewModel.deleteOrderFromFirebase()
                        }
                        is Result.Success -> {
                            viewModel.shouldCheckAccount = true
                            // payment successful
                            val isCashOnDelivery = result.data

                            val currentOrder = viewModel.currentCartOrder.value
                            if (currentOrder != null) {
                                val listOfCakes = mutableListOf<Cake>()
                                for (item in currentOrder.items) {
                                    val cake = item.cake
                                    cake.isAddedToCart = false
                                    listOfCakes.add(cake)
                                }

                                viewModel.insertCakes(listOfCakes)
                            }

                            viewModel.setCurrentCartOrder(null)

                            setAnimation(true)

                            val msg = if (isCashOnDelivery) {
                                getString(R.string.confirm_cod_text)
                            } else {
                                getString(R.string.confirm_payment_text)
                            }

                            displayMessage(msg, true)

                        }
                    }


                    delay(8000)
                    findNavController().navigateUp()

                }
            }

        }

        binding.checkOrderBtn.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun displayMessage(msg: String, isPaymentSuccessful: Boolean) {
        if (isPaymentSuccessful) {
            binding.paymentResultMessage.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.greenDarkTextColor
            ))
        } else {
            binding.paymentResultMessage.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.darkRedTextColor
            ))
        }
        binding.paymentResultMessage.text = msg
    }

    private fun setAnimation(isPaymentSuccessful: Boolean) {
        if (isPaymentSuccessful) {
            binding.paymentAnimationSuccess.show()
            binding.paymentAnimationSuccess.playAnimation()

            binding.checkOrderBtn.alpha = 0f
            binding.checkOrderBtn.show()

            binding.paymentResultMessage.alpha = 0f
            binding.paymentResultMessage.show()

            val animator = ObjectAnimator.ofFloat(binding.checkOrderBtn, View.ALPHA, 1f)
            val animator1 = ObjectAnimator.ofFloat(binding.checkOrderBtn, View.TRANSLATION_Y, convertDpToPx(100).toFloat(), 0f)
            val animator2 = ObjectAnimator.ofFloat(binding.paymentResultMessage, View.ALPHA, 1f)
            val animator3 = ObjectAnimator.ofFloat(binding.paymentResultMessage, View.TRANSLATION_Y, convertDpToPx(100).toFloat(), 0f)

            AnimatorSet().apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(animator, animator1, animator2, animator3)
                start()
            }

        } else {
            binding.paymentAnimationFailure.show()
            binding.paymentAnimationFailure.playAnimation()
        }
    }

}