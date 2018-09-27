package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.wear.widget.WearableRecyclerView;

public class RecyclerViewCompat extends WearableRecyclerView {
    public RecyclerViewCompat(Context context) {
        super(context);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
    }

    private void init(){
        setEdgeItemsCenteringEnabled(true);
        setCircularScrollingGestureEnabled(true);
        setBezelFraction(0.5f);
        setScrollDegreesPerScreen(90);
    }
}
