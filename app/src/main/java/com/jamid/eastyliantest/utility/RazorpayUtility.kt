package com.jamid.eastyliantest.utility

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.*
import com.jamid.eastyliantest.model.Order
import com.jamid.eastyliantest.model.OrderStatus
import com.jamid.eastyliantest.model.Refund
import com.jamid.eastyliantest.model.User
import com.razorpay.Checkout
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.json.JSONObject

object RazorpayUtility {

    private val client = RazorpayClient(BuildConfig.RAZORPAY_TEST_KEY, BuildConfig.RAZORPAY_SECRET_KEY)

    private const val logoUrl = "https://firebasestorage.googleapis.com/v0/b/testeastylian.appspot.com/o/logo-6.svg?alt=media&token=9f985a26-8547-4b3a-8a83-d0677a1d149a"

    fun initiateRefund(order: Order, onComplete: (task: Task<Void>) -> Unit) {
        val batch = Firebase.firestore.batch()
        if (order.status[0] == OrderStatus.Paid) {
            try {
                val refundRequest = JSONObject()
                refundRequest.put("payment_id", order.paymentId)
                refundRequest.put(AMOUNT, order.prices.total.toInt().toString())
                refundRequest.put(SPEED, OPTIMUM)
                val r1 = client.Refunds.create(refundRequest)

                val refundId = r1.get<String>(ID)
                val status = r1.get<String>(STATUS)

                val refund = Refund(refundId, order.orderId, order.senderId, order.paymentId, order.prices.total, status, System.currentTimeMillis())

                val ref = Firebase.firestore.collection(USERS)
                    .document(order.senderId)
                    .collection(REFUNDS)
                    .document(refundId)

                batch.set(ref, refund)
            } catch (e: RazorpayException) {
                // Handle Exception
                    Log.d(TAG, "Something went wrong .. " + e.localizedMessage)
                println(e.message)
            }

            val ref1 = Firebase.firestore.collection(RESTAURANT)
                .document(EASTYLIAN)

            batch.update(ref1, mapOf(
                TOTAL_ORDER_COUNT to FieldValue.increment(-1),
                TOTAL_SALES_AMOUNT to FieldValue.increment(-order.prices.total)
            ))

        }

        batch.update(Firebase.firestore.collection(USERS)
            .document(order.senderId)
            .collection(ORDERS)
            .document(order.orderId), mapOf(STATUS to listOf(CANCELLED)))

        batch.commit()
            .addOnCompleteListener {
                onComplete(it)
            }
    }

    fun checkout(activity: Activity, currentUser: User, order: Order) {
        val co = Checkout()
        co.setKeyID(BuildConfig.RAZORPAY_TEST_KEY)

        try {
            val options = getOptions(activity, currentUser, order)
            co.open(activity, options)
        } catch (e: Exception) {
            Toast.makeText(activity, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun getOptions(context: Context, currentUser: User, order: Order): JSONObject {
        val options = JSONObject()

        options.put(NAME, context.getString(R.string.app_name))
        options.put(DESCRIPTION, "Payment of ₹ " + (order.prices.total / 100).toString())

        // You can omit the image option to fetch the image from dashboard
        options.put(IMAGE, logoUrl)
        options.put(COLOR, "#ED7D31")
        options.put(CURRENCY, order.currency)
        options.put(ORDER_ID_ALT, order.razorpayOrderId)
        options.put(AMOUNT, order.prices.total)

        val retryObj = JSONObject()
        retryObj.put(ENABLED, true)
        retryObj.put(MAX_COUNT, 4)
        options.put(RETRY, retryObj)

        val prefill = JSONObject()
        prefill.put(EMAIL, currentUser.email)
        prefill.put(CONTACT, currentUser.phoneNo)
        options.put(PREFILL, prefill)

        return options
    }

    fun getRefundStatus(refund: Refund, onComplete: (refund: Refund?) -> Unit) {
        val newRefund = client.Refunds.fetch(refund.refundId)
        val newStatus = newRefund.get<String>(STATUS)
        if (newStatus != refund.status) {
            Firebase.firestore.collection(USERS)
                .document(refund.receiverId)
                .collection(REFUNDS)
                .document(refund.refundId)
                .update(mapOf(STATUS to newStatus))
                .addOnSuccessListener {
                    refund.status = newStatus
                    onComplete(refund)
                }.addOnFailureListener {
                    onComplete(null)
                    it.localizedMessage?.let { it1 ->
                        Log.e(TAG, it1)
                    }
                }
        }
    }

    private const val TAG = "RazorpayUtility"
}