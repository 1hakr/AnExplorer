package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.fab.AbsListViewScrollDetector;
import dev.dworks.apps.anexplorer.fab.AddFloatingActionButton;
import dev.dworks.apps.anexplorer.fab.ScrollDirectionListener;
import dev.dworks.apps.anexplorer.misc.Utils;


public class FloatingActionsMenu extends dev.dworks.apps.anexplorer.fab.FloatingActionsMenu {
    private static final int TRANSLATE_DURATION_MILLIS = 200;
    private boolean mVisible;
    private int mScrollThreshold;
    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public FloatingActionsMenu(Context context) {
        this(context, null);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mVisible = true;
        mScrollThreshold = getResources().getDimensionPixelOffset(R.dimen.scroll_threshold);
    }

    public void setColorNormalResId(@ColorRes int colorNormal) {
        setColorNormal(getColor(colorNormal));
    }

    public void setColorNormal(int color) {
        getAddButton().setColorNormal(color);
    }

    public void setColorPressedResId(@ColorRes int colorPressed) {
        setColorPressed(getColor(colorPressed));
    }

    public void setColorPressed(int color) {
        getAddButton().setColorPressed(color);
    }

    int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    public AddFloatingActionButton getAddButton() {
        return mAddButton;
    }
    public boolean isVisible() {
        return mVisible;
    }

    public void show() {
        show(true);
    }

    public void hide() {
        hide(true);
    }

    public void show(boolean animate) {
        toggle(true, animate, false);
    }

    public void hide(boolean animate) {
        toggle(false, animate, false);
    }

    private void toggle(final boolean visible, final boolean animate, boolean force) {
        if (mVisible != visible || force) {
            mVisible = visible;
            int height = getHeight();
            if (height == 0 && !force) {
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ViewTreeObserver currentVto = getViewTreeObserver();
                            if (currentVto.isAlive()) {
                                currentVto.removeOnPreDrawListener(this);
                            }
                            toggle(visible, animate, true);
                            return true;
                        }
                    });
                    return;
                }
            }
            int translationY = visible ? 0 : height + getMarginBottom();
            if (animate) {
                animate().setInterpolator(mInterpolator)
                    .setDuration(TRANSLATE_DURATION_MILLIS)
                    .translationY(translationY);
            } else {
                setTranslationY(translationY);
            }
            // On pre-Honeycomb a translated view is still clickable, so we need to disable clicks manually
            if (!Utils.hasHoneycomb()) {
                setClickable(visible);
            }
        }

        if(isExpanded()) {
            collapse();
        }
    }

    private int getMarginBottom() {
        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
    }

    public void attachToListView(@NonNull AbsListView listView) {
        attachToListView(listView, null);
    }

    public void attachToListView(@NonNull AbsListView listView, ScrollDirectionListener listener) {
        AbsListViewScrollDetectorImpl scrollDetector = new AbsListViewScrollDetectorImpl();
        scrollDetector.setListener(listener);
        scrollDetector.setListView(listView);
        scrollDetector.setScrollThreshold(mScrollThreshold);
        listView.setOnScrollListener(scrollDetector);
    }

    private class AbsListViewScrollDetectorImpl extends AbsListViewScrollDetector {
        private ScrollDirectionListener mListener;

        private void setListener(ScrollDirectionListener scrollDirectionListener) {
            mListener = scrollDirectionListener;
        }

        @Override
        public void onScrollDown() {
            show();
            if (mListener != null) {
                mListener.onScrollDown();
            }
        }

        @Override
        public void onScrollUp() {
            hide();
            if (mListener != null) {
                mListener.onScrollUp();
            }
        }
    }
}