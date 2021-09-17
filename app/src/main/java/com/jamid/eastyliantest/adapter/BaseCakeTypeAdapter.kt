package com.jamid.eastyliantest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.interfaces.CakeClickListener
import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.Flavor

class BaseCakeTypeAdapter(private val cakes: List<Cake>): RecyclerView.Adapter<BaseCakeTypeAdapter.CakeViewHolder>() {

    inner class CakeViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        private val cakeClickListener: CakeClickListener = view.context as CakeClickListener

        fun bind(cake: Cake) {
            val cakeNameText = view.findViewById<TextView>(R.id.cakeTypeText)
            val imageView = view.findViewById<ImageView>(R.id.cakeTypeImage)

            if (cake.flavors.size > 1) {
                imageView.apply {
                    when (cake.flavors[1]) {
                        Flavor.BLACK_FOREST -> {
                            setImageResource(R.drawable.black_forest)
                            cakeNameText.text = view.context.getString(R.string.black_forest)
                        }
                        Flavor.WHITE_FOREST -> {
                            setImageResource(R.drawable.white_forest)
                            cakeNameText.text = view.context.getString(R.string.white_forest)
                        }
                        Flavor.VANILLA -> {
                            setImageResource(R.drawable.vanilla)
                            cakeNameText.text = view.context.getString(R.string.vanilla)
                        }
                        Flavor.CHOCOLATE_FANTASY -> {
                            setImageResource(R.drawable.chocolate_fantasy)
                            cakeNameText.text = view.context.getString(R.string.chocolate_fantasy)
                        }
                        Flavor.RED_VELVET -> {
                            setImageResource(R.drawable.red_velvet)
                            cakeNameText.text = view.context.getString(R.string.red_velvet)
                        }
                        Flavor.HAZELNUT -> {
                            setImageResource(R.drawable.hazelnut)
                            cakeNameText.text = view.context.getString(R.string.hazelnut)
                        }
                        Flavor.MANGO -> {
                            setImageResource(R.drawable.mango)
                            cakeNameText.text = view.context.getString(R.string.mango)
                        }
                        Flavor.STRAWBERRY -> {
                            setImageResource(R.drawable.strawberry)
                            cakeNameText.text = view.context.getString(R.string.strawberry)
                        }
                        Flavor.KIWI -> {
                            setImageResource(R.drawable.kiwi)
                            cakeNameText.text = view.context.getString(R.string.kiwi)
                        }
                        Flavor.ORANGE -> {
                            setImageResource(R.drawable.orange)
                            cakeNameText.text = view.context.getString(R.string.orange)
                        }
                        Flavor.PINEAPPLE -> {
                            setImageResource(R.drawable.pineapple)
                            cakeNameText.text = view.context.getString(R.string.pineapple)
                        }
                        Flavor.BUTTERSCOTCH -> {
                            setImageResource(R.drawable.butterscotch)
                            cakeNameText.text = view.context.getString(R.string.butterscotch)
                        }
                        Flavor.NONE -> {
                            if (cake.baseName.isNotBlank()) {
                                cakeNameText.text = cake.baseName
                                val image = if (cake.baseName == view.context.getString(R.string.fondant)) {
                                    R.drawable.fondant
                                } else {
                                    R.drawable.sponge
                                }
                                imageView.setImageResource(image)
                            }
                        }
                    }
                }
            } else {
                cakeNameText.text = cake.baseName
                val image = if (cake.baseName == view.context.getString(R.string.fondant)) {
                    R.drawable.fondant
                } else {
                    R.drawable.sponge
                }
                imageView.setImageResource(image)
            }

            view.setOnClickListener {
                cakeClickListener.onCakeClick(cake)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CakeViewHolder {
        return CakeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.base_cake_item, parent, false))
    }

    override fun onBindViewHolder(holder: CakeViewHolder, position: Int) {
        holder.bind(cakes[position])
    }

    override fun getItemCount(): Int {
        return cakes.size
    }

}