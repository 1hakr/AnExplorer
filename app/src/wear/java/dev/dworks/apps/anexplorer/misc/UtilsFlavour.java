package dev.dworks.apps.anexplorer.misc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.activity.ConfirmationActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.wear.widget.ConfirmationOverlay;
import androidx.wear.widget.drawer.WearableActionDrawerView;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

public class UtilsFlavour {

    public static void showInfo(Context context, int messageId){
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                context.getString(messageId));
        context.startActivity(intent);
    }

    public static Menu getActionDrawerMenu(DocumentsActivity activity){
        WearableActionDrawerView actionDrawer = activity.findViewById(R.id.bottom_action_drawer);
        Menu menu = actionDrawer.getMenu();
        actionDrawer.setOnMenuItemClickListener(activity);
        return menu;
    }

    public static View getActionDrawer(DocumentsActivity activity){
        WearableActionDrawerView actionDrawer = activity.findViewById(R.id.bottom_action_drawer);
        return actionDrawer;
    }

    public static void inflateActionMenu(Activity activity,
                                         MenuItem.OnMenuItemClickListener listener,
                                         boolean contextual, RootInfo root, DocumentInfo cwd) {
        WearableActionDrawerView actionDrawer = activity.findViewById(R.id.bottom_action_drawer);
        Menu menu = actionDrawer.getMenu();
        if(null != listener) {
            actionDrawer.setOnMenuItemClickListener(listener);
        }
        if(!contextual){
            menu.clear();
            if(null != root
                    && !root.isHome() && !root.isConnections() && !root.isApp()) {
                activity.getMenuInflater().inflate(R.menu.document_base, menu);
            }
            activity.getMenuInflater().inflate(R.menu.activity_base, menu);
            actionDrawer.getController().closeDrawer();
        } else {
            menu.clear();
            if(null != root && root.isApp()) {
                activity.getMenuInflater().inflate(R.menu.apps_base, menu);
            } else {
                activity.getMenuInflater().inflate(R.menu.directory_base, menu);
            }
            actionDrawer.getController().peekDrawer();
        }
    }

    public static void showMessage(Activity activity, String message,
                                    int duration, String action, View.OnClickListener listener){
        int type = !TextUtils.isEmpty(action) && action.equals("ERROR")
                ? ConfirmationOverlay.FAILURE_ANIMATION : ConfirmationOverlay.SUCCESS_ANIMATION;
        new ConfirmationOverlay()
                .setType(type)
                .setMessage(message)
                .showOn(activity);
    }
}
