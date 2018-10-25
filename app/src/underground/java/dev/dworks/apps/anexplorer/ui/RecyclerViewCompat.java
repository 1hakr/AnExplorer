package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewCompat extends RecyclerView {
    public RecyclerViewCompat(Context context) {
        super(context);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
