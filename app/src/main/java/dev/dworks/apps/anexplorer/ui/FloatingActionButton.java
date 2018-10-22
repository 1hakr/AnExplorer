package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import dev.dworks.apps.anexplorer.R;


public class FloatingActionButton extends com.google.android.material.floatingactionbutton.FloatingActionButton {
    private static final int TRANSLATE_DURATION_MILLIS = 200;
    private boolean mVisible;
    private int mScrollThreshold;
    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mVisible = true;
        mScrollThreshold = getResources().getDimensionPixelOffset(R.dimen.scroll_threshold);
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