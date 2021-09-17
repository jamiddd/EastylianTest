package com.jamid.eastyliantest.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.CakeClickListener
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.utility.convertDpToPx
import com.jamid.eastyliantest.utility.hide
import com.jamid.eastyliantest.utility.show
import com.jamid.eastyliantest.utility.updateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CakeAdapter : ListAdapter<Cake, CakeAdapter.CakeViewHolder>(
    cakeComparator) {

    var isAlreadyOrdered = false
    var isVertical = false
    var addedCakeList = listOf<String>()

    companion object {
        val cakeComparator = object: DiffUtil.ItemCallback<Cake>() {
            override fun areItemsTheSame(oldItem: Cake, newItem: Cake): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Cake, newItem: Cake): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class CakeViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val cakeClickListener: CakeClickListener = view.context as CakeClickListener

        fun bind(cake: Cake) {
            val primaryActionBtn = view.findViewById<Button>(R.id.primaryCakeAction)
            val cakeImage = view.findViewById<SimpleDraweeView>(R.id.cake_image)
            val cakeName = view.findViewById<TextView>(R.id.cakeName)
            val cakePrice = view.findViewById<TextView>(R.id.cakePrice)
            val favoriteBtn = view.findViewById<Button>(R.id.favoriteBtn)
            val cakeDesc = view.findViewById<TextView>(R.id.cakeDesc)
            val customizableText = view.findViewById<TextView>(R.id.customizableText)
            val animatedTick = view.findViewById<ImageView>(R.id.animatedTick)

            cakeName.text = cake.fancyName
            cakePrice.text = view.context.getString(R.string.currency_prefix) + (cake.price/100).toString()

            if (isAlreadyOrdered) {
                primaryActionBtn.text = view.context.getString(R.string.reorder)
            }

            cakeImage.setImageURI(cake.thumbnail)

            view.setOnClickListener {
                cakeClickListener.onCakeClick(cake)
            }

            if (addedCakeList.contains(cake.id)) {
                primaryActionBtn.text = view.context.getString(R.string.add_more)
            }

            primaryActionBtn.setOnClickListener {
                animatedTick.show()
                primaryActionBtn.text = view.context.getString(R.string.add_more)
                (animatedTick.drawable as Animatable).start()
                cakeClickListener.onCakeAddClick(cake)
                it.findViewTreeLifecycleOwner()?.lifecycle?.coroutineScope?.launch {
                    delay(1000)
                    animatedTick.hide()
                }
            }

            favoriteBtn.isSelected = cake.isFavorite

            favoriteBtn.setOnClickListener {
                favoriteBtn.isSelected = !favoriteBtn.isSelected
                cake.isFavorite = favoriteBtn.isSelected
                cakeClickListener.onCakeSetFavorite(cake)
            }

            if (isVertical) {
                view.updateLayout(ViewGroup.LayoutParams.WRAP_CONTENT)
            } else {
                view.updateLayout(height = view.context.convertDpToPx(400), width = view.context.convertDpToPx(270))
            }

            cakeDesc.text = cake.description

            if (cake.isCustomizable) {
                customizableText.text = view.context.getString(R.string.customizable)
            } else {
                customizableText.text = view.context.getString(R.string.non_customizable)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CakeViewHolder {
        return CakeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cake_item, parent, false))
    }

    override fun onBindViewHolder(holder: CakeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}