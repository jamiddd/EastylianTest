package com.jamid.eastyliantest.ui.notifications

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.eastyliantest.IS_ADMIN
import com.jamid.eastyliantest.NOTIFICATIONS
import com.jamid.eastyliantest.R
import com.jamid.eastyliantest.adapter.NotificationPagingAdapter
import com.jamid.eastyliantest.adapter.NotificationViewHolder
import com.jamid.eastyliantest.databinding.FragmentNotificationsBinding
import com.jamid.eastyliantest.model.SimpleNotification
import com.jamid.eastyliantest.ui.PagerListFragment
import com.jamid.eastyliantest.utility.slideRightNavOptions

@ExperimentalPagingApi
class NotificationsFragment : PagerListFragment<SimpleNotification, NotificationViewHolder, FragmentNotificationsBinding>() {

    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isAdmin = arguments?.getBoolean(IS_ADMIN) ?: false
        if (isAdmin) {
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.notifications_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.create_notification) {
            findNavController().navigate(R.id.action_notificationsFragment_to_addNotificationFragment, null, slideRightNavOptions())
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onViewLaidOut() {
        super.onViewLaidOut()
        initLayout(
            binding.notificationsRecycler
        )

        val query = Firebase.firestore.collection(NOTIFICATIONS)

        binding.notificationsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        getItems {
            viewModel.pagedNotificationsFlow(query)
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = NotificationsFragment()
    }

    override fun getViewBinding(): FragmentNotificationsBinding {
        return FragmentNotificationsBinding.inflate(layoutInflater)
    }

    override fun getAdapter(): PagingDataAdapter<SimpleNotification, NotificationViewHolder> {
        return NotificationPagingAdapter()
    }
}