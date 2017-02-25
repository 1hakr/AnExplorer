package dev.dworks.apps.anexplorer.misc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_ROOT;

/**
 * Created by HaKr on 05/09/16.
 */

public class NotificationUtils {

    public static final int FTP_NOTIFICATION_ID = 916;

    public static void createFtpNotification(Context context, Intent intent, int notification_id){
        RootInfo root = intent.getExtras().getParcelable(EXTRA_ROOT);
        if(null == root){
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        long when = System.currentTimeMillis();

        CharSequence contentTitle = getString(context,R.string.ftp_notif_title);
        CharSequence contentText = String.format(getString(context,R.string.ftp_notif_text),
                ConnectionUtils.getFTPAddress(context));
        CharSequence tickerText = getString(context, R.string.ftp_notif_starting);
        CharSequence stopText = getString(context,R.string.ftp_notif_stop_server);

        Intent notificationIntent = new Intent(context, DocumentsActivity.class);
        notificationIntent.setData(root.getUri());
        notificationIntent.putExtras(intent.getExtras());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        Intent stopIntent = new Intent(ACTION_STOP_FTPSERVER);
        stopIntent.putExtras(intent.getExtras());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_server)
                .setTicker(tickerText)
                .setWhen(when)
                .setOngoing(true)
                .setColor(SettingsActivity.getPrimaryColor())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.drawable.ic_action_stop, stopText, stopPendingIntent)
                .setShowWhen(false);

        Notification notification = builder.build();

        notificationManager.notify(notification_id, notification);
    }

    public static void removeNotification(Context context, int notification_id){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notification_id);
    }

    private static String getString(Context context, int id){
        return  context.getResources().getString(id);
    }
}
