package dev.dworks.apps.anexplorer.ui;

import android.view.View;

import androidx.wear.widget.drawer.WearableCompatDrawerLayout;
import androidx.wear.widget.drawer.WearableDrawerView;

public class DrawerLayoutHelper {

    WearableCompatDrawerLayout mWearableDrawerLayout;

    public DrawerLayoutHelper(View view){
        if(view instanceof WearableCompatDrawerLayout) {
            mWearableDrawerLayout = (WearableCompatDrawerLayout) view;
        }
    }

    public boolean isDrawerOpen(View view){
        return false;
    }

    public void setDrawerLockMode(int lockMode) {

    }

    public void setDrawerLockMode(int lockModeUnlocked, View mInfoContainer) {

    }

    public void openDrawer(View drawer) {
        if(drawer instanceof WearableDrawerView) {
            mWearableDrawerLayout.openDrawer((WearableDrawerView) drawer);
        }
    }

    public void closeDrawer(View drawer) {
        if(drawer instanceof WearableDrawerView) {
            mWearableDrawerLayout.closeDrawer((WearableDrawerView) drawer);
        }
    }
}
