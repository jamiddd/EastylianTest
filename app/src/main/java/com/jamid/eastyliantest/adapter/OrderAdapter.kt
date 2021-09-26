package com.jamid.eastyliantest.adapter

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.USERS
import com.jamid.eastyliantest.databinding.OrderItemCustomerLayoutBinding
import com.jamid.eastyliantest.interfaces.OrderClickListener
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderStatus.*
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.utility.disable
import com.jamid.eastyliantest.utility.enable
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "OrderAdapter"

class OrderAdapter: ListAdapter<Order, OrderViewHolder>(OrderComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.order_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class OrderComparator: DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
        return oldItem == newItem
    }
}

class OrderViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val orderClickListener = view.context as OrderClickListener

    private val progressBar: ProgressBar = view.findViewById(R.id.orderProgress)
    private val primaryAction: MaterialButton = view.findViewById(R.id.primaryOrderAction)
    private val secondaryAction: MaterialButton = view.findViewById(R.id.secondaryOrderAction)
    private val orderDeliveryText: MaterialButton = view.findViewById(R.id.deliveryStatus)
    private val bottomActionLayout: View = view.findViewById(R.id.actionLayout)
    private val actionDivider: View = view.findViewById(R.id.primaryActionDivider)
    private val deliveryBoyAnimation: LottieAnimationView = view.findViewById(R.id.deliveryBoyAnimation)
    private val orderCustomerDivider = view.findViewById<View>(R.id.divider14)

    var isAdmin = false
    var isDeliveryExecutive = false
    val randomIcons = mutableListOf<String>()

    fun resetState() {
        progressBar.hide()
        primaryAction.enable()
        secondaryAction.enable()
    }

    fun bind(order: Order?) {

        if (order == null)
            return

        Log.d(TAG, order.toString())

        val orderId = view.findViewById<TextView>(R.id.orderId)
        val orderTotalPrice = view.findViewById<TextView>(R.id.orderTotalPrice)
        val orderItemsRecycler = view.findViewById<RecyclerView>(R.id.orderItemsRecycler)

        resetState()

        val orderItemsAdapter = OrderItemAdapter(order.items)

        orderItemsRecycler.apply {
            adapter = orderItemsAdapter
            layoutManager = LinearLayoutManager(view.context)
        }

        // constant changes
        val priceText = "₹ ${(order.prices.total)/100}"
        orderTotalPrice.text = priceText
        orderId.text = order.orderId

        primaryAction.setOnClickListener {
            finalState()
            orderClickListener.onPrimaryActionClick(this, order)
        }

        secondaryAction.setOnClickListener {
            finalState()
            orderClickListener.onSecondaryActionClick(this, order)
        }


        if (isAdmin) {
            // do everything for admin
            setUpOrderForAdmin(order)
            return
        }

        if (isDeliveryExecutive) {
            // do everything related to delivery executive
            setUpOrderForDeliveryExecutive(order)
            return
        }

        // do everything related to the user
        setUpForCustomer(order)

    }

    private fun finalState() {
        progressBar.show()
        primaryAction.disable()
        secondaryAction.disable()
    }

    private fun setOrderDeliveryText(msg: String, @ColorRes color: Int, @DrawableRes icon: Int, visibility: Boolean = true) {
        val c = ContextCompat.getColor(view.context, color)
        orderDeliveryText.text = msg
        orderDeliveryText.icon = ContextCompat.getDrawable(view.context, icon)
        orderDeliveryText.setTextColor(c)
        orderDeliveryText.iconTint = ColorStateList.valueOf(c)

        if (visibility) {
            orderDeliveryText.show()
        } else {
            orderDeliveryText.hide()
        }
    }

    private fun setUpOrderForAdmin(order: Order) {
        when (order.status[0]) {
            Created -> {
                // nothing happens here
            }
            Due, Paid -> {
                // it's a request, admin can cancel the order here
                setBottomActionVisibility(true)
                setUpPrimaryActionButton("Start Order")
                setUpSecondaryActionButton("Cancel")

                val deliveryText = "Order to be delivered on " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(order.deliveryAt)
                setOrderDeliveryText(deliveryText, R.color.primaryColor, R.drawable.ic_new_releases_black_24dp__1_)

            }
            Preparing -> {

                if (order.delivery) {
                    setBottomActionVisibility(false)
                } else {
                    setBottomActionVisibility(true)
                    setUpPrimaryActionButton("Notify customer")
                }

                val deliveryText = "Order is being prepared."
                setOrderDeliveryText(deliveryText, R.color.blueDarkTextColor, R.drawable.ic_play_circle_black_24dp)

            }
            Delivering -> {
                val deliveryText = "Delivering now. The order will reach within short time."
                if (!order.delivery) {
                    setBottomActionVisibility(true)
                    setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_round_directions_bike_24, false)

                    setUpPrimaryActionButton("Deliver Order", true)

                } else {
                    setBottomActionVisibility(false)

                    setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_round_directions_bike_24)
                }

            }
            Delivered -> {
                // if the order is cash on delivery, admin needs to finish the order.
                if (order.paymentMethod == "cod") {
                    if (order.paymentId.isBlank()) {
                        setUpPrimaryActionButton("Finish Order")
                        setBottomActionVisibility(true)
                    } else {
                        setBottomActionVisibility(false)
                    }
                } else {
                    setBottomActionVisibility(false)
                }

                val deliveryText = "Order was delivered at - " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(order.deliveryAt)
                setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_verified_black_24dp)

            }
            Cancelled -> {
                setBottomActionVisibility(false)
            }
            Rejected -> {
                // not implemented yet
            }
        }

        view.setOnClickListener {
            displayCustomer(order)
        }

    }

    private fun displayCustomer(order: Order) {

        val inflatedView = view.findViewById<View>(R.id.customer)

        if (inflatedView == null) {
            orderCustomerDivider.show()
            val st = view.findViewById<ViewStub>(R.id.customerLayoutStub)
            val v = st.inflate()

            val binding = OrderItemCustomerLayoutBinding.bind(v)

            Firebase.firestore.collection(USERS).document(order.senderId).get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        val customer = it.toObject(User::class.java)!!
                        binding.customerName.text = customer.name

                        if (!customer.photoUrl.isNullOrBlank()) {
                            binding.customerImage.setImageURI(customer.photoUrl)
                        } else {
                            if (randomIcons.isNotEmpty()) {
                                val image = randomIcons.random()
                                binding.customerImage.setImageURI(image)
                            }
                        }

                        binding.callCustomerBtn.setOnClickListener {
                            orderClickListener.onCustomerClick(this, customer)
                        }
                        binding.customerName.setOnClickListener {
                            orderClickListener.onCustomerClick(this, customer)
                        }
                    }
                }.addOnFailureListener {
                    Log.d(TAG, it.localizedMessage!!)
                }
        } else {
            // Don't do anything, or maybe hide it temporarily
            if (inflatedView.isVisible) {
                orderCustomerDivider.hide()
                inflatedView.hide()
            } else {
                orderCustomerDivider.show()
                inflatedView.show()
            }
        }

    }

    private fun setBottomActionVisibility(state: Boolean) {
        // whether actions are needed or not
        if (state) {
            bottomActionLayout.show()
            actionDivider.show()
        } else {
            bottomActionLayout.hide()
            actionDivider.hide()
        }
    }

    private fun setUpPrimaryActionButton(label: String, isVisible: Boolean = true) {
        primaryAction.text = label
        if (isVisible) {
            primaryAction.show()
        } else {
            primaryAction.hide()
        }
    }

    private fun setUpSecondaryActionButton(label: String, isVisible: Boolean = true) {
        secondaryAction.text = label
        if (isVisible) {
            secondaryAction.show()
        } else {
            secondaryAction.hide()
        }
    }

    private fun setUpOrderForDeliveryExecutive(order: Order) {
        when (order.status[0]) {
            Created -> {
                // nothing here
            }
            Due, Paid -> {
                // nothing here
            }
            Preparing -> {
                setBottomActionVisibility(true)
                if (order.delivery) {
                    setUpPrimaryActionButton("Navigate", true)
                } else {
                    setUpPrimaryActionButton("Notify Customer", true)
                }

                val hours = kotlin.math.abs((((System.currentTimeMillis() - order.deliveryAt) / 1000) / 60) / 60)

                val deliveryText = "This order is expected to finish in $hours Hours."
                setOrderDeliveryText(deliveryText, R.color.blueDarkTextColor, R.drawable.ic_play_circle_black_24dp)

            }
            Delivering -> {
                setBottomActionVisibility(true)
                setUpPrimaryActionButton("Deliver Order", true)
                setUpSecondaryActionButton("Call customer", true)

                val deliveryText = "Order to be delivered on - " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(order.deliveryAt)
                setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_round_directions_bike_24)

            }
            Delivered -> {
                // nothing here
            }
            Cancelled -> {
                // nothing here
            }
            Rejected -> {
                // not implemented yet
            }
        }

        view.setOnClickListener {
            displayCustomer(order)
        }
    }

    private fun setUpForCustomer(order: Order) {

        when (order.status[0]) {
            Created -> {
                // nothing here
            }
            Due, Paid -> {

                val deliveryText = "Order to be delivered on - " + SimpleDateFormat("dd, MMM, EEEE", Locale.getDefault()).format(order.deliveryAt)
                setOrderDeliveryText(deliveryText, R.color.primaryColor, R.drawable.ic_new_releases_black_24dp__1_)

            }
            Preparing -> {

                val deliveryText = "Working on it. Building a nice and tasty cake for you."
                setOrderDeliveryText(deliveryText, R.color.blueDarkTextColor, R.drawable.ic_play_circle_black_24dp)

            }
            Delivering -> {
                setBottomActionVisibility(true)

                if (order.delivery) {
                    deliveryBoyAnimation.show()

                    setUpSecondaryActionButton("Call delivery Executive", true)
                } else {
                    setUpPrimaryActionButton("Navigate", true)
                }

                val deliveryText = if (order.delivery) {
                    "Delivering now, will reach within short time."
                } else {
                    "Pick up your cake ASAP. It's calling you."
                }
                setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_round_directions_bike_24)

            }
            Delivered -> {
                setBottomActionVisibility(true)

                setUpSecondaryActionButton("Feedback", true)
                setUpPrimaryActionButton("Reorder", true)

                val deliveryText = "Delivered on - " + SimpleDateFormat("hh:mm a - dd, MMM, EEEE", Locale.getDefault()).format(order.deliveryAt)
                setOrderDeliveryText(deliveryText, R.color.greenDarkTextColor, R.drawable.ic_verified_black_24dp)

            }
            Cancelled -> {
                setBottomActionVisibility(false)

                val deliveryText = "Order was cancelled. If money was deducted, it will be refunded."
                setOrderDeliveryText(deliveryText, R.color.darkRedTextColor, R.drawable.ic_round_error_24)

            }
            Rejected -> {
                // not implemented yet
            }
        }
    }

}