package dev.dworks.apps.anexplorer.ui.fabs;

import android.support.design.internal.NavigationMenu;
import android.view.MenuItem;

/**
 * This adapter class provides empty implementations of the methods from
 * {@link FabSpeedDial.MenuListener}.
 * Created by yavorivanov on 03/01/2016.
 */
public class SimpleMenuListenerAdapter implements FabSpeedDial.MenuListener {

    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {

        return true;
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public void onMenuClosed() {
    }
}
