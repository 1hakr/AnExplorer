package dev.dworks.apps.anexplorer.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;

import androidx.wear.widget.SwipeDismissFrameLayout;

public class DirectoryCommonView extends SwipeDismissFrameLayout {
    public DirectoryCommonView(Context context) {
        this(context, null);
    }

    public DirectoryCommonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DirectoryCommonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Activity activity = (Activity) context;
        addCallback(new Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                super.onDismissed(layout);
                activity.onBackPressed();
            }
        });
    }
}
