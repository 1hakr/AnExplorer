package dev.dworks.apps.anexplorer.misc;

import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.recyclerview.widget.RecyclerView;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class UtilsFlavour {

    public static void showInfo(Context context, int messageId){

    }

    public static Menu getActionDrawerMenu(Activity activity){
        return null;
    }

    public static View getActionDrawer(Activity activity){
        return null;
    }

    public static void inflateActionMenu(Activity activity,
                                         MenuItem.OnMenuItemClickListener listener,
                                         boolean contextual, RootInfo root, DocumentInfo cwd) {
    }

    public static void showMessage(Activity activity, String message,
                                   int duration, String action, View.OnClickListener listener){
        Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.content_view), message, duration);
        if (null != listener) {
            snackbar.setAction(action, listener)
                    .setActionTextColor(SettingsActivity.getAccentColor());
        }
        snackbar.show();
    }

    public static void setItemsCentered(RecyclerView recyclerView, boolean isEnabled) {

    }

    public static void inflateNoteActionMenu(Activity activity,
                                             MenuItem.OnMenuItemClickListener listener, boolean updated) {
    }

    public static void closeNoteActionMenu(Activity activity) {
    }
}