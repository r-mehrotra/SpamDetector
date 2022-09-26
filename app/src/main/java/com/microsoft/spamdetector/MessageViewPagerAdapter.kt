package com.microsoft.spamdetector

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MessageViewPagerAdapter(act: FragmentActivity) : FragmentStateAdapter(act) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SpamListFragment()
            else -> HamListFragment()
        }
    }
}