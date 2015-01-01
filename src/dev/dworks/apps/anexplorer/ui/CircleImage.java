package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import dev.dworks.apps.anexplorer.R;

public class CircleImage extends View {

	private int centerY;
	private int centerX;
	private int outerRadius;
	private Paint circlePaint;
	private int defaultColor = Color.GRAY;

	public CircleImage(Context context) {
		super(context);
		init(context, null);
	}

	public CircleImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public CircleImage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawCircle(centerX, centerY, outerRadius , circlePaint);
		super.onDraw(canvas);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		centerX = w / 2;
		centerY = h / 2;
		outerRadius = Math.min(w, h) / 2;
	}

	public void setColor(int color) {
		this.defaultColor = color;
		circlePaint.setColor(defaultColor);

		this.invalidate();
	}

	private void init(Context context, AttributeSet attrs) {
		//this.setScaleType(ScaleType.CENTER_INSIDE);

		circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setStyle(Paint.Style.FILL);

		int color = defaultColor;
		if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.View);
            color = a.getColor(R.styleable.View_background, color);
			a.recycle();
		}

		setColor(color);
	}

    @Override
    public void setBackgroundColor(int color) {
        setColor(color);
    }
}