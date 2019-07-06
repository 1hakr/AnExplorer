package dev.dworks.apps.anexplorer.transfer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.ShareDeviceActivity;
import dev.dworks.apps.anexplorer.misc.FileUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;
import dev.dworks.apps.anexplorer.service.TransferService;
import dev.dworks.apps.anexplorer.transfer.model.Bundle;
import dev.dworks.apps.anexplorer.transfer.model.FileItem;
import dev.dworks.apps.anexplorer.transfer.model.Item;
import dev.dworks.apps.anexplorer.transfer.model.Transfer;
import dev.dworks.apps.anexplorer.transfer.model.TransferStatus;
import dev.dworks.apps.anexplorer.transfer.model.UrlItem;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_START_FTPSERVER;

public class TransferHelper {

    private static final String TAG = "TransferHelper";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".transfer.documents";
    public static final String SERVICE_TYPE = "_anexplorer._tcp.";
    public static final String UUID = "uuid";

    public static final String ACTION_START_LISTENING = BuildConfig.APPLICATION_ID + ".action.START_LISTENING";
    public static final String ACTION_STOP_LISTENING = BuildConfig.APPLICATION_ID + ".action.STOP_LISTENING";

    public static final String ACTION_START_TRANSFER = BuildConfig.APPLICATION_ID + ".action.START_TRANSFER";
    public static final String ACTION_STOP_TRANSFER = BuildConfig.APPLICATION_ID + ".action.STOP_TRANSFER";
    public static final String ACTION_REMOVE_TRANSFER = BuildConfig.APPLICATION_ID + ".action.REMOVE_TRANSFER";

    public static final String ACTION_BROADCAST = BuildConfig.APPLICATION_ID + ".action.BROADCAST";

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String EXTRA_URIS = "EXTRA_URLS";
    public static final String EXTRA_ID =  "EXTRA_ID";

    public static final String EXTRA_TRANSFER = "EXTRA_TRANSFER";

    public static final String TRANSFER_UPDATED = "EXTRA_TRANSFER_UPDATED";
    public static final String EXTRA_STATUS = "EXTRA_STATUS";

    private static final int SHARE_REQUEST = 1;

    private Context mContext;
    private final NotificationHelper mNotificationHelper;
    private final SparseArray<Transfer> mTransfers = new SparseArray<>();

    public TransferHelper(Context context, NotificationHelper notificationHelper) {
        mContext = context;
        mNotificationHelper = notificationHelper;
    }

    public void startTransferServer(){
        Intent intent = new Intent(ACTION_START_LISTENING);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        mContext.sendBroadcast(intent);
    }

    public void stopTransferServer(){
        Intent intent = new Intent(ACTION_STOP_LISTENING);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        mContext.sendBroadcast(intent);
    }

    private void broadcastTransferStatus(TransferStatus transferStatus) {
        Intent intent = new Intent();
        intent.setAction(TRANSFER_UPDATED);
        intent.putExtra(EXTRA_STATUS, transferStatus);
        mContext.sendBroadcast(intent);
    }

    /**
     * Add a transfer to the list
     */
    public void addTransfer(final Transfer transfer, final Intent intent) {

        // Grab the initial status
        TransferStatus transferStatus = transfer.getStatus();

        Log.i(TAG, String.format("starting transfer #%d...", transferStatus.getId()));

        // Add a listener for status change events
        transfer.addStatusChangedListener(new Transfer.StatusChangedListener() {
            @Override
            public void onStatusChanged(TransferStatus transferStatus) {

                // Broadcast transfer status
                broadcastTransferStatus(transferStatus);

                // Update the transfer notification manager
                mNotificationHelper.updateTransfer(transferStatus, intent);
            }
        });

        // Add a listener for items being received
        transfer.addItemReceivedListener(new Transfer.ItemReceivedListener() {
            @Override
            public void onItemReceived(Item item) {
                if ( item instanceof FileItem) {
                    String path = ((FileItem) item).getPath();
                    FileUtils.updateMediaStore(mContext, path);
                } else if (item instanceof UrlItem) {
                    try {
                        mNotificationHelper.showUrl(((UrlItem) item).getUrl());
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });

        // Add the transfer to the list
        synchronized (mTransfers) {
            mTransfers.append(transferStatus.getId(), transfer);
        }

        // Add the transfer to the notification manager and immediately update it
        mNotificationHelper.addTransfer(transferStatus);
        mNotificationHelper.updateTransfer(transferStatus, intent);

        // Create a new thread and run the transfer in it
        new Thread(transfer).start();
    }

    /**
     * Stop the transfer with the specified ID
     */
    public void stopTransfer(int id) {
        synchronized (mTransfers) {
            Transfer transfer = mTransfers.get(id);
            if (transfer != null) {
                Log.i(TAG, String.format("stopping transfer #%d...", transfer.getStatus().getId()));
                transfer.stop();
            }
        }
    }

    /**
     * Remove the transfer with the specified ID
     *
     * Transfers that are in progress cannot be removed and a warning is logged
     * if this is attempted.
     */
    public void removeTransfer(int id) {
        synchronized (mTransfers) {
            Transfer transfer = mTransfers.get(id);
            if (transfer != null) {
                TransferStatus transferStatus = transfer.getStatus();
                if (!transferStatus.isFinished()) {
                    Log.w(TAG, String.format("cannot remove ongoing transfer #%d",
                            transferStatus.getId()));
                    return;
                }
                mTransfers.remove(id);
            }
        }
    }

    /**
     * Trigger a broadcast of all transfers
     */
    public void broadcastTransfers() {
        for (int i = 0; i < mTransfers.size(); i++) {
            broadcastTransferStatus(mTransfers.valueAt(i).getStatus());
        }
    }

    public static boolean isTransferAuthority(Intent intent){
        if(null != intent.getData()){
            String authority = intent.getData().getAuthority();
            return TransferHelper.AUTHORITY.equals(authority);
        }
        return false;
    }
    public static boolean isServerRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        String transferServiceClassName = TransferService.class.getName();
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            String currentClassName = service.service.getClassName();
            if (transferServiceClassName.equals(currentClassName)) {
                return true;
            }
        }
        return false;
    }

    public static void sendDocs(Activity activity, ArrayList<DocumentInfo> docs) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (DocumentInfo doc: docs) {
            uris.add(doc.derivedUri);
        }
       sendFiles(activity, uris);
    }

    public static void sendFiles(Activity activity, ArrayList<Uri> uris) {
        Intent shareIntent = new Intent(activity, ShareDeviceActivity.class);
        shareIntent.setAction("android.intent.action.SEND_MULTIPLE");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        activity.startActivityForResult(shareIntent, SHARE_REQUEST);
    }
    /**
     * Attempt to resolve the provided URI
     * @param uri URI to resolve
     * @return file descriptor
     */
    public static AssetFileDescriptor getAssetFileDescriptor(Context context, Uri uri) throws IOException {
        AssetFileDescriptor assetFileDescriptor;
        try {
            assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            throw new IOException(String.format("unable to resolve \"%s\"", uri.toString()));
        }
        if (assetFileDescriptor == null) {
            throw new IOException(String.format("no file descriptor for \"%s\"", uri.toString()));
        }
        return assetFileDescriptor;
    }

    // Overwrite files with identical names
    public static boolean overwriteFiles(){
        return true;
    }

    // Default sounds, vibrate, etc. for transfers
    public static boolean makeNotificationSound(){
        return true;
    }

    // Device name broadcast via mDNS
    public static String deviceName(){
        return Build.MODEL;
    }

    // Unique identifier for the device
    public static String deviceUUID(){
        String uuid = String.format("{%s}", java.util.UUID.randomUUID().toString());
        return uuid;
    }

    // Directory for storing received files
    public static String transferDirectory(){
        File storage = Environment.getExternalStorageDirectory();
        File downloads = new File(storage, "Download");
        return new File(downloads, "AnExplorer").getAbsolutePath();
    }

    /**
     * Determine the appropriate filename for a URI
     * @param uri URI to use for filename
     * @return filename
     */
    public static String getFilename(Context context, Uri uri) {
        String filename = uri.getLastPathSegment();
        String[] projection = {
                MediaStore.MediaColumns.DISPLAY_NAME,
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                String columnValue = cursor.getString(cursor.getColumnIndexOrThrow(
                        MediaStore.MediaColumns.DISPLAY_NAME));
                if (columnValue != null) {
                    filename = new File(columnValue).getName();
                }
            } catch (IllegalArgumentException ignored) {
            }
            cursor.close();
        }
        return filename;
    }

    /**
     * Traverse a directory tree and add all files to the bundle
     * @param root the directory to which all filenames will be relative
     * @param bundle target for all files that are found
     */
    public static void traverseDirectory(File root, Bundle bundle) throws IOException {
        Stack<File> stack = new Stack<>();
        stack.push(root);
        while (!stack.empty()) {
            File topOfStack = stack.pop();
            for (File f : topOfStack.listFiles()) {
                if (f.isDirectory()) {
                    stack.push(f);
                } else {
                    String relativeFilename = f.getAbsolutePath().substring(
                            root.getParentFile().getAbsolutePath().length() + 1);
                    bundle.addItem(new FileItem(f, relativeFilename));
                }
            }
        }
    }
}
