package dev.dworks.apps.anexplorer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_FAILEDTOSTART;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STARTED;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.ACTION_FTPSERVER_STOPPED;
import static dev.dworks.apps.anexplorer.misc.ConnectionUtils.getAvailablePortForFTP;


public class ConnectionsService extends Service {

    protected static final int MSG_START = 1;
    protected static final int MSG_STOP = 2;

    private static FtpServer ftpServer;
    private FTPServiceHandler FTPServiceHandler;
    private Looper serviceLooper;

    public ConnectionsService() {
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceLooper = thread.getLooper();
        FTPServiceHandler = new FTPServiceHandler(serviceLooper, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_REDELIVER_INTENT;
        }
        Bundle extras = intent.getExtras();
        Message msg = FTPServiceHandler.obtainMessage();
        msg.arg1 = MSG_START;
        FTPServiceHandler.sendMessage(msg);

        // we don't want the system to kill the ftp server
        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Message msg = FTPServiceHandler.obtainMessage();
        msg.arg1 = MSG_STOP;
        FTPServiceHandler.sendMessage(msg);
    }

    private final static class FTPServiceHandler extends Handler {

        private final WeakReference<ConnectionsService> ftpServiceRef;

        public FTPServiceHandler(Looper looper, ConnectionsService ftpService) {
            super(looper);
            this.ftpServiceRef = new WeakReference<>(ftpService);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectionsService ftpService = ftpServiceRef.get();
            if (ftpService == null) {
                return;
            }

            int toDo = msg.arg1;
            if (toDo == MSG_START) {
                if (ftpService.ftpServer == null) {
                    ftpService.launchFtpServer();
                    if (ftpService.ftpServer == null) {
                        ftpService.stopSelf();
                    }
                }

            } else if (toDo == MSG_STOP) {
                if (ftpService.ftpServer != null) {
                    ftpService.ftpServer.stop();
                    ftpService.ftpServer = null;
                }
                if (ftpService.ftpServer == null) {
                    ftpService.sendBroadcast(new Intent(ACTION_FTPSERVER_STOPPED));
                }
                ftpService.stopSelf();
            }
        }
    }

    protected void launchFtpServer() {
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(getAvailablePortForFTP());

        FtpServerFactory serverFactory = new FtpServerFactory();
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
        serverFactory.addListener("default", listenerFactory.createListener());

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        BaseUser user = new BaseUser();
        user.setName("anonymous");
        user.setHomeDirectory(path);
        List<Authority> list = new ArrayList<>();
        list.add(new WritePermission());
        user.setAuthorities(list);
        try {
            serverFactory.getUserManager().save(user);
        } catch (FtpException e) {
            e.printStackTrace();
        }

        // do start server
        try{
            ftpServer = serverFactory.createServer();
            ftpServer.start();
            sendBroadcast(new Intent(ACTION_FTPSERVER_STARTED));
        }catch(Exception e){
            ftpServer = null;
            sendBroadcast(new Intent(ACTION_FTPSERVER_FAILEDTOSTART));
        }
    }

    public static boolean isRunning() {
        return null != ftpServer && !ftpServer.isStopped();
    }
}
