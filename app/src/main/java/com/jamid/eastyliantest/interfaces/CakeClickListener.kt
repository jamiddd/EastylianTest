package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.Cake

interface CakeClickListener {
    fun onCakeClick(cake: Cake)
    fun onCakeAddClick(cake: Cake)
    fun onCakeSetFavorite(cake: Cake)
}