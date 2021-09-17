package com.jamid.eastyliantest.adapter

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.model.Refund
import com.jamid.eastyliantest.utility.RazorpayUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val processed = "processed"

class RefundViewHolder(val view: View): RecyclerView.ViewHolder(view) {
	fun bind(refund: Refund?) {
		if (refund != null) {
			val orderIdText = view.findViewById<TextView>(R.id.orderIdText)
			val refundAmount = view.findViewById<TextView>(R.id.refundAmount)
			val refundStatus = view.findViewById<TextView>(R.id.refundStatus)

			orderIdText.text = refund.orderId
			val amountText = "₹ ${(refund.amount)/100}"
			refundAmount.text = amountText

			val (color, status) = if (refund.status == processed) {
				ContextCompat.getColor(view.context, R.color.greenDarkTextColor) to view.context.getString(R.string.processed)
			} else {
				view.findViewTreeLifecycleOwner()?.lifecycleScope?.launch (Dispatchers.IO) {
					RazorpayUtility.getRefundStatus(refund) {
						bind(it)
					}
				}
				ContextCompat.getColor(view.context, R.color.darkRedTextColor) to view.context.getString(R.string.require_confirmation)
			}

			refundStatus.text = status
			refundStatus.setTextColor(color)
			refundAmount.setTextColor(color)
		}
	}
}