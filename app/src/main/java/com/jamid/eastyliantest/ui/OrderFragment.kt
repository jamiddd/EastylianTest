package com.jamid.eastyliantest.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.jamid.eastyliantest.R

class OrderFragment : Fragment(R.layout.fragment_order) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    companion object {

        @JvmStatic
        fun newInstance() = OrderFragment()
    }
}