package dev.dworks.apps.anexplorer.network;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.service.NetworkServerService;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STARTED;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STOPPED;
import static dev.dworks.apps.anexplorer.misc.Utils.EXTRA_ROOT;

public class NetworkServiceHandler extends Handler {

    private static final String TAG = NetworkServiceHandler.class.getSimpleName();

    private final WeakReference<NetworkServerService> serviceRef;

    public NetworkServiceHandler(Looper looper, NetworkServerService service) {
        super(looper);
        this.serviceRef = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
        NetworkServerService service = serviceRef.get();
        if (service == null) {
            return;
        }

        int toDo = msg.arg1;
        if (toDo == NetworkServerService.MSG_START) {
            handleStart(service);

        } else if (toDo == NetworkServerService.MSG_STOP) {
            handleStop(service);
        }
    }

    protected void handleStart(NetworkServerService service) {
        if (service.getServer() == null) {

            boolean started = service.launchServer();
            if (started && service.getServer() != null) {
                sendBroadcast(service, ACTION_FTPSERVER_STARTED);
            } else {
                service.stopSelf();
            }

        }
    }

    protected void handleStop(NetworkServerService service) {
        if (service.getServer() != null) {
            service.stopServer();
        }
        service.stopSelf();
        sendBroadcast(service, ACTION_FTPSERVER_STOPPED);
    }

    private void sendBroadcast(NetworkServerService service, String action){
        Intent intent = new Intent(action);
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ROOT, service.getRootInfo());
        intent.setPackage(BuildConfig.APPLICATION_ID);
        intent.putExtras(args);
        service.sendBroadcast(intent);
    }
}