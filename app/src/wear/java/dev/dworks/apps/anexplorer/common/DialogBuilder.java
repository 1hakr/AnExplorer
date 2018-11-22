package dev.dworks.apps.anexplorer.common;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.CircledImageView;
import dev.dworks.apps.anexplorer.ui.MaterialProgressBar;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DialogBuilder extends DialogCommonBuilder {

    public DialogBuilder(Context context) {
        super(context);
    }

    @Override
    public Dialog create() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final LayoutInflater dialogInflater = LayoutInflater.from(mContext);

        final View view = dialogInflater.inflate(R.layout.dialog_layout_base, null, false);
        final TextView text1 = view.findViewById(android.R.id.text1);
        final TextView text2 = view.findViewById(android.R.id.text2);
        final CircledImageView button1 = view.findViewById(android.R.id.button1);
        final CircledImageView button2 = view.findViewById(android.R.id.button2);

        if(!TextUtils.isEmpty(mTitle)) {
            text1.setText(mTitle);
        } else if(!TextUtils.isEmpty(mMessage)) {
            if(TextUtils.isEmpty(mTitle)){
                text1.setText(mMessage);
            } else {
                text2.setText(mMessage);
            }
        } else {
            text1.setVisibility(View.GONE);
        }
        builder.setCancelable(mCancelable);

        builder.setView(view);
        if(mindeterminate){
            mCustomView = dialogInflater.inflate(R.layout.dialog_layout_progress, null, false);
            MaterialProgressBar progress = mCustomView.findViewById(R.id.circular_progress);
            progress.setColor(SettingsActivity.getAccentColor());

            button1.setVisibility(Utils.getVisibility(false));
            button2.setVisibility(Utils.getVisibility(false));
        }
        if (null != mCustomView) {
            final FrameLayout custom = (FrameLayout) view.findViewById(R.id.custom);
            final View scrolltext = view.findViewById(R.id.scrolltext);
            custom.setVisibility(Utils.getVisibility(true));
            scrolltext.setVisibility(Utils.getVisibility(false));
            custom.addView(mCustomView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
        final Dialog dialog = builder.create();
        button1.setCircleColor(SettingsActivity.getAccentColor());
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(null != mPositiveButtonListener){
                    mPositiveButtonListener.onClick(dialog, 1);
                }
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(null != mNegativeButtonListener){
                    mNegativeButtonListener.onClick(dialog, 2);
                }
            }
        });
        return dialog;
    }
}
