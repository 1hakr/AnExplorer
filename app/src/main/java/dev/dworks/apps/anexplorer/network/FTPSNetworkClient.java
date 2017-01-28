package dev.dworks.apps.anexplorer.network;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.io.InputStream;

import dev.dworks.apps.anexplorer.misc.CrashReportingManager;
import dev.dworks.apps.anexplorer.misc.LogUtils;

/**
 * Created by HaKr on 02/01/17.
 */

public class FTPSNetworkClient extends NetworkClient {

    private static final String TAG = FTPSNetworkClient.class.getSimpleName();

    FTPSClient client;
    public String host;
    public int port;
    public String username;
    public String password;

    public FTPSNetworkClient(final String host, final int port){
        client = new FTPSClient();
        this.host = host;
        this.port = port;
        this.username = "anonymous";
        this.password = "";
    }

    public FTPSNetworkClient(final String host, final int port, final String userName, final String password){
        client = new FTPSClient();
        this.host = host;
        this.port = port;
        this.username = userName;
        this.password = password;
    }

    @Override
    public boolean logout() throws IOException {
        return client.logout();
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable();
    }

    @Override
    public void disconnect() throws IOException {
        client.disconnect();
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        return client.login(username, password);
    }

    @Override
    public boolean login(String username, String password, String account) throws IOException {
        return client.login(username, password, account);
    }

    @Override
    public boolean connectClient() throws IOException {
        boolean isLoggedIn = true;
        client.setAutodetectUTF8(true);
        client.setControlEncoding("UTF-8");
        client.connect(host, port);
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.enterLocalPassiveMode();
        client.login(username, password);
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect();
            LogUtils.LOGD(TAG, "Negative reply form FTP server, aborting, id was {}:"+ reply);
            //throw new IOException("failed to connect to FTP server");
            isLoggedIn = false;
        }
        return isLoggedIn;
    }

    @Override
    public String getWorkingDirectory() throws IOException {
        return client.printWorkingDirectory();
    }

    @Override
    public void changeWorkingDirectory(String pathname) throws IOException {
        client.changeWorkingDirectory(pathname);
    }

    /**
     * Return input stream for given file name
     *
     * @param fileName  name of the remote file
     * @param directory remote directory
     * @return input stream or null
     */
    public InputStream getInputStream(final String fileName, final String directory) {

        try {
            connectClient();
            changeWorkingDirectory(directory);
            return client.retrieveFileStream(fileName);

        } catch (IOException e) {
            LogUtils.LOGE(TAG, "Error retrieving file from FTP server: " + host, e);
            CrashReportingManager.logException(e);
        }
        return null;
    }

    /**
     * Creates directories on FTP server
     *
     * @param directory directory to create
     * @return SimpleFtpClientResult.CREATED on succes SimpleFtpClientResult.ERROR on error
     * @throws IOException on error
     */
    public boolean createDirectories(final String directory) throws IOException {
        String[] iterable = directory.split("/");
        for (String dir : iterable) {
            boolean dirExists = client.changeWorkingDirectory(dir);
            if (!dirExists) {
                // try to create directory:
                client.makeDirectory(dir);
                dirExists = client.changeWorkingDirectory(dir);
            }
            if (!dirExists) {
                LogUtils.LOGD(TAG, "failed to change FTP directory (forms), not doing anything");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deleteFile(String path) throws IOException {
        return client.deleteFile(path);
    }

    @Override
    public FTPFile[] listFiles() throws IOException {
        return client.listFiles();
    }

    @Override
    public boolean completePendingCommand() throws IOException {
        return client.completePendingCommand();
    }
}
