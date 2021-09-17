package com.jamid.eastyliantest.ui.notifications

import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingDataAdapter
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
import com.jamid.eastyliantest.utility.updateLayout

@ExperimentalPagingApi
class NotificationsFragment : PagerListFragment<SimpleNotification, NotificationViewHolder, FragmentNotificationsBinding>() {

    override fun onViewLaidOut() {
        super.onViewLaidOut()
        val isAdmin = arguments?.getBoolean(IS_ADMIN) ?: false

        initLayout(
            binding.notificationsRecycler,
            binding.noNotificationsText,
            binding.notificationProgress
        )

        val query = Firebase.firestore.collection(NOTIFICATIONS)

        getItems {
            viewModel.pagedNotificationsFlow(query)
        }

        binding.fragmentNotificationsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.fragmentNotificationsToolbar.updateLayout(marginTop = top)
        }

        binding.addNotificationBtn.setOnClickListener {
            findNavController().navigate(R.id.action_notificationsFragment_to_addNotificationFragment, null, slideRightNavOptions())
        }


        if (isAdmin) {
            binding.addNotificationBtn.show()
        } else {
            binding.addNotificationBtn.hide()
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