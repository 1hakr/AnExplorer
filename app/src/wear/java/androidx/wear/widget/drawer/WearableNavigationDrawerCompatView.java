/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.widget.drawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.concurrent.TimeUnit;

public class WearableNavigationDrawerCompatView extends androidx.wear.widget.drawer.WearableNavigationDrawerView {

    private static final long AUTO_CLOSE_DRAWER_DELAY_MS = TimeUnit.SECONDS.toMillis(5);
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCloseDrawerRunnable =
            new Runnable() {
                @Override
                public void run() {
                    getController().closeDrawer();
                }
            };
    private boolean mIsAccessibilityEnabled;

    private final GestureDetector mGestureDetector;
    private final GestureDetector.SimpleOnGestureListener mOnGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @SuppressLint("RestrictedApi")
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    getController().closeDrawer();
                    return mPresenter.onDrawerTapped();
                }
            };

    public WearableNavigationDrawerCompatView(Context context) {
        this(context, (AttributeSet) null);
    }
    public WearableNavigationDrawerCompatView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableNavigationDrawerCompatView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearableNavigationDrawerCompatView(Context context, AttributeSet attrs, int defStyleAttr,
                                              int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mGestureDetector = new GestureDetector(getContext(), mOnGestureListener);
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mIsAccessibilityEnabled = accessibilityManager.isEnabled();

    }

    @Override
    public void setDrawerController(WearableDrawerController controller) {
        super.setDrawerController(controller);
        autoCloseDrawerAfterDelay();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        autoCloseDrawerAfterDelay();
        return mGestureDetector != null && mGestureDetector.onTouchEvent(ev);
    }

    private void autoCloseDrawerAfterDelay() {
        try {
            if (!mIsAccessibilityEnabled) {
                mMainThreadHandler.removeCallbacks(mCloseDrawerRunnable);
                mMainThreadHandler.postDelayed(mCloseDrawerRunnable, AUTO_CLOSE_DRAWER_DELAY_MS);
            }
        } catch (Exception e) {}
    }
}
