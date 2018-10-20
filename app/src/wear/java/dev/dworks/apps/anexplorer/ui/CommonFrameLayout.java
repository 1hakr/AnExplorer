package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.widget.BoxInsetLayout;

public class CommonFrameLayout extends BoxInsetLayout {
    public CommonFrameLayout(@NonNull Context context) {
        super(context);
    }

    public CommonFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CommonFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
