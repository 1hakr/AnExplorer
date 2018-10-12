package dev.dworks.apps.anexplorer.common;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.ui.MaterialProgressDialog;

import static dev.dworks.apps.anexplorer.common.DialogFragment.tintButtons;

public abstract class DialogCommonBuilder {
    public Context mContext;
    protected String mTitle;
    protected View mCustomView;
    protected String mMessage;
    protected String mPositiveButtonText;
    protected DialogInterface.OnClickListener mPositiveButtonListener;
    protected String mNegativeButtonText;
    protected DialogInterface.OnClickListener mNegativeButtonListener;
    protected boolean mCancelable = true;
    protected boolean mindeterminate;

    public DialogCommonBuilder(Context context){
        mContext = context;
    }

    public DialogCommonBuilder setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    public DialogCommonBuilder setTitle(int title) {
        this.mTitle = mContext.getString(title);
        return this;
    }

    public DialogCommonBuilder setMessage(String message) {
        this.mMessage = message;
        return this;
    }

    public DialogCommonBuilder setMessage(int message) {
        this.mMessage = mContext.getString(message);
        return this;
    }

    public DialogCommonBuilder setPositiveButtonText(String text) {
        this.mPositiveButtonText = text;
        return this;
    }

    public DialogCommonBuilder setNegativeButtonText(String text) {
        this.mNegativeButtonText = text;
        return this;
    }

    public DialogCommonBuilder setPositiveButtonListener(DialogInterface.OnClickListener onClickListener) {
        this.mPositiveButtonListener = onClickListener;
        return this;
    }

    public DialogCommonBuilder setNegativeButtonListener(DialogInterface.OnClickListener onClickListener) {
        this.mNegativeButtonListener = onClickListener;
        return this;
    }

    public DialogCommonBuilder setPositiveButton(int text, DialogInterface.OnClickListener onClickListener) {
        setPositiveButtonText(mContext.getString(text));
        setPositiveButtonListener(onClickListener);
        return this;
    }
    public DialogCommonBuilder setPositiveButton(String text, DialogInterface.OnClickListener onClickListener) {
        setPositiveButtonText(text);
        setPositiveButtonListener(onClickListener);
        return this;
    }

    public DialogCommonBuilder setNegativeButton(int text, DialogInterface.OnClickListener onClickListener) {
        setNegativeButtonText(mContext.getString(text));
        setNegativeButtonListener(onClickListener);
        return this;
    }

    public DialogCommonBuilder setNegativeButton(String text, DialogInterface.OnClickListener onClickListener) {
        setNegativeButtonText(text);
        setNegativeButtonListener(onClickListener);
        return this;
    }

    public DialogCommonBuilder setCancelable(boolean cancelable) {
        this.mCancelable = cancelable;
        return this;
    }

    public void setIndeterminate(boolean indeterminate) {
        mindeterminate = indeterminate;
    }

    public DialogCommonBuilder setView(View view) {
        this.mCustomView = view;
        return this;
    }

    public Dialog create() {
        if(mindeterminate){
            return createProgressDialog();
        }
        mPositiveButtonText = TextUtils.isEmpty(mPositiveButtonText)
                ? mContext.getString(android.R.string.ok) : mPositiveButtonText;
        mNegativeButtonText = TextUtils.isEmpty(mNegativeButtonText)
                ? mContext.getString(android.R.string.cancel) : mNegativeButtonText;

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mMessage).setCancelable(mCancelable);
        builder.setPositiveButton(mPositiveButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (null != mPositiveButtonListener) {
                    mPositiveButtonListener.onClick(dialog, which);
                }
            }
        });
        builder.setNegativeButton(mNegativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (null != mNegativeButtonListener) {
                    mNegativeButtonListener.onClick(dialog, which);
                }
            }
        });
        if (!TextUtils.isEmpty(mTitle)) {
            builder.setTitle(mTitle);
        }
        if(null != mCustomView){
            builder.setView(mCustomView);

        }
        Dialog dialog = builder.create();
        return dialog;
    }

    public Dialog createProgressDialog(){
        MaterialProgressDialog progressDialog = new MaterialProgressDialog(mContext);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setColor(SettingsActivity.getAccentColor());
        progressDialog.setCancelable(mCancelable);
        progressDialog.setMessage(mMessage);
        return progressDialog;
    }

    public void show(){
        Dialog dialog = create();
        dialog.show();
        tintButtons(dialog);
    }

    public void showDialog(){
        show();
    }
}
