package dev.dworks.apps.anexplorer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Palette;
import dev.dworks.apps.anexplorer.misc.Utils;

public class LineColorPicker extends View {

	public interface OnColorChangedListener {
		void onColorChanged(int c);
	}

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	int[] colors;
    {
        if (isInEditMode()) {
            colors = Palette.DEFAULT;
        } else {
            colors = new int[1];
        }
    }

	private Paint paint;
	private Rect rect = new Rect();

	// indicate if nothing selected
	boolean isColorSelected = false;

	private int selectedColor = colors[0];
	private int selectedColorIndex = 0;

	private OnColorChangedListener onColorChanged;

	private int cellSize;

	private int mOrientation = HORIZONTAL;

	public LineColorPicker(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setStyle(Style.FILL);

		final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LineColorPicker, 0, 0);

		try {
			mOrientation = a.getInteger(R.styleable.LineColorPicker_orientation, HORIZONTAL);

            if (!isInEditMode()) {
                final int colorsArrayResId = a.getResourceId(R.styleable.LineColorPicker_colors, -1);

                if (colorsArrayResId > 0) {
                    final int[] colors = context.getResources().getIntArray(colorsArrayResId);
                    setColors(colors);
                }
            }

            final int selected = a.getInteger(R.styleable.LineColorPicker_selectedColorIndex, -1);

            if (selected != -1) {
                final int[] currentColors = getColors();

                final int currentColorsLength = currentColors != null ? currentColors.length : 0;

                if (selected < currentColorsLength) {
                    setSelectedColorPosition(selected);
                }
            }
		} finally {
			a.recycle();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mOrientation == HORIZONTAL) {
			drawHorizontalPicker(canvas);
		} else {
			drawVerticalPicker(canvas);
		}

	}

	private void drawVerticalPicker(Canvas canvas) {
		rect.left = 0;
		rect.top = 0;
		rect.right = canvas.getWidth();
		rect.bottom = 0;

		// 8%
		int margin = Math.round(canvas.getWidth() * 0.08f);

		for (int i = 0; i < colors.length; i++) {

			paint.setColor(colors[i]);

			rect.top = rect.bottom;
			rect.bottom += cellSize;

			if (isColorSelected && colors[i] == selectedColor) {
				rect.left = 0;
				rect.right = canvas.getWidth();
			} else {
				rect.left = margin;
				rect.right = canvas.getWidth() - margin;
			}

			canvas.drawRect(rect, paint);
		}

	}

	private void drawHorizontalPicker(Canvas canvas) {
		rect.left = 0;
		rect.top = 0;
		rect.right = 0;
		rect.bottom = canvas.getHeight();

		// 8%
		int margin = Math.round(canvas.getHeight() * 0.08f);

		for (int i = 0; i < colors.length; i++) {

			paint.setColor(colors[i]);

			rect.left = rect.right;
			rect.right += cellSize;

			if (isColorSelected && colors[i] == selectedColor) {
				rect.top = 0;
				rect.bottom = canvas.getHeight();
			} else {
				rect.top = margin;
				rect.bottom = canvas.getHeight() - margin;
			}

			canvas.drawRect(rect, paint);
		}
	}

	private void onColorChanged(int color) {
		if (onColorChanged != null) {
			onColorChanged.onColorChanged(color);
		}
	}

	private boolean isClick = false;
	private int screenW;
	private int screenH;

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int actionId = event.getAction();

		int newColor;

		switch (actionId) {
		case MotionEvent.ACTION_DOWN:
			isClick = true;
			break;
		case MotionEvent.ACTION_UP:
			newColor = getColorAtXY(event.getX(), event.getY());

			setSelectedColor(newColor);

			if (isClick) {
				performClick();
			}

			break;

		case MotionEvent.ACTION_MOVE:
			newColor = getColorAtXY(event.getX(), event.getY());

			setSelectedColor(newColor);

			break;
		case MotionEvent.ACTION_CANCEL:
			isClick = false;
			break;

		case MotionEvent.ACTION_OUTSIDE:
			isClick = false;
			break;

		default:
			break;
		}

		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		int increment = selectedColorIndex;
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_MINUS:
				increment = Utils.isRTL() ? increment + 1 : increment -1;
				if(increment < 0){
					return false;
				}
				setSelectedColorPosition(increment);
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_PLUS:
				increment =  Utils.isRTL() ? increment - 1  : increment + 1;
				if(increment >= colors.length){
					return false;
				}
				setSelectedColorPosition(increment);
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Return color at x,y coordinate of view.
	 */
	private int getColorAtXY(float x, float y) {

		// FIXME: colors.length == 0 -> devision by ZERO.s

		if (mOrientation == HORIZONTAL) {
			int left = 0;
			int right = 0;

			for (int i = 0; i < colors.length; i++) {
				left = right;
				right += cellSize;

				if (left <= x && right >= x) {
					return colors[i];
				}
			}

		} else {
			int top = 0;
			int bottom = 0;

			for (int i = 0; i < colors.length; i++) {
				top = bottom;
				bottom += cellSize;

				if (y >= top && y <= bottom) {
					return colors[i];
				}
			}
		}

		return selectedColor;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		// begin boilerplate code that allows parent classes to save state
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		// end

		ss.selectedColor = this.selectedColor;
		ss.selectedColorIndex = this.selectedColorIndex;
		ss.isColorSelected = this.isColorSelected;

		return ss;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		// begin boilerplate code so parent classes can restore state
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		// end

		this.selectedColor = ss.selectedColor;
		this.selectedColorIndex = ss.selectedColorIndex;
		this.isColorSelected = ss.isColorSelected;
	}

	static class SavedState extends BaseSavedState {
		int selectedColor;
		int selectedColorIndex;
		boolean isColorSelected;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			this.selectedColor = in.readInt();
			this.selectedColorIndex = in.readInt();
			this.isColorSelected = in.readInt() == 1;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(this.selectedColor);
			out.writeInt(this.selectedColorIndex);
			out.writeInt(this.isColorSelected ? 1 : 0);
		}

		// required field that makes Parcelables from a Parcel
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public boolean performClick() {
		return super.performClick();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {

		screenW = w;
		screenH = h;

		recalcCellSize();

		super.onSizeChanged(w, h, oldw, oldh);
	}

	// @Override
	// protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	// int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
	// int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
	// this.setMeasuredDimension(parentWidth, parentHeight);
	// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	// }

	/**
	 * Return currently selected color.
	 */
	public int getColor() {
		return selectedColor;
	}

	/**
	 * Set selected color as color value from palette.
	 */
	public void setSelectedColor(int color) {

		// not from current palette
		int index = containsColor(colors, color);
		if (index == -1) {
			return;
		}

		// do we need to re-draw view?
		if (!isColorSelected || selectedColor != color) {
			this.selectedColor = color;
			selectedColorIndex = index;
			isColorSelected = true;

			invalidate();

			onColorChanged(color);
		}
	}

	/**
	 * Set selected color as index from palete
	 */
	public void setSelectedColorPosition(int position) {
		selectedColorIndex = position;
		setSelectedColor(colors[selectedColorIndex]);
	}

	/**
	 * Set picker palette
	 */
	public void setColors(int[] colors) {
		// TODO: selected color can be NOT in set of colors
		// FIXME: colors can be null
		this.colors = colors;

		if (containsColor(colors, selectedColor) == - 1) {
			selectedColor = colors[0];
		}

		recalcCellSize();

		invalidate();
	}

	private int recalcCellSize() {

		if (mOrientation == HORIZONTAL) {
			cellSize = Math.round(screenW / (colors.length * 1f));
		} else {
			cellSize = Math.round(screenH / (colors.length * 1f));
		}

		return cellSize;
	}

	/**
	 * Return current picker palete
	 */
	public int[] getColors() {
		return colors;
	}


	/**
	 * Return position if palette contains this color or else - 1
	 */
	private int containsColor(int[] colors, int c) {
		for (int i = 0; i < colors.length; i++) {
			if (colors[i] == c)
				return i;

		}

		return -1;
	}

	/**
	 * Set onColorChanged listener
	 *
	 * @param l
	 */
	public void setOnColorChangedListener(OnColorChangedListener l) {
		this.onColorChanged = l;
	}
}