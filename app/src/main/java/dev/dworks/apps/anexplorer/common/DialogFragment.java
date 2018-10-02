package dev.dworks.apps.anexplorer.common;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import android.text.TextUtils;
import android.widget.Button;

import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.Utils;

/**
 * Created by HaKr on 12/06/16.
 */

public class DialogFragment extends AppCompatDialogFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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

    public static void tintButtons(Dialog dialog){
        Utils.tintButton(getButton(dialog, DialogInterface.BUTTON_POSITIVE));
        Utils.tintButton(getButton(dialog, DialogInterface.BUTTON_NEGATIVE));
        Utils.tintButton(getButton(dialog, DialogInterface.BUTTON_NEUTRAL));
    }

    private static Button getButton(Dialog dialog, int which){
        return ((AlertDialog)dialog).getButton(which);
    }

    public static void showThemedDialog(AlertDialog.Builder builder){
        Dialog dialog = builder.create();
        dialog.show();
        tintButtons(dialog);
    }

    public static void showThemedDialog(Activity activity,
                                        String titleId, String messageId, String positiveButtonId,
                                        String negativeButtonId,
                                        final DialogInterface.OnClickListener positiveClickListener,
                                        final DialogInterface.OnClickListener negativeClickListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(messageId)
                .setCancelable(false)
                .setPositiveButton(positiveButtonId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (null != positiveClickListener) {
                            positiveClickListener.onClick(dialog, which);
                        }
                    }
                }).setNegativeButton(negativeButtonId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (null != negativeClickListener) {
                    negativeClickListener.onClick(dialog, which);
                }
            }
        });
        if(TextUtils.isEmpty(titleId)){
            builder.setTitle(titleId);
        }
        Dialog dialog = builder.create();
        dialog.show();
        tintButtons(dialog);
    }

}
