package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class RecylerViewCompat extends RecyclerView {
    public RecylerViewCompat(Context context) {
        super(context);
    }

    public RecylerViewCompat(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecylerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
