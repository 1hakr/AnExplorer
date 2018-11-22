package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;
import dev.dworks.apps.anexplorer.R;

/**
 * Defines a custom EditText View that draws lines between each line of text
 * that is displayed.
 */
public class LinedEditText extends AppCompatEditText {
	private Rect mRect;
	private Paint mPaint;

	public LinedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		mRect = new Rect();
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(context.getResources().getColor(R.color.statusBarColor));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int count = getLineCount();
		Rect r = mRect;
		Paint paint = mPaint;

		for (int i = 0; i < count; i++) {
			int baseline = getLineBounds(i, r);
			canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
		}

		super.onDraw(canvas);
	}
}