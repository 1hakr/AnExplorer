package dev.dworks.apps.anexplorer.ui;

import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;

public class DrawerLayoutHelper {

    DrawerLayout mDrawerLayout;

    public DrawerLayoutHelper(View view){
        mDrawerLayout = (DrawerLayout) view;
    }

    public boolean isDrawerOpen(View view){
        if(null == mDrawerLayout){
            return false;
        }
        return mDrawerLayout.isDrawerOpen(view);
    }

    public void setDrawerLockMode(int lockMode) {
        if(null == mDrawerLayout){
            return;
        }
        mDrawerLayout.setDrawerLockMode(lockMode);
    }

    public void setDrawerLockMode(int lockModeUnlocked, View mInfoContainer) {
        if(null == mDrawerLayout){
            return;
        }
        mDrawerLayout.setDrawerLockMode(lockModeUnlocked, mInfoContainer);
    }

    public void openDrawer(View drawer) {
        if(null == mDrawerLayout || null == drawer){
            return;
        }
        mDrawerLayout.openDrawer(drawer);
    }

    public void closeDrawer(View drawer) {
        if(null == mDrawerLayout || null == drawer){
            return;
        }
        mDrawerLayout.closeDrawer(drawer);
    }
}
