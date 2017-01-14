package dev.dworks.apps.anexplorer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
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

}
