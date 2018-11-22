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
import android.content.DialogInterface;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.common.DialogBuilder;
import dev.dworks.apps.anexplorer.misc.ColorPalette;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.common.DialogFragment.tintButtons;


/**
 * A preference that allows the user to choose an application or shortcut.
 */
public class MaterialColorPreference extends Preference {
    public static final int TYPE_PRIMARY = 0;
    public static final int TYPE_ACCENT = 1;

    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.pref_layout_color;
    private int colorType;
    private View mPreviewView;

    public MaterialColorPreference(Context context) {
        super(context);
        initAttrs(null, 0);
    }

    public MaterialColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public MaterialColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs, defStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColorPreference, defStyle, defStyle);

        try {
            mItemLayoutId = a.getResourceId(R.styleable.ColorPreference_itemLayout, mItemLayoutId);
            colorType = a.getInt(R.styleable.ColorPreference_color_type, TYPE_PRIMARY);
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
        fragment.setColorType(colorType);

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
            fragment.setColorType(colorType);
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
        private MaterialColorPreference mPreference;
        private LineColorPicker mColorPicker;
        private LineColorPicker mShadePicker;
        private TextView mTitle;
        int colorType;

        public ColorDialogFragment() {
        }

        public static ColorDialogFragment newInstance() {
            return new ColorDialogFragment();
        }

        public void setPreference(MaterialColorPreference preference) {
            mPreference = preference;
        }

        public void setColorType(int type){
            colorType = type;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if(null != savedInstanceState) {
                dismiss();
                return;
            }
            if(!getShowsDialog()){
                return;
            }
            getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    try{
                        tintButtons(getDialog());
                    } catch (Exception e){
                        CrashReportingManager.logException(e);
                    }
                }
            });
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final DialogBuilder builder = new DialogBuilder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(context);

            final View rootView = dialogInflater.inflate(R.layout.layout_color_preference, null, false);

            mColorPicker = (LineColorPicker) rootView.findViewById(R.id.color_picker);
            mShadePicker = (LineColorPicker) rootView.findViewById(R.id.shade_picker);
            mTitle = (TextView) rootView.findViewById(R.id.title);

            int color = SettingsActivity.getPrimaryColor();
            if(colorType == 0){
                mTitle.setText(R.string.primary_color);
                mColorPicker.setColors(ColorPalette.getBaseColors(context));
                for (int i : mColorPicker.getColors()) {
                    for (int j : ColorPalette.getColors(context, i)) {
                        if (j == color) {
                            mColorPicker.setSelectedColor(i);
                            mShadePicker.setColors(ColorPalette.getColors(context, i));
                            mShadePicker.setSelectedColor(j);
                            break;
                        }
                    }
                }
            }
            else if(colorType == 1){
                mTitle.setText(R.string.accent_color);
                mColorPicker.setColors(ColorPalette.getAccentColors(context));
                mShadePicker.setVisibility(View.GONE);
                color = SettingsActivity.getAccentColor();
                mColorPicker.setSelectedColor(color);
            }

            mTitle.setBackgroundColor(color);
            mColorPicker.setOnColorChangedListener(new LineColorPicker.OnColorChangedListener() {
                @Override
                public void onColorChanged(int c) {
                    mTitle.setBackgroundColor(c);
                    if(colorType == 0) {
                        mShadePicker.setColors(ColorPalette.getColors(context, mColorPicker.getColor()));
                        mShadePicker.setSelectedColor(mColorPicker.getColor());
                        ((SettingsActivity)getActivity()).changeActionBarColor(mColorPicker.getColor());
                    }
                }
            });
            mShadePicker.setOnColorChangedListener(new LineColorPicker.OnColorChangedListener() {
                @Override
                public void onColorChanged(int c) {
                    mTitle.setBackgroundColor(c);
                }
            });

            builder.setView(rootView);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(colorType == 0) {
                        mPreference.setValue(mShadePicker.getColor());
                        SettingsActivity.setAccentColor(ColorPalette.getAccentColor(context, mColorPicker.getColor()));
                    } else if (colorType == 1) {
                        mPreference.setValue(mColorPicker.getColor());
                    }
                    dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(colorType == 0) {
                        ((SettingsActivity)getActivity()).changeActionBarColor(SettingsActivity.getPrimaryColor());
                    }
                    dismiss();
                }
            });
            return builder.create();
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