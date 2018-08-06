package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import dev.dworks.apps.anexplorer.R;

public class RecyclerViewPlus extends RecyclerView {
    public static final int TYPE_LIST = 0;
    public static final int TYPE_GRID = 1;
    public static final int TYPE_GALLERY = 2;

    private LayoutManager layoutManager;
    private int mType = TYPE_LIST;
    private int columnWidth = -1;
    private Context mContext;

    public RecyclerViewPlus(Context context) {
        this(context, null);
    }

    public RecyclerViewPlus(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewPlus(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RecyclerViewPlus,
                defStyle, 0);
        mType = attributes.getInt(R.styleable.RecyclerViewPlus_type, TYPE_LIST);
        columnWidth = attributes.getDimensionPixelSize(R.styleable.RecyclerViewPlus_columnWidth, -1);
        attributes.recycle();
        setType(mType);
    }

    public void setType(int type){
        mType = type;
        updateLayoutManager();
    }

    private void updateLayoutManager(){
        switch (mType){
            case TYPE_LIST:
                layoutManager =
                        new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
                break;
            case TYPE_GRID:
                layoutManager = new GridLayoutManager(mContext, 1);
                break;
            case TYPE_GALLERY:
                layoutManager =
                        new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                break;
            default:
                layoutManager =
                        new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
                break;
        }

        setLayoutManager(layoutManager);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        switch (mType){
            case TYPE_GRID:
                if (columnWidth > 0) {
                    //The spanCount will always be at least 1
                    int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                    ((GridLayoutManager)layoutManager).setSpanCount(spanCount);
                }
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }
}