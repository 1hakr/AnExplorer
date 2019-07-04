package dev.dworks.apps.anexplorer.network;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import dev.dworks.apps.anexplorer.libcore.io.IoUtils;
import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.LogUtils;
import dev.dworks.apps.anexplorer.model.Durable;
import dev.dworks.apps.anexplorer.model.DurableUtils;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.ExplorerProvider;
import dev.dworks.apps.anexplorer.provider.ExplorerProvider.ConnectionColumns;
import dev.dworks.apps.anexplorer.provider.NetworkStorageProvider;

import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorBolean;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorInt;
import static dev.dworks.apps.anexplorer.model.DocumentInfo.getCursorString;

/**
 * Created by HaKr on 31/12/16.
 */

public class NetworkConnection  implements Durable, Parcelable {

    private static final String TAG = NetworkConnection.class.getSimpleName();
    private static final String ROOT = "/";
    public static final String SERVER = "server";
    public static final String CLIENT = "client";

    public int id;
    public String name;
    public String scheme;
    public String type;
    public String host;
    public int port;
    public String username;
    public String password;
    public String path;
    public NetworkFile file;
    public boolean isAnonymousLogin = false;
    private boolean isLoggedIn;
    public NetworkClient client;

    public NetworkConnection() {
        reset();
    }

    /**
     * Constructor ff port is different than port 21
     *
     * @param host   server address (name or ip address))
     * @param port     port number
     */
    public NetworkConnection(final String scheme, final String host, final int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        setAnonymous(true);
        setDefaults();
    }

    /**
     * Constructor ff port is different than port 21
     *
     * @param userName login name
     * @param password login password
     * @param host   server address (name or ip address))
     * @param port     port number
     */
    public NetworkConnection(final String scheme, final String host, final int port,
                             final String userName, final String password) {
        this.scheme = scheme;
        this.username = userName;
        this.password = password;
        this.host = host;
        this.port = port;
        setDefaults();
    }

    private void setDefaults() {
        this.client =  NetworkClient.create(scheme, host, port, username, password);
        this.path = ROOT;
        this.file = new NetworkFile(path, host);
    }

    public void setAnonymous(boolean anonymous) {
        this.isAnonymousLogin = anonymous;
        if(anonymous) {
            this.username = "anonymous";
            this.password = "";
        }
    }


    public void build() {
        setDefaults();
    }

    public String getName(){
        return name;
    }

    public String getSummary(){
        return type.compareToIgnoreCase(SERVER) == 0 ? path : scheme + "://" + host + ":" + port;
    }

    /**
     * Return input stream for given file name
     *
     * @param file  newtork file
     * @return input stream or null
     */
    public InputStream getInputStream(final NetworkFile file) {
        return getClient().getInputStream(file.getName(), file.getParentFile().getAbsolutePath());
    }

    /**
     * Changes working director
     *
     * @param directory directory (relative to user home dir)
     * @throws IOException on error
     */
    private void changeToDir(final String directory) throws IOException {
        String[] iterable = directory.split("/");
        // note: important: we first go to user home
        client.changeWorkingDirectory(getHomeDirectory());
        for (String dir : iterable) {
            client.changeWorkingDirectory(dir);
        }
    }
    public void changeWorkingDirectory(final String directory) throws IOException {
        getClient().changeWorkingDirectory(directory);
    }

    /**
     * Connects client if not connected yet. must be called before we do any actions
     *
     * @throws IOException on error
     */
    public final boolean connectClient() throws IOException {
        if (!client.isConnected()) {
            isLoggedIn = getClient().connectClient();
            path = getClient().getWorkingDirectory();
            file = new NetworkFile(path, getHost());
        }
        return isLoggedIn;
    }

    public final boolean isLoggedIn(){
        return isLoggedIn;
    }

    private boolean createDirectories(final String directory) throws IOException {
        return getClient().createDirectories(directory);
    }

    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isAnonymousLogin(){
        return isAnonymousLogin;
    }

    public String getScheme() {
        return scheme;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public void close() throws IOException {
        client.logout();
        client.disconnect();
    }

    public NetworkClient getClient() {
        return client;
    }

    public NetworkClient getConnectedClient() throws IOException {
        if (!isLoggedIn()) {
            connectClient();
        }
        return client;
    }

    public String getHomeDirectory() {
        if (path == null) {
            try {
                connectClient();
            } catch (IOException e) {
                LogUtils.LOGD(TAG, "Error getting home dir:"+ e);
                CrashReportingManager.logException(e);
            }
        }
        return path;
    }

    /**
     * Convert the path to a URI for the return intent
     *
     * @return a Uri
     */
    public Uri toUri(@NonNull NetworkFile path) {
        String user = "";
        if (!username.isEmpty()) {
            user = username;
            if (!password.isEmpty()) {
                user += ":" + password;
            }
            user += "@";
        }
        return Uri.parse(scheme + "://" + user + host + ":" + port + path.getPath());
    }

    public Uri toUri(@NonNull String path) {
        String user = "";
        if (!username.isEmpty()) {
            user = username;
            if (!password.isEmpty()) {
                user += ":" + password;
            }
            user += "@";
        }
        return Uri.parse(host + path);
    }

    public static NetworkConnection fromConnectionsCursor(Cursor cursor){
        String scheme = getCursorString(cursor, ConnectionColumns.SCHEME);
        String host = getCursorString(cursor, ConnectionColumns.HOST);
        int port = getCursorInt(cursor, ConnectionColumns.PORT);
        String username = getCursorString(cursor, ConnectionColumns.USERNAME);
        String password = getCursorString(cursor, ConnectionColumns.PASSWORD);

        NetworkConnection networkConnection
                = NetworkConnection.create(scheme, host, port, username, password);
        networkConnection.id = getCursorInt(cursor, BaseColumns._ID);
        networkConnection.name = getCursorString(cursor, ConnectionColumns.NAME);
        networkConnection.type = getCursorString(cursor, ConnectionColumns.TYPE);
        networkConnection.path = getCursorString(cursor, ConnectionColumns.PATH);
        networkConnection.setAnonymous(getCursorBolean(cursor, ConnectionColumns.ANONYMOUS_LOGIN));
        return networkConnection;
    }

    public static NetworkConnection fromRootInfo(Context context, RootInfo root) {
        Cursor cursor = null;
        NetworkConnection networkConnection = null;
        try {
            cursor = context.getContentResolver()
                    .query(ExplorerProvider.buildConnection(), null,
                            ConnectionColumns.HOST + "=? AND "
                                    + ConnectionColumns.PATH + "=? "
                            , new String[]{root.rootId, root.path}, null);
            if (null != cursor && cursor.moveToFirst()) {
                networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + NetworkStorageProvider.AUTHORITY + ": " + e);
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        return networkConnection;
    }

    public static NetworkConnection fromConnectionId(Context context, int id) {
        Cursor cursor = null;
        NetworkConnection networkConnection = null;
        if(id == 0) {
            networkConnection = new NetworkConnection();
            return networkConnection;
        }
        try {
            cursor = context.getContentResolver()
                    .query(ExplorerProvider.buildConnection(), null,
                            BaseColumns._ID + "=? "
                            , new String[]{Integer.toString(id)}, null);
            if (null != cursor && cursor.moveToFirst()) {
                networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + NetworkStorageProvider.AUTHORITY + ": " + e);
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        return networkConnection;
    }

    public static NetworkConnection getDefaultServer(Context context) {
        Cursor cursor = null;
        NetworkConnection networkConnection = null;
        try {
            cursor = context.getContentResolver()
                    .query(ExplorerProvider.buildConnection(), null,
                            ConnectionColumns.TYPE + "=? "
                            , new String[]{SERVER}, null);
            if (null != cursor && cursor.moveToFirst()) {
                networkConnection = NetworkConnection.fromConnectionsCursor(cursor);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + NetworkStorageProvider.AUTHORITY + ": " + e);
            CrashReportingManager.logException(e);
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        return networkConnection;
    }

    public static boolean deleteConnection(Context context, int id) {
        try {
            int resultId = context.getContentResolver()
                    .delete(ExplorerProvider.buildConnection(),
                            BaseColumns._ID + "=? "
                            , new String[]{Integer.toString(id)});
            if (0 != resultId) {
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load some roots from " + NetworkStorageProvider.AUTHORITY + ": " + e);
        }

        return false;
    }

    public static NetworkConnection create(String scheme, String host, int port, String userName, String password) {
        return new NetworkConnection(scheme, host, port, userName, password);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NetworkConnection");
        sb.append("{userName='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        DurableUtils.writeToParcel(dest, this);
    }

    public static final Creator<NetworkConnection> CREATOR = new Creator<NetworkConnection>() {
        @Override
        public NetworkConnection createFromParcel(Parcel in) {
            final NetworkConnection networkConnection = new NetworkConnection();
            DurableUtils.readFromParcel(in, networkConnection);
            return networkConnection;
        }

        @Override
        public NetworkConnection[] newArray(int size) {
            return new NetworkConnection[size];
        }
    };

    @Override
    public void reset() {
        name = null;
        scheme = null;
        type = null;
        host = null;
        port = 0;
        username = null;
        password = null;
        path = null;
        file = null;
        isAnonymousLogin = false;
        isLoggedIn = false;
        client = null;
    }

    @Override
    public void read(DataInputStream in) throws IOException {

    }

    @Override
    public void write(DataOutputStream out) throws IOException {

    }
}