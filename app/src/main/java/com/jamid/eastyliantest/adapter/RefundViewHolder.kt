package com.jamid.eastyliantest.adapter

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.USERS
import com.jamid.eastyliantest.interfaces.RefundClickListener
import com.jamid.eastyliantest.model.Refund
import com.jamid.eastyliantest.model.User
import com.jamid.eastyliantest.utility.toast

private const val processed = "processed"

class RefundViewHolder(val view: View): RecyclerView.ViewHolder(view) {

	private val refundClickListener = view.context as RefundClickListener

	fun bind(refund: Refund?) {
		if (refund != null) {
			val orderIdText = view.findViewById<TextView>(R.id.orderIdText)
			val refundAmount = view.findViewById<TextView>(R.id.refundAmount)
			val refundStatus = view.findViewById<TextView>(R.id.refundStatus)
			val updateStatusBtn = view.findViewById<Button>(R.id.updateStatusBtn)

			orderIdText.text = refund.orderId
			val amountText = "₹ ${(refund.amount)/100}"
			refundAmount.text = amountText

			val (color, status) = if (refund.status == processed) {
				ContextCompat.getColor(view.context, R.color.greenDarkTextColor) to view.context.getString(R.string.processed)
			} else {
				ContextCompat.getColor(view.context, R.color.darkRedTextColor) to view.context.getString(R.string.require_confirmation)
			}

			refundStatus.text = status
			refundStatus.setTextColor(color)
			refundAmount.setTextColor(color)

			Firebase.firestore.collection(USERS).document(refund.receiverId).get()
				.addOnSuccessListener {
					if (it.exists()) {
						val user = it.toObject(User::class.java)!!

						updateStatusBtn.setOnClickListener {
							refundClickListener.onUpdateBtnClick(refund, user)
						}
					}
				}.addOnFailureListener {
					view.context.toast("Something went wrong while trying to get user data. ${it.localizedMessage}")
				}

		}
	}
}