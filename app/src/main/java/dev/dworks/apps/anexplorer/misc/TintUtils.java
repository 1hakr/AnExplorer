package dev.dworks.apps.anexplorer.misc;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.core.graphics.drawable.DrawableCompat;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class TintUtils {


    public static void tintWidget(View view) {
        tintWidget(view, SettingsActivity.getAccentColor());
    }

    public static void tintButton(Button view) {
        view.setTextColor(SettingsActivity.getAccentColor());
    }

    public static void tintWidget(View view, int color) {
        Drawable wrappedDrawable = DrawableCompat.wrap(view.getBackground());
        DrawableCompat.setTint(wrappedDrawable.mutate(), color);
        ViewCompat.setBackground(view, wrappedDrawable);
    }

    public static void tintWidget(SeekBar view, int color){
        LayerDrawable layerDrawable = (LayerDrawable) view.getProgressDrawable();
        Drawable clipDrawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
        tintDrawable(clipDrawable, color);
    }

    public static void tintDrawable(Drawable drawable, int color){
        Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrappedDrawable.mutate(), color);
    }

}
