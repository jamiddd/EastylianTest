package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.CakeClickListener
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.CakeMenuItem
import com.jamid.eastyliantest.utility.getFlavorFromName

class BaseCakeTypeAdapter: ListAdapter<CakeMenuItem, BaseCakeTypeAdapter.CakeViewHolder>(
    cakeMenuItemComparator) {

    var isAdmin = false

    companion object {
        private val cakeMenuItemComparator = object : DiffUtil.ItemCallback<CakeMenuItem>() {
            override fun areItemsTheSame(oldItem: CakeMenuItem, newItem: CakeMenuItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: CakeMenuItem, newItem: CakeMenuItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class CakeViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        var isAdmin = false

        private val cakeClickListener: CakeClickListener = view.context as CakeClickListener
        private val cakeNameText = view.findViewById<TextView>(R.id.cakeTypeText)
        private val imageView = view.findViewById<SimpleDraweeView>(R.id.cakeTypeImage)

        fun bind(cakeMenuItem: CakeMenuItem) {

            cakeNameText.text = cakeMenuItem.title
            imageView.setImageURI(cakeMenuItem.image)

            view.setOnClickListener {
                if (isAdmin) {
                    cakeClickListener.onBaseCakeClick(cakeMenuItem)
                } else {
                    val cake = if (cakeMenuItem.category == "flavor") {
                        Cake.newFlavorInstance(listOf(view.context.getFlavorFromName(cakeMenuItem.title)))
                    } else {
                        Cake.newInstance(cakeMenuItem.title)
                    }
                    cake.isCustomizable = true
                    cake.thumbnail = cakeMenuItem.image
                    cakeClickListener.onCakeClick(cake)
                }
            }

            view.setOnLongClickListener {
                if (isAdmin) {
                    cakeClickListener.onBaseCakeLongClick(cakeMenuItem)
                }
                true
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CakeViewHolder {
        val vh = CakeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.base_cake_item, parent, false))
        vh.isAdmin = isAdmin
        return vh
    }

    override fun onBindViewHolder(holder: CakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}