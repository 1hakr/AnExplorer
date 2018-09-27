package androidx.wear.widget.drawer;

public class WearableDrawerControllerCompat extends WearableDrawerController {

    private final WearableDrawerLayout mDrawerLayout;
    private final WearableDrawerView mDrawerView;

    public WearableDrawerControllerCompat(WearableDrawerLayout drawerLayout, WearableDrawerView drawerView) {
        super(drawerLayout, drawerView);
        mDrawerLayout = drawerLayout;
        mDrawerView = drawerView;
    }

    /**
     * Requests that the {@link WearableDrawerView} be opened.
     */
    public void openDrawer() {
        mDrawerLayout.openDrawer(mDrawerLayout.mTopDrawerView);
    }

    /**
     * Requests that the {@link WearableDrawerView} be closed.
     */
    public void closeDrawer() {
        mDrawerLayout.closeDrawer(mDrawerLayout.mTopDrawerView);
    }

    /**
     * Requests that the {@link WearableDrawerView} be peeked.
     */
    public void peekDrawer() {
        mDrawerLayout.peekDrawer(mDrawerLayout.mTopDrawerView);
    }
}
