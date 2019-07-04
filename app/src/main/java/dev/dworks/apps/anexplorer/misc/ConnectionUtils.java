package dev.dworks.apps.anexplorer.misc;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import dev.dworks.apps.anexplorer.BuildConfig;
import dev.dworks.apps.anexplorer.fragment.CreateConnectionFragment;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;
import dev.dworks.apps.anexplorer.server.WebServer;
import dev.dworks.apps.anexplorer.service.ConnectionsService;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by HaKr on 05/09/16.
 */

public class ConnectionUtils {

    public static final String TAG = ConnectionUtils.class.getSimpleName();

    static public final String ACTION_FTPSERVER_STARTED = BuildConfig.APPLICATION_ID + ".action.FTPSERVER_STARTED";
    static public final String ACTION_FTPSERVER_STOPPED = BuildConfig.APPLICATION_ID + ".action.FTPSERVER_STOPPED";
    static public final String ACTION_FTPSERVER_FAILEDTOSTART = BuildConfig.APPLICATION_ID + ".action.FTPSERVER_FAILEDTOSTART";

    static public final String ACTION_START_FTPSERVER = BuildConfig.APPLICATION_ID + ".action.START_FTPSERVER";
    static public final String ACTION_STOP_FTPSERVER = BuildConfig.APPLICATION_ID + ".action.STOP_FTPSERVER";

    public static int FTP_SERVER_PORT = 2211;

    public static boolean isConnectedToLocalNetwork(Context context) {
        boolean connected = false;
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an WIFI");
            connected = isConnectedToWifi(context);
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an Ethernet");
            connected = isConnectedToEthernet(context);
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an WIFI AP");
            connected = isConnectedToWifiAP(context);
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an USB AP");
            connected = isConnectedToUSBAP(context);
        }
        if (!connected) {
            Log.d(TAG, "isConnectedToLocalNetwork: see if it is an Mobile");
            connected = isConnectedToMobile(context);
        }
        return connected;
    }

    public static boolean isConnectedToMobile(Context context) {
        return isConnectedToNetwork(context, ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isConnectedToWifi(Context context) {
        return isConnectedToNetwork(context, ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isConnectedToEthernet(Context context) {
        return isConnectedToNetwork(context, ConnectivityManager.TYPE_ETHERNET);
    }

    public static boolean isConnectedToNetwork(Context context, int type) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnectedOrConnecting()
                && (networkInfo.getType() == type);
        return connected;
    }

    public static boolean isConnectedToWifiAP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        boolean connected = false;
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            connected = (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return connected;
    }

    public static boolean isConnectedToUSBAP(Context context) {
        boolean connected = false;
        try {
            for (NetworkInterface netInterface : Collections.list(NetworkInterface
                    .getNetworkInterfaces())) {
                if (netInterface.getDisplayName().startsWith("rndis")) {
                    connected = true;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return connected;
    }

    public static InetAddress getLocalInetAddress(Context context) {
        if (!isConnectedToLocalNetwork(context)) {
            Log.e(TAG, "getLocalInetAddress called and no connection");
            return null;
        }

        if (isConnectedToWifi(context)) {
            WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            if (ipAddress == 0)
                return null;
            return intToInet(ipAddress);
        }

        try {
            Enumeration<NetworkInterface> netinterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (netinterfaces.hasMoreElements()) {
                NetworkInterface netinterface = netinterfaces.nextElement();
                Enumeration<InetAddress> adresses = netinterface.getInetAddresses();
                while (adresses.hasMoreElements()) {
                    InetAddress address = adresses.nextElement();
                    // this is the condition that sometimes gives problems
                    if (!address.isLoopbackAddress()
                            && !address.isLinkLocalAddress())
                        return address;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InetAddress intToInet(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = byteOfInt(value, i);
        }
        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            // This only happens if the byte array has a bad length
            return null;
        }
    }

    public static byte byteOfInt(int value, int which) {
        int shift = which * 8;
        return (byte) (value >> shift);
    }

    public static boolean isPortAvailable(int port) {

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }

    public static String getFTPAddress(Context context){
        return getIpAccess(context, true, FTP_SERVER_PORT);
    }

    public static String getHTTPAccess(Context context) {
        return getIpAccess(context, false, WebServer.DEFAULT_PORT);
    }

    public static String getIpAccess(Context context, boolean ftp, int port) {
        InetAddress inetAddress = getLocalInetAddress(context);
        String scheme = ftp ? "ftp://" : "http://";
        if(null != inetAddress) {
            return scheme + inetAddress.getHostAddress() + ":" + port;
        }
        return "";
    }

    public static int getAvailablePortForFTP(){
        int port = 0;
        for(int i = FTP_SERVER_PORT ;i<65000;i++){
            if(isPortAvailable(i)) {
                port = i;
                break;
            }
        }
        return port;
    }

    public static boolean isServerRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        String ftpServiceClassName = ConnectionsService.class.getName();
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            String currentClassName = service.service.getClassName();
            if (ftpServiceClassName.equals(currentClassName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServerAuthority(Intent intent){
        if(null != intent.getData()){
            String authority = intent.getData().getAuthority();
            return NetworkStorageProvider.AUTHORITY.equals(authority);
        }
        return false;
    }


    public static void addConnection(AppCompatActivity activity) {
        CreateConnectionFragment.show(activity.getSupportFragmentManager());
        AnalyticsManager.logEvent("connection_add");
    }

    public static void editConnection(AppCompatActivity activity, int connection_id) {
        CreateConnectionFragment.show(activity.getSupportFragmentManager(), connection_id);
        AnalyticsManager.logEvent("connection_edit");
    }

}
