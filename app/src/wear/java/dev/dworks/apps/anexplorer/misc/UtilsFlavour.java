package dev.dworks.apps.anexplorer.misc;

import android.content.Context;
import android.content.Intent;
import android.support.wearable.activity.ConfirmationActivity;

public class UtilsFlavour {

    public static void showInfo(Context context, int messageId){
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                context.getString(messageId));
        context.startActivity(intent);
    }
}
