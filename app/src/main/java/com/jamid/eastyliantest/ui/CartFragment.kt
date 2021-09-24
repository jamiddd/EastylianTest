package com.jamid.eastyliantest.ui

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.*
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.adapter.CartItemAdapter
import com.jamid.eastyliantest.databinding.FragmentCartBinding
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.model.Result
import com.jamid.eastyliantest.utility.*
import com.razorpay.RazorpayClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CartFragment: Fragment(R.layout.fragment_cart) {

    private lateinit var binding: FragmentCartBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var mContext: Context
    private var dialog: Dialog? = null
    private var lastHeight = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        mContext = requireContext()
        lastHeight = convertDpToPx(200)

        val cartItemAdapter = initCartItemRecycler()

        viewModel.currentCartOrder.observe(viewLifecycleOwner) { currentOrder ->

            updateContainerUI(currentOrder)
            setCheckoutButton(currentOrder)

            if (currentOrder != null) {

                binding.cartItemRecycler.alpha = 0f
                val animator = ObjectAnimator.ofFloat(binding.cartItemRecycler, View.ALPHA, 1f)
                animator.duration = 500
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.start()

                cartItemAdapter.submitList(currentOrder.items.sortedByDescending {
                    it.cartItemId
                })

                updateDeliveryOrPickUpMethod(currentOrder)

                updateCashOnDeliveryUI(currentOrder)

            }
        }

        binding.deliveryToggle.addOnButtonCheckedListener { _, checkedId, b ->
            if (checkedId == R.id.normal && b) {
                viewModel.updateOrderTime(System.currentTimeMillis() + (6 * 60 * 60 * 1000), false)
            } else if (checkedId == R.id.custom && b) {
                val currentOrder = viewModel.currentCartOrder.value
                if (currentOrder != null) {
                    if (currentOrder.timeSetByUser) {
                        val deliveryText = "Delivering on - " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(currentOrder.deliveryAt)
                        binding.timeText.text = deliveryText
                    } else {
                        setOrderTime()
                    }
                } else {
                   Log.d(TAG, "This will happen when the current order becomes NULL.")
                }
            }
        }

        binding.pickupToggle.addOnButtonCheckedListener { _, checkedId, _ ->
            viewModel.updateDeliveryMethod(checkedId == R.id.delivery)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.pickupToggle.updateLayout(marginTop = top)
            binding.fragmentCartScroll.setPadding(0, convertDpToPx(8), 0, convertDpToPx(246))
            binding.bottomCartAction.setContentPadding(0, 0, 0, convertDpToPx(56))
        }

        binding.changeLocationBtn.setOnClickListener {
            findNavController().navigate(R.id.action_cartFragment_to_locationFragment2)
        }

        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.name.isNotBlank()) {
                    val currentOrder = viewModel.currentCartOrder.value
                    if (currentOrder != null) {
                        val currentOrderPlace = currentOrder.place
                        if (currentOrderPlace.latitude != it.latitude || currentOrderPlace.longitude != it.longitude) {
                            viewModel.updateOrderLocation(it)
                        }
                    }
                    binding.addressText.text = it.name
                } else {
                    binding.addressText.text = getString(R.string.no_location_text)
                }
            } else {
                binding.addressText.text = getString(R.string.no_location_text)
            }
        }

        val preferences = activity?.getSharedPreferences(EASTYLIAN_PREFERENCES, MODE_PRIVATE)
        val editor = preferences?.edit()

        if (preferences?.contains(PREF_DELIVERY_INFO) == true) {
            if (preferences.getBoolean(PREF_DELIVERY_INFO, false)) {
                binding.deliveryInfoText.hide()
            } else {
                binding.deliveryInfoText.show()
            }
        } else {
            binding.deliveryInfoText.show()
        }

        binding.deliveryInfoText.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                binding.deliveryInfoText.hide()
            }
            editor?.putBoolean(PREF_DELIVERY_INFO, true)
            editor?.apply()
        }

    }

    private fun updateCashOnDeliveryUI(currentOrder: Order) {

        if (!currentOrder.delivery) {
            binding.cashOnDeliveryBtn.hide()
        } else {
            binding.cashOnDeliveryBtn.show()
        }

        binding.cashOnDeliveryBtn.isChecked = currentOrder.paymentMethod == COD

        binding.cashOnDeliveryBtn.setOnCheckedChangeListener { _, isChecked ->
            val method = if (isChecked) {
                COD
            } else {
                "$ONLINE|$COUNTER"
            }
            currentOrder.paymentMethod = method
            viewModel.updateOrderPaymentMethod(currentOrder)
        }
    }

    private fun setOrderTime() {
        val builder = MaterialDatePicker.Builder.datePicker()

        val constraintsBuilderRange = CalendarConstraints.Builder()

        //....define min and max for example with LocalDateTime and ZonedDateTime or Calendar
        val now = System.currentTimeMillis()
        val min = now + 24 * 60 * 60 * 1000
        val max = min + (4 * 24 * 60 * 60 * 1000)

        val dateValidatorMin: DateValidator = DateValidatorPointForward.from(min)
        val dateValidatorMax: DateValidator =
            DateValidatorPointBackward.before(max)

        val listValidators = ArrayList<DateValidator>()
        listValidators.add(dateValidatorMin)
        listValidators.add(dateValidatorMax)
        val validators = CompositeDateValidator.allOf(listValidators)
        constraintsBuilderRange.setValidator(validators)

        builder.setCalendarConstraints(constraintsBuilderRange.build())

        builder.setTitleText("Select delivery date")
        val datePicker = builder.build()
        datePicker.show(childFragmentManager, "DatePicker")

        datePicker.addOnDismissListener {
            if (datePicker.selection == null) {
                binding.deliveryToggle.check(R.id.normal)
            }
        }

        datePicker.addOnPositiveButtonClickListener {
            if (it != null) {
                viewModel.updateOrderTime(it)
            }
        }
    }

    private fun updateDeliveryOrPickUpMethod(currentOrder: Order) {
        if (currentOrder.delivery) {
            if (binding.pickupToggle.checkedButtonId != R.id.delivery) {
                binding.pickupToggle.check(R.id.delivery)
            }
        } else {
            if (binding.pickupToggle.checkedButtonId != R.id.pickUp) {
                binding.pickupToggle.check(R.id.pickUp)
            }
            updateLocationUI(currentOrder)
        }
        updateDeliveryTimeUI(currentOrder)
        updateLocationUI(currentOrder)
        updateBill(currentOrder)
    }

    private fun updateDeliveryTimeUI(currentOrder: Order) {
        if (currentOrder.delivery) {
            binding.deliveryToggle.show()

            if (currentOrder.timeSetByUser) {
                val deliveryText = "Delivering on - " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(currentOrder.deliveryAt)
                binding.timeText.text = deliveryText
            } else {
                binding.timeText.text = getString(R.string.approx_time_text)
            }

        } else {
            binding.timeText.text = getString(R.string.approx_time_text)
            binding.deliveryToggle.hide()
        }
    }

    private fun updateContainerUI(currentOrder: Order?) {
        if (currentOrder != null && currentOrder.status[0] == OrderStatus.Created) {
            binding.cartContainerLayout.show()
            binding.bottomCartAction.show()
            binding.noItemsText.hide()
            binding.noItemsAnimation.hide()
            binding.pickupToggle.show()
        } else {
            binding.cartContainerLayout.hide()
            binding.noItemsText.show()
            binding.bottomCartAction.hide()
            binding.noItemsAnimation.show()
            binding.pickupToggle.hide()
        }
    }

    private fun initCartItemRecycler(): CartItemAdapter {
        val cartItemAdapter = CartItemAdapter()

        binding.cartItemRecycler.apply {
            adapter = cartItemAdapter
            layoutManager = LinearLayoutManager(mContext)
        }
        return cartItemAdapter
    }

    private fun initiateOrder(order: Order, onComplete: (res: Result<Order>) -> Unit)  = viewLifecycleOwner.lifecycleScope.launch (Dispatchers.IO) {
        try {
            val client = RazorpayClient(
                getString(R.string.razorpay_test_key),
                getString(R.string.razorpay_secret_key)
            )

            val orderRequest = JSONObject()
            orderRequest.put(AMOUNT, order.prices.total)
            orderRequest.put(CURRENCY, order.currency)
            orderRequest.put(RECEIPT, order.receiptId)

            val razorpayOrder = client.Orders.create(orderRequest)

            val razorpayOrderId = razorpayOrder.get<String>(ID)
            order.razorpayOrderId = razorpayOrderId

            val currentUser = viewModel.repo.currentUser.value!!
            RazorpayUtility.checkout(requireActivity(), currentUser, order)

            onComplete(Result.Success(order))
        } catch (e: Exception) {
            onComplete(Result.Error(e))
        }
    }

    private fun updateLocationUI(currentOrder: Order) {
        if (currentOrder.delivery) {
            binding.changeLocationBtn.show()
            binding.addressText.show()
            binding.deliveringToText.show()
        } else {
            binding.changeLocationBtn.hide()
            binding.addressText.hide()
            binding.deliveringToText.hide()
        }
    }

    private fun setCheckoutButton(currentOrder: Order?) {
        if (currentOrder != null && currentOrder.status[0] == OrderStatus.Created) {
            binding.checkOutBtn.show()

            if (currentOrder.paymentMethod == COD) {
                binding.checkOutBtn.text = getString(R.string.place_order)
                binding.checkOutBtn.setOnClickListener {

                    currentOrder.status = listOf(OrderStatus.Due)

                    viewModel.setCurrentCartOrder(currentOrder)

                    viewModel.uploadOrder(currentOrder) {
                        if (it.isSuccessful) {

                            val bundle = Bundle()
                            bundle.putBoolean(COD, true)

                            viewModel.setCurrentPaymentResult(Result.Success(true))

                            findNavController().navigate(R.id.action_cartFragment_to_paymentResultFragment, bundle, slideRightNavOptions())
                        } else {

                            it.exception?.let { e ->
                                viewModel.setCurrentError(e)
                            }
                        }
                    }
                }
            } else {
                binding.checkOutBtn.text = getString(R.string.checkout)

                binding.checkOutBtn.setOnClickListener {

                    binding.checkOutProgress.show()
                    binding.checkOutBtn.disable()

                    initiateOrder(currentOrder){
                        when (it) {
                            is Result.Error -> {
                                binding.checkOutProgress.disappear()
                                binding.checkOutBtn.enable()

                                Toast.makeText(requireContext(), it.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                            }
                            is Result.Success -> {
                                viewModel.uploadOrder(it.data) { it1 ->
                                    if (it1.isSuccessful) {
                                        findNavController().navigate(R.id.action_cartFragment_to_paymentResultFragment, null, slideRightNavOptions())
                                    } else {
                                        it1.exception?.localizedMessage?.toString()?.let { it2 ->
                                            Log.d(TAG,
                                                it2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val rightNow = Calendar.getInstance()
            val currentHourIn24Format = rightNow.get(Calendar.HOUR_OF_DAY)

            if (currentHourIn24Format > 20 || currentHourIn24Format < 9) {
                if (currentOrder.delivery) {
//                    binding.checkOutBtn.disable()
                } else {
                    binding.checkOutBtn.enable()
                }
            }

        } else {
            binding.checkOutBtn.hide()
        }
    }

    private fun updateBill(currentOrder: Order) {
        val cartPrices = currentOrder.prices
        var basePrice = ZERO_L

        for (item in currentOrder.items) {
            basePrice += item.totalPrice
        }

        binding.subtotalPrice.text = cartPrices.subTotalString
        binding.sgstPrice.text = cartPrices.sgstString
        binding.cgstPrice.text = cartPrices.cgstString
        val priceT = "₹ " + (basePrice/100).toString()
        binding.orderItemsPrice.text = priceT

        if (currentOrder.delivery) {
            binding.deliveryChargeLayout.visibility = View.VISIBLE
            binding.deliveryPrice.text = cartPrices.deliveryPriceString
        } else {
            binding.deliveryChargeLayout.visibility = View.GONE
        }

        binding.totalPrice.text = cartPrices.totalString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
    }

    companion object {

        private const val TAG = "CartFragment"

    }

}