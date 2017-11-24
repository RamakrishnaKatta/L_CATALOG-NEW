package com.immerrsionslabs.l_catalog_new.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.immerrsionslabs.l_catalog_new.Fragment_Illustration;
import com.immerrsionslabs.l_catalog_new.Fragment_Overview;

public class MainPageAdapter  extends FragmentStatePagerAdapter {
    private int mNumOfTabs;

    public MainPageAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new Fragment_Illustration();
            case 1:
                return new Fragment_Overview();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}