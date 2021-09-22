package com.jamid.eastyliantest.interfaces

import com.jamid.eastyliantest.model.Cake
import com.jamid.eastyliantest.model.CakeMenuItem

interface CakeClickListener {
    fun onCakeClick(cake: Cake)
    fun onCakeAddClick(cake: Cake)
    fun onCakeSetFavorite(cake: Cake)

    fun onBaseCakeClick(cakeMenuItem: CakeMenuItem)
    fun onBaseCakeLongClick(cakeMenuItem: CakeMenuItem)
}