package androidx.wear.widget.drawer;

import android.content.Context;
import android.util.AttributeSet;

import androidx.wear.widget.drawer.WearableDrawerLayout;

public class WearableCompatDrawerLayout extends WearableDrawerLayout {
    public WearableCompatDrawerLayout(Context context) {
        super(context);
    }

    public WearableCompatDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WearableCompatDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void openDrawer(int gravity) {
        super.openDrawer(gravity);
    }

    public void closeDrawer(int gravity) {
        super.closeDrawer(gravity);
    }

    @Override
    public void openDrawer(WearableDrawerView drawer) {
        super.openDrawer(drawer);
    }

    @Override
    public void closeDrawer(WearableDrawerView drawer) {
        mTopDrawerView.getController();
        super.closeDrawer(drawer);
    }
}
