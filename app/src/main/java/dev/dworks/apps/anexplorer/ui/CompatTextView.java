package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;

public class CompatTextView extends AppCompatTextView {
	public CompatTextView(Context context) {
		super(context);
		init(null);
	}

	public CompatTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public CompatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	private void init(@Nullable AttributeSet attrs) {
		if (attrs != null) {
			Context context = getContext();
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CompatTextView);

			// Obtain DrawableManager used to pull Drawables safely, and check if we're in RTL
			AppCompatDrawableManager dm = AppCompatDrawableManager.get();

			// Grab the compat drawable resources from the XML
			int startDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableStart, 0);
			int topDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableTop, 0);
			int endDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableEnd, 0);
			int bottomDrawableRes = a.getResourceId(R.styleable.CompatTextView_drawableBottom, 0);

			// Load the used drawables, falling back to whatever may be set in an "android:" namespace attribute
			Drawable[] currentDrawables = getCompoundDrawables();
			Drawable left = startDrawableRes != 0 ? dm.getDrawable(context, startDrawableRes) : currentDrawables[0];
			Drawable right = endDrawableRes != 0 ? dm.getDrawable(context, endDrawableRes) : currentDrawables[1];
			Drawable top = topDrawableRes != 0 ? dm.getDrawable(context, topDrawableRes) : currentDrawables[2];
			Drawable bottom = bottomDrawableRes != 0 ? dm.getDrawable(context, bottomDrawableRes) : currentDrawables[3];

			// Account for RTL and apply the compound Drawables
			Drawable start = isLayoutRtl() ? right : left;
			Drawable end = isLayoutRtl() ? left : right;
			setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);

			a.recycle();
		}
	}

	public boolean isLayoutRtl() {
		return Utils.isRTL();
	}
}