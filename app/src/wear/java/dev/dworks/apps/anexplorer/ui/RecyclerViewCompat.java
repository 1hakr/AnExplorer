package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.wear.widget.WearableRecyclerView;

public class RecyclerViewCompat extends WearableRecyclerView {
    public RecyclerViewCompat(Context context) {
        this(context, null);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public RecyclerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
        init();
    }

    private void init(){
        //setEdgeItemsCenteringEnabled(true);
        setCircularScrollingGestureEnabled(true);
        setBezelFraction(0.5f);
        setScrollDegreesPerScreen(90);
    }
}
