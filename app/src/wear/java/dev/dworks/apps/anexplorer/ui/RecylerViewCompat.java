package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.wear.widget.WearableRecyclerView;
import android.util.AttributeSet;

public class RecylerViewCompat extends WearableRecyclerView {
    public RecylerViewCompat(Context context) {
        super(context);
    }

    public RecylerViewCompat(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecylerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public RecylerViewCompat(Context context, @Nullable AttributeSet attrs, int defStyle, int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
    }

    private void init(){
        setEdgeItemsCenteringEnabled(true);
        setCircularScrollingGestureEnabled(true);
        setBezelFraction(0.5f);
        setScrollDegreesPerScreen(90);
    }
}
