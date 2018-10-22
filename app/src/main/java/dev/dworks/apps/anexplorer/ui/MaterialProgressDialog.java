package dev.dworks.apps.anexplorer.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import dev.dworks.apps.anexplorer.R;

public class MaterialProgressDialog extends ProgressDialog {
    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    private MaterialProgressDrawable indeterminateDrawable;
    private int mDefaultColor;
    private int mColor;

    public MaterialProgressDialog(Context context) {
        super(context);
    }

    public MaterialProgressDialog(Context context, int defStyle) {
        super(context, defStyle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDefaultColor = ContextCompat.getColor(getContext(), R.color.accentColor);

        indeterminateDrawable = new MaterialProgressDrawable(getContext(), findViewById(android.R.id.progress));
        indeterminateDrawable.setBackgroundColor(CIRCLE_BG_LIGHT);
        indeterminateDrawable.setAlpha(255);
        indeterminateDrawable.updateSizes(MaterialProgressDrawable.XLARGE);
        indeterminateDrawable.setColorSchemeColors(getColor());
        indeterminateDrawable.start();
        setIndeterminateDrawable(indeterminateDrawable);
    }

    public void setColor(int color){
        mColor = color;
    }

    public int getColor(){
        if(mColor == 0){
            return mDefaultColor;
        }
        return mColor;
    }
}