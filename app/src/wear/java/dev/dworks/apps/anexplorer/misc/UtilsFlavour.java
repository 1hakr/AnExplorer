package dev.dworks.apps.anexplorer.misc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.activity.ConfirmationActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.wear.widget.drawer.WearableActionDrawerView;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;

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
}
