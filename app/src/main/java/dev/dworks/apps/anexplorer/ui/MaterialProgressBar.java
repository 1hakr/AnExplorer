package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import dev.dworks.apps.anexplorer.R;

public class MaterialProgressBar extends ProgressBar {
    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    private MaterialProgressDrawable indeterminateDrawable;

    public MaterialProgressBar(Context context) {
        this(context, null);
    }

    public MaterialProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (isInEditMode()) {
            setIndeterminateDrawable(new MaterialProgressDrawable(getContext(), this));
            return;
        }

        Resources res = context.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialProgressBar, defStyle, 0);

        final int color = a.getColor(R.styleable.MaterialProgressBar_color, res.getColor(R.color.accentColor));
        final float strokeWidth = a.getDimension(R.styleable.MaterialProgressBar_stroke_width, res.getDimension(R.dimen.default_stroke_width));
        final float sweepSpeed = a.getFloat(R.styleable.MaterialProgressBar_sweep_speed, Float.parseFloat(res.getString(R.string.default_sweep_speed)));
        final float rotationSpeed = a.getFloat(R.styleable.MaterialProgressBar_rotation_speed, Float.parseFloat(res.getString(R.string.default_rotation_speed)));
        final int minSweepAngle = a.getInteger(R.styleable.MaterialProgressBar_min_sweep_angle, res.getInteger(R.integer.default_min_sweep_angle));
        final int maxSweepAngle = a.getInteger(R.styleable.MaterialProgressBar_max_sweep_angle, res.getInteger(R.integer.default_max_sweep_angle));
        a.recycle();

        indeterminateDrawable = new MaterialProgressDrawable(getContext(), this);
        indeterminateDrawable.setBackgroundColor(CIRCLE_BG_LIGHT);
        indeterminateDrawable.setAlpha(255);
        indeterminateDrawable.updateSizes(MaterialProgressDrawable.LARGE);
        setColor(color);
    }

    public void setColor(int color){
        indeterminateDrawable.stop();
        indeterminateDrawable.setColorSchemeColors(color);
        indeterminateDrawable.start();
        setIndeterminateDrawable(indeterminateDrawable);
    }
}