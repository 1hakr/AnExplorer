package dev.dworks.apps.anexplorer.network;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import dev.dworks.apps.anexplorer.R;
import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.misc.LogUtils;
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
        LogUtils.LOGD(TAG, "handleMessage()");

        NetworkServerService service = serviceRef.get();
        if (service == null) {
            LogUtils.LOGD(TAG, "serviceRef is null");
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
            LogUtils.LOGD(TAG, "starting {} server");

            boolean started = service.launchServer();
            if (started && service.getServer() != null) {
                sendBroadcast(service, ACTION_FTPSERVER_STARTED);
                if(null == service.getRootInfo()) {
                    Context context = service.getApplicationContext();
                    String contentTitle = context.getString(R.string.ftp_notif_title)
                            + " \n " + String.format(context.getString(R.string.ftp_notif_text),
                            ConnectionUtils.getFTPAddress(context));
                    Toast.makeText(context, contentTitle, Toast.LENGTH_LONG).show();
                }
            } else {
                service.stopSelf();
            }

        }
    }

    protected void handleStop(NetworkServerService service) {
        if (service.getServer() != null) {
            LogUtils.LOGD(TAG, "stopping {} server");
            service.stopServer();
        }
        LogUtils.LOGD(TAG, "stopSelf ({})");
        service.stopSelf();
        sendBroadcast(service, ACTION_FTPSERVER_STOPPED);
    }

    private void sendBroadcast(NetworkServerService service, String action){
        Intent intent = new Intent(action);
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ROOT, service.getRootInfo());
        intent.putExtras(args);
        service.sendBroadcast(intent);
    }
}