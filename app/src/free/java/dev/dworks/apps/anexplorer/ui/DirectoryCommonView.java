package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class DirectoryCommonView extends FrameLayout {
    public DirectoryCommonView(Context context) {
        this(context, null);
    }

    public DirectoryCommonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DirectoryCommonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
