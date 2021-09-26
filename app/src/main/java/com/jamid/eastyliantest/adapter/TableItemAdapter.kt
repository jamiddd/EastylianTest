package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.jamid.eastyliantest.R

interface TableItemClickListener {
	fun onPrimaryActionClick(item: String)
}

class TableItemAdapter(val tableItemClickListener: TableItemClickListener): ListAdapter<String, TableItemAdapter.TableItemViewHolder>(stringComparator) {

	var isAdmin = false
	var icons = mutableListOf<String>()

	companion object {
		val stringComparator = object: DiffUtil.ItemCallback<String>() {
			override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
				return oldItem == newItem
			}

			override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
				return oldItem.length == newItem.length
			}
		}
	}

	inner class TableItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

		var isAdmin = false

		private val contactImage = view.findViewById<SimpleDraweeView>(R.id.contactImage)
		private val tableItem = view.findViewById<TextView>(R.id.tableItemText)
		private val actionBtn = view.findViewById<MaterialButton>(R.id.actionBtn)

		fun bind(item: String) {
			tableItem.text = item

			if (icons.isNotEmpty()) {
				contactImage.setImageURI(icons.random())
			}

			actionBtn.setOnClickListener {
				tableItemClickListener.onPrimaryActionClick(item)
			}

			if (!isAdmin) {
				if (item.contains(".")) {
					actionBtn.text = "Compose"
					actionBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_email_24)
				} else {
					actionBtn.text = "Call"
					actionBtn.icon = ContextCompat.getDrawable(view.context, R.drawable.ic_round_call_24)
				}
			} else {
				view.setOnClickListener {
					// Not implemented
				}
			}


		}

	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableItemViewHolder {
		val vh = TableItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.simple_table_item_with_action, parent, false))
		vh.isAdmin = isAdmin
		return vh
	}

	override fun onBindViewHolder(holder: TableItemViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

}