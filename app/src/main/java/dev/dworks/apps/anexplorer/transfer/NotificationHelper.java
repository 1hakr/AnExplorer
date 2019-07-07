package dev.dworks.apps.anexplorer.transfer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.DocumentsActivity;
import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.RootsCache;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.setting.SettingsActivity;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;
import static dev.dworks.apps.anexplorer.misc.NotificationUtils.RECEIVE_CHANNEL;
import static dev.dworks.apps.anexplorer.misc.NotificationUtils.TRANSFER_CHANNEL;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_ROOT;
import static dev.dworks.apps.anexplorer.misc.Utils.isWatch;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_LISTENING;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_ID;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;

/**
 * Manage notifications and service lifecycle
 *
 * A persistent notification is shown as long as the transfer service is
 * running. A notification is also shown for each transfer in progress,
 * enabling it to be individually cancelled or retried.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    private static final int NOTIFICATION_ID = 1;
    private Service mService;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private PendingIntent mIntent;

    private boolean mListening = false;
    private int mNumTransfers = 0;

    private int mNextId = 2;

    /**
     * Create a notification manager for the specified service
     * @param service service to manage
     */
    public NotificationHelper(Service service) {
        mService = service;
        mNotificationManager = (NotificationManager) mService.getSystemService(
                Service.NOTIFICATION_SERVICE);
        RootsCache roots = DocumentsApplication.getRootsCache(mService);
        RootInfo root = roots.getTransferRoot();

        long when = System.currentTimeMillis();
        CharSequence stopText = mService.getString(R.string.ftp_notif_stop_server);
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ROOT, root);
        Intent notificationIntent = new Intent(mService, DocumentsActivity.class);
        notificationIntent.setPackage(BuildConfig.APPLICATION_ID);
        notificationIntent.setData(root.getUri());
        notificationIntent.putExtras(args);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mIntent = PendingIntent.getActivity(mService, 0, notificationIntent, 0);

        // Create the builder
        mBuilder = new NotificationCompat.Builder(mService, TRANSFER_CHANNEL)
                .setContentIntent(mIntent)
                .setContentTitle(mService.getString(R.string.service_transfer_server_title))
                .setColor(SettingsActivity.getPrimaryColor())
                .setSmallIcon(R.drawable.ic_stat_server)
                .setLocalOnly(true)
                .setWhen(when)
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setShowWhen(false);

        Intent stopIntent = new Intent(ACTION_STOP_LISTENING);
        stopIntent.setPackage(BuildConfig.APPLICATION_ID);
        stopIntent.putExtras(args);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(mService, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT);

        boolean isWatch = isWatch(mService);
        NotificationCompat.Action.Builder actionBuilder =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_action_stop, stopText, stopPendingIntent);

        if(isWatch){
            final NotificationCompat.Action.WearableExtender inlineActionForWear =
                    new NotificationCompat.Action.WearableExtender()
                            .setHintDisplayActionInline(true)
                            .setHintLaunchesActivity(false);
            actionBuilder.extend(inlineActionForWear);
            mBuilder.extend(new NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true));
        }

        NotificationCompat.Action stopAction = actionBuilder.build();
        mBuilder.addAction(stopAction);
    }

    /**
     * Retrieve the next unique integer for a transfer
     * @return new ID
     *
     * The notification with ID equal to 1 is for the persistent notification
     * shown while the service is active.
     */
    public synchronized int nextId() {
        return mNextId++;
    }

    /**
     * Indicate that the server is listening for transfers
     */
    public synchronized void startListening() {
        mListening = true;
        updateNotification();
    }

    /**
     * Indicate that the server has stopped listening for transfers
     */
    public synchronized void stopListening() {
        mListening = false;
        stop();
    }

    /**
     * Stop the service if no tasks are active
     */
    public synchronized void stopService() {
        stop();
    }

    /**
     * Remove a notification
     */
    public void removeNotification(int id) {
        mNotificationManager.cancel(id);
    }

    /**
     * Add a new transfer
     */
    synchronized void addTransfer(TransferStatus transferStatus) {
        mNumTransfers++;
        updateNotification();

        // Clear any existing notification (this shouldn't be necessary, but it is :P)
        removeNotification(transferStatus.getId());
    }

    /**
     * Update a transfer in progress
     */
    synchronized void updateTransfer(TransferStatus transferStatus, Intent intent) {
        if (transferStatus.isFinished()) {
            Log.i(TAG, String.format("#%d finished", transferStatus.getId()));

            // Close the ongoing notification (yes, again)
            mNotificationManager.cancel(transferStatus.getId());

            // Do not show a notification for successful transfers that contain no content
            if (transferStatus.getState() != TransferStatus.State.Succeeded ||
                    transferStatus.getBytesTotal() > 0) {

                // Prepare an appropriate notification for the transfer
                CharSequence contentText;
                int icon;

                if (transferStatus.getState() == TransferStatus.State.Succeeded) {
                    contentText = mService.getString(
                            R.string.service_transfer_status_success,
                            transferStatus.getRemoteDeviceName()
                    );
                    icon = R.drawable.ic_action_done;
                } else {
                    contentText = mService.getString(
                            R.string.service_transfer_status_error,
                            transferStatus.getRemoteDeviceName(),
                            transferStatus.getError()
                    );
                    icon = R.drawable.ic_action_close;
                }

                // Build the notification
                boolean notifications = TransferHelper.makeNotificationSound();
                NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                        .setDefaults(notifications ? NotificationCompat.DEFAULT_ALL : 0)
                        .setContentIntent(mIntent)
                        .setContentTitle(mService.getString(R.string.service_transfer_server_title))
                        .setContentText(contentText)
                        .setSmallIcon(icon);

                // For transfers that send files (and fail), it is possible to retry them
                if (transferStatus.getState() == TransferStatus.State.Failed &&
                        transferStatus.getDirection() == TransferStatus.Direction.Send) {

                    // Ensure the error notification is replaced by the next transfer (I have no idea
                    // why the first line is required but it works :P)
                    intent.setClass(mService, TransferService.class);
                    intent.putExtra(EXTRA_ID, transferStatus.getId());

                    // Add the action
                    builder.addAction(
                            new NotificationCompat.Action.Builder(
                                    R.drawable.ic_refresh,
                                    mService.getString(R.string.service_transfer_action_retry),
                                    PendingIntent.getService(
                                            mService, transferStatus.getId(),
                                            intent, PendingIntent.FLAG_ONE_SHOT
                                    )
                            ).build()
                    );
                }

                // Show the notification
                mNotificationManager.notify(transferStatus.getId(), builder.build());
            }

            mNumTransfers--;

            // Stop the service if there are no active tasks
            if (stop()) {
                return;
            }

            // Update the notification
            updateNotification();

        } else {

            // Prepare the appropriate text for the transfer
            CharSequence contentText;
            int icon;

            if (transferStatus.getDirection() == TransferStatus.Direction.Receive) {
                contentText = mService.getString(
                        R.string.service_transfer_status_receiving,
                        transferStatus.getRemoteDeviceName()
                );
                icon = android.R.drawable.stat_sys_download;
            } else {
                contentText = mService.getString(
                        R.string.service_transfer_status_sending,
                        transferStatus.getRemoteDeviceName()
                );
                icon = android.R.drawable.stat_sys_upload;
            }

            // Intent for stopping this particular service
            Intent stopIntent = new Intent(mService, TransferService.class)
                    .setAction(ACTION_STOP_TRANSFER)
                    .putExtra(EXTRA_TRANSFER, transferStatus.getId());

            // Update the notification
            mNotificationManager.notify(
                    transferStatus.getId(),
                    new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                            .setContentIntent(mIntent)
                            .setContentTitle(mService.getString(R.string.service_transfer_title))
                            .setContentText(contentText)
                            .setOngoing(true)
                            .setProgress(100, transferStatus.getProgress(), false)
                            .setSmallIcon(icon)
                            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                            .addAction(
                                    new NotificationCompat.Action.Builder(
                                            R.drawable.ic_action_stop,
                                            mService.getString(R.string.service_transfer_action_stop),
                                            PendingIntent.getService(mService, transferStatus.getId(), stopIntent, 0)
                                    ).build()
                            )
                            .build()
            );
        }
    }

    /**
     * Create a notification for URLs
     * @param url full URL
     */
    void showUrl(String url) {
        int id = nextId();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mService,
                id,
                new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                0
        );
        mNotificationManager.notify(
                id,
                new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                        .setContentIntent(pendingIntent)
                        .setContentTitle(mService.getString(R.string.service_transfer_notification_url))
                        .setContentText(url)
                        .setSmallIcon(R.drawable.ic_url)
                        .build()
        );
    }

    private void updateNotification() {
        Log.i(TAG, String.format("updating notification with %d transfer(s)...", mNumTransfers));

        if (mNumTransfers == 0) {
            mBuilder.setContentText(mService.getString(
                    R.string.service_transfer_server_listening_text));
        } else {
            mBuilder.setContentText(mService.getResources().getQuantityString(
                    R.plurals.service_transfer_server_transferring_text,
                    mNumTransfers, mNumTransfers));
        }
        mService.startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    private boolean stop() {
        if (!mListening && mNumTransfers == 0) {
            Log.i(TAG, "not listening and no transfers, shutting down...");

            mService.stopSelf();
            return true;
        }
        return false;
    }
}