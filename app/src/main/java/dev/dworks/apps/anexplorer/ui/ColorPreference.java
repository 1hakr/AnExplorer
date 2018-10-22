/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.anexplorer.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.misc.Utils;


/**
 * A preference that allows the user to choose an application or shortcut.
 */
public class ColorPreference extends Preference {
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.grid_item_color;
    private int mNumColumns = 5;
    private View mPreviewView;

    public ColorPreference(Context context) {
        super(context);
        initAttrs(null, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs, defStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColorPreference, defStyle, defStyle);

        try {
            mItemLayoutId = a.getResourceId(R.styleable.ColorPreference_itemLayout, mItemLayoutId);
            mNumColumns = a.getInteger(R.styleable.ColorPreference_numColumns, mNumColumns);
            int choicesResId = a.getResourceId(R.styleable.ColorPreference_choices,
                    R.array.default_color_choice_values);
            if (choicesResId > 0) {
                String[] choices = a.getResources().getStringArray(choicesResId);
                mColorChoices = new int[choices.length];
                for (int i = 0; i < choices.length; i++) {
                    mColorChoices[i] = Color.parseColor(choices[i]);
                }
            }

        } finally {
            a.recycle();
        }

        setWidgetLayoutResource(mItemLayoutId);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mPreviewView = view.findViewById(R.id.color_view);
        setColorViewValue(mPreviewView, mValue, false);
    }

    public void setValue(int value) {
        if (callChangeListener(value)) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected void onClick() {
        super.onClick();

        ColorDialogFragment fragment = ColorDialogFragment.newInstance();
        fragment.setPreference(this);

        Activity activity = (Activity) getContext();
        activity.getFragmentManager().beginTransaction()
                .add(fragment, getFragmentTag())
                .commit();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        Activity activity = (Activity) getContext();
        ColorDialogFragment fragment = (ColorDialogFragment) activity
                .getFragmentManager().findFragmentByTag(getFragmentTag());
        if (fragment != null) {
            // re-bind preference to fragment
            fragment.setPreference(this);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public String getFragmentTag() {
        return "color_" + getKey();
    }

    public int getValue() {
        return mValue;
    }

    public static class ColorDialogFragment extends DialogFragment {
        private ColorPreference mPreference;
        private GridLayout mColorGrid;

        public ColorDialogFragment() {
        }

        public static ColorDialogFragment newInstance() {
            return new ColorDialogFragment();
        }

        public void setPreference(ColorPreference preference) {
            mPreference = preference;
            repopulateItems();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            repopulateItems();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View rootView = layoutInflater.inflate(R.layout.dialog_colors, null);

            mColorGrid = (GridLayout) rootView.findViewById(R.id.color_grid);
            mColorGrid.setColumnCount(mPreference.mNumColumns);
            repopulateItems();

            return new DialogBuilder(getActivity())
                    .setView(rootView)
                    .create();
        }

        private void repopulateItems() {
            if (mPreference == null || mColorGrid == null) {
                return;
            }

            Context context = mColorGrid.getContext();
            mColorGrid.removeAllViews();
            for (final int color : mPreference.mColorChoices) {
                View itemView = LayoutInflater.from(context)
                        .inflate(R.layout.grid_item_color, mColorGrid, false);

                setColorViewValue(itemView.findViewById(R.id.color_view), color,
                        color == mPreference.getValue());
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPreference.setValue(color);
                        dismiss();
                    }
                });

                mColorGrid.addView(itemView);
            }

            sizeDialog();
        }

        @Override
        public void onStart() {
            super.onStart();
            sizeDialog();
        }

        private void sizeDialog() {
            if (mPreference == null || mColorGrid == null) {
                return;
            }

            Dialog dialog = getDialog();
            if (dialog == null) {
                return;
            }

            final Resources res = mColorGrid.getContext().getResources();
            DisplayMetrics dm = res.getDisplayMetrics();

            // Can't use Integer.MAX_VALUE here (weird issue observed otherwise on 4.2)
            mColorGrid.measure(
                    View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.AT_MOST));
            int width = mColorGrid.getMeasuredWidth();
            int height = mColorGrid.getMeasuredHeight();

            int extraPadding = res.getDimensionPixelSize(R.dimen.color_grid_extra_padding);

            width += extraPadding;
            height += extraPadding;

            dialog.getWindow().setLayout(width, height);
        }
    }

    private static void setColorViewValue(View view, int color, boolean selected) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Resources res = imageView.getContext().getResources();

            Drawable currentDrawable = imageView.getDrawable();
            GradientDrawable colorChoiceDrawable;
            if (currentDrawable instanceof GradientDrawable) {
                // Reuse drawable
                colorChoiceDrawable = (GradientDrawable) currentDrawable;
            } else {
                colorChoiceDrawable = new GradientDrawable();
                colorChoiceDrawable.setShape(GradientDrawable.OVAL);
            }

            // Set stroke to dark version of color
            int darkenedColor = Color.rgb(
                    Color.red(color) * 192 / 256,
                    Color.green(color) * 192 / 256,
                    Color.blue(color) * 192 / 256);

            colorChoiceDrawable.setColor(color);
            colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2, res.getDisplayMetrics()), darkenedColor);

            Drawable drawable = colorChoiceDrawable;
            if (selected) {

                VectorDrawableCompat checkmark = VectorDrawableCompat.create(view.getResources(),
                        R.drawable.checkmark_white, null);
                InsetDrawable checkmarkInset = new InsetDrawable(checkmark, Utils.dpToPx(5));
                drawable = new LayerDrawable(new Drawable[]{
                        colorChoiceDrawable,
                        checkmarkInset});
            }

            imageView.setImageDrawable(drawable);

        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
    }
}