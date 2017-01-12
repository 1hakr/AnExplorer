package dev.dworks.apps.anexplorer.service;

import android.os.Looper;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.List;

import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.network.NetworkServiceHandler;


public class ConnectionsService extends NetworkServerService {

    private FtpServer ftpServer;

    @Override
    protected NetworkServiceHandler createServiceHandler(Looper serviceLooper, NetworkServerService service) {
        return new NetworkServiceHandler(serviceLooper, service);
    }

    @Override
    public Object getServer() {
        return ftpServer;
    }

    @Override
    public boolean launchServer() {
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(ConnectionUtils.getAvailablePortForFTP());

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.addListener("default", listenerFactory.createListener());

        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(getNetworkConnection().isAnonymousLogin());
        connectionConfigFactory.setMaxLoginFailures(5);
        connectionConfigFactory.setLoginFailureDelay(2000);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        BaseUser user = new BaseUser();
        user.setName(getNetworkConnection().getUserName());
        user.setPassword(getNetworkConnection().getPassword());
        user.setHomeDirectory(getNetworkConnection().getPath());

        List<Authority> list = new ArrayList<>();
        list.add(new WritePermission());
        list.add(new TransferRatePermission(0, 0));
        list.add(new ConcurrentLoginPermission(10, 10));
        user.setAuthorities(list);

        try {
            serverFactory.getUserManager().save(user);
        } catch (FtpException e) {
            CrashReportingManager.logException(e);
        }

        // do start server
        try{
            ftpServer = serverFactory.createServer();
            ftpServer.start();
            return true;
        }catch(Exception e){
            ftpServer = null;
            handleServerStartError(e);
        }
        return false;
    }

    @Override
    public void stopServer() {
        ftpServer.stop();
        ftpServer = null;
    }
}
