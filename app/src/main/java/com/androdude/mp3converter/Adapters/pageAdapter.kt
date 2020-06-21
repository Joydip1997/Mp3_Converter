package com.androdude.mp3converter.Adapters


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter




class pageAdapter(val fm: FragmentManager, val fragmentList : ArrayList<Fragment>) :FragmentStatePagerAdapter(fm){

    override fun getItem(position: Int): Fragment {
        return fragmentList.get(position)
    }

    override fun getCount(): Int {
        return fragmentList.size
    }


}