package dev.dworks.apps.anexplorer.server;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.transfer.model.Device;
import dev.dworks.apps.anexplorer.transfer.TransferHelper;
import dev.dworks.apps.anexplorer.transfer.model.Transfer;
import dev.dworks.apps.anexplorer.transfer.NotificationHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Listen for new connections and create Transfers for them
 */
public class TransferServer implements Runnable {

    private static final String TAG = "TransferServer";

    public interface Listener {
        void onNewTransfer(Transfer transfer);
    }

    private Thread mThread = new Thread(this);
    private boolean mStop;

    private Context mContext;
    private Listener mListener;
    private NotificationHelper mNotificationHelper;
    private Selector mSelector = Selector.open();

    private NsdManager.RegistrationListener mRegistrationListener =
            new NsdManager.RegistrationListener() {
        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "service registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "service unregistered");
        }
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, String.format("registration failed: %d", errorCode));
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, String.format("unregistration failed: %d", errorCode));
        }
    };

    /**
     * Create a new transfer server
     * @param context context for retrieving string resources
     * @param notificationHelper notification manager
     * @param listener callback for new transfers
     */
    public TransferServer(Context context, NotificationHelper notificationHelper, Listener listener) throws IOException {
        mContext = context;
        mNotificationHelper = notificationHelper;
        mListener = listener;
    }

    /**
     * Start the server if it is not already running
     */
    public void start() {
        if (!mThread.isAlive()) {
            mStop = false;
            mThread.start();
        }
    }

    /**
     * Stop the transfer server if it is running and wait for it to finish
     */
    public void stop() {
        if (mThread.isAlive()) {
            mStop = true;
            mSelector.wakeup();
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    // TODO: this method could use some refactoring

    @Override
    public void run() {
        Log.i(TAG, "starting server...");

        // Inform the notification manager that the server has started
        mNotificationHelper.startListening();

        NsdManager nsdManager = null;

        try {
            // Create a server and attempt to bind to a port
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(40818));
            serverSocketChannel.configureBlocking(false);

            Log.i(TAG, String.format("server bound to port %d",
                    serverSocketChannel.socket().getLocalPort()));

            // Register the service
            nsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
            nsdManager.registerService(
                    new Device(
                            TransferHelper.deviceName(),
                            TransferHelper.deviceUUID(),
                            null,
                            40818
                    ).toServiceInfo(),
                    NsdManager.PROTOCOL_DNS_SD,
                    mRegistrationListener
            );

            // Register the server with the selector
            SelectionKey selectionKey = serverSocketChannel.register(mSelector,
                    SelectionKey.OP_ACCEPT);

            // Create Transfers as new connections come in
            while (true) {
                mSelector.select();
                if (mStop) {
                    break;
                }
                if (selectionKey.isAcceptable()) {
                    Log.i(TAG, "accepting incoming connection");
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    String unknownDeviceName = mContext.getString(
                            R.string.service_transfer_unknown_device);
                    mListener.onNewTransfer(
                            new Transfer(
                                    socketChannel,
                                    TransferHelper.transferDirectory(),
                                    TransferHelper.overwriteFiles(),
                                    unknownDeviceName
                            )
                    );
                }
            }

            // Close the server socket
            serverSocketChannel.close();

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        // Unregister the service
        if (nsdManager != null) {
            nsdManager.unregisterService(mRegistrationListener);
        }

        // Inform the notification manager that the server has stopped
        mNotificationHelper.stopListening();

        Log.i(TAG, "server stopped");
    }
}
