package dev.dworks.apps.anexplorer.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import dev.dworks.apps.anexplorer.misc.LogUtils;
import dev.dworks.apps.anexplorer.misc.Utils;
import dev.dworks.apps.anexplorer.server.TransferServer;
import dev.dworks.apps.anexplorer.transfer.model.Device;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.NotificationHelper;
import dev.dworks.apps.anexplorer.transfer.model.Bundle;
import dev.dworks.apps.anexplorer.transfer.model.FileItem;
import dev.dworks.apps.anexplorer.transfer.model.Transfer;

import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_BROADCAST;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_REMOVE_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_START_LISTENING;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_START_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_LISTENING;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.ACTION_STOP_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_DEVICE;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_ID;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_TRANSFER;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.EXTRA_URIS;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.getAssetFileDescriptor;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.getFilename;
import static dev.dworks.apps.anexplorer.transfer.TransferHelper.traverseDirectory;

/**
 * Receive incoming transfers and initiate outgoing transfers
 *
 * This service listens for new connections and instantiates Transfer instances
 * to process them. It will also initiate a transfer when the appropriate
 * intent is supplied.
 */
public class TransferService extends Service {

    private static final String TAG = "TransferService";

    private NotificationHelper mNotificationHelper;
    private TransferServer mTransferServer;
    private TransferHelper mTransferHelper;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationHelper = new NotificationHelper(this);
        try {
            mTransferServer = new TransferServer(this, mNotificationHelper, new TransferServer.Listener() {
                @Override
                public void onNewTransfer(Transfer transfer) {
                    transfer.setId(mNotificationHelper.nextId());
                    mTransferHelper.addTransfer(transfer, null);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        mTransferHelper = new TransferHelper(this, mNotificationHelper);
    }

    private void startListening() {
        mTransferServer.start();
    }

    private void stopListening() {
        mTransferServer.stop();
    }

    /**
     * Start or stop the service
     * @param context context to use for sending the intent
     * @param start true to start the service; false to stop it
     */
    public static void startStopService(Context context, boolean start) {
        Intent intent = new Intent(context, TransferService.class);
        if (start) {
            LogUtils.LOGI(TAG, "sending intent to start service");
            intent.setAction(ACTION_START_LISTENING);
        } else {
            LogUtils.LOGI(TAG, "sending intent to stop service");
            intent.setAction(ACTION_STOP_LISTENING);
        }

        // Android O doesn't allow certain broadcasts to start services as per usual
        if (Utils.hasOreo()) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Create a bundle from the list of URIs
     * @param uriList list of URIs to add
     * @return newly created bundle
     */
    private Bundle createBundle(ArrayList<Parcelable> uriList) throws IOException {
        Bundle bundle = new Bundle();
        for (Parcelable parcelable : uriList) {
            Uri uri = (Uri) parcelable;
            switch (uri.getScheme()) {
                case ContentResolver.SCHEME_ANDROID_RESOURCE:
                case ContentResolver.SCHEME_CONTENT:
                    bundle.addItem(new FileItem(
                            getAssetFileDescriptor(this, uri),
                            getFilename(this, uri)
                    ));
                    break;
                case ContentResolver.SCHEME_FILE:
                    File file = new File(uri.getPath());
                    if (file.isDirectory()) {
                        traverseDirectory(file, bundle);
                    } else {
                        bundle.addItem(new FileItem(file));
                    }
                    break;
            }
        }
        return bundle;
    }

    /**
     * Start a transfer using the provided intent
     */
    private void startTransfer(Intent intent) {

        // Build the parameters needed to start the transfer
        Device device = (Device) intent.getSerializableExtra(EXTRA_DEVICE);

        // Add each of the items to the bundle and send it
        try {
            Bundle bundle = createBundle(intent.getParcelableArrayListExtra(EXTRA_URIS));
            int nextId = intent.getIntExtra(EXTRA_ID, 0);
            if (nextId == 0) {
                nextId = mNotificationHelper.nextId();
            }
            Transfer transfer = new Transfer(device, TransferHelper.deviceName(), bundle);
            transfer.setId(nextId);
            mTransferHelper.addTransfer(transfer, intent);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

            mNotificationHelper.stopService();
        }
    }

    /**
     * Stop a transfer in progress
     */
    private void stopTransfer(Intent intent) {
        mTransferHelper.stopTransfer(intent.getIntExtra(EXTRA_TRANSFER, -1));
    }

    /**
     * Remove (dismiss) a transfer that has completed
     */
    private int removeTransfer(Intent intent) {
        int id = intent.getIntExtra(EXTRA_TRANSFER, -1);
        mTransferHelper.removeTransfer(id);
        mNotificationHelper.removeNotification(id);
        mNotificationHelper.stopService();
        return START_NOT_STICKY;
    }

    /**
     * Trigger a broadcast for all transfers
     */
    private void broadcast() {
        mTransferHelper.broadcastTransfers();
        mNotificationHelper.stopService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.LOGI(TAG, String.format("received intent: %s", intent.getAction()));
        if (intent == null) {
            return START_REDELIVER_INTENT;
        }
        switch (intent.getAction()) {
            case ACTION_START_LISTENING:
                startListening();
                // return START_REDELIVER_INTENT;
                break;
            case ACTION_STOP_LISTENING:
                stopListening();
                break;
            case ACTION_START_TRANSFER:
                startTransfer(intent);
                break;
            case ACTION_STOP_TRANSFER:
                stopTransfer(intent);
                break;
            case ACTION_REMOVE_TRANSFER:
                removeTransfer(intent);
                break;
            case ACTION_BROADCAST:
                broadcast();
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.LOGD(TAG, "service destroyed");
    }
}
