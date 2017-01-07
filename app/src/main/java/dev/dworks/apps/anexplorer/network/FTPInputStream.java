package dev.dworks.apps.anexplorer.network;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPInputStream extends FilterInputStream {
    public static final int BUFFER_SIZE = 32 * 1024;
    private NetworkClient client;

    public FTPInputStream(InputStream fd, NetworkClient client) {
        super(new BufferedInputStream(fd, BUFFER_SIZE));
        this.client = client;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (client != null) {
            client.completePendingCommand();
        }
    }
}