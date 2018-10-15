package dev.dworks.apps.anexplorer.server;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.model.DocumentsContract;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.provider.MediaDocumentsProvider;
import dev.dworks.apps.anexplorer.provider.StorageProvider;

import static dev.dworks.apps.anexplorer.cast.CastUtils.MEDIA_THUMBNAILS;

public class WebServer extends SimpleWebServer {

    public static final int DEFAULT_PORT = 1212;

    private static WebServer server = null;
    private boolean isStarted;

    public static WebServer getServer() {
        if(server == null){
            RootInfo rootInfo = DocumentsApplication.getRootsCache().getPrimaryRoot();
            File root = null != rootInfo ? new File(rootInfo.path) : Environment.getExternalStorageDirectory();
            server = new WebServer(root);
        }
        return server;
    }

    public boolean startServer(Context context) {
        if (!isStarted) {
            try {
                if(ConnectionUtils.isConnectedToWifi(context)) {
                    server.start();
                    isStarted = true;
                }
                return isStarted;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean stopServer() {
        if (isStarted && server != null) {
            server.stop();
            isStarted = false;
            return true;
        }
        return false;
    }

    public WebServer(File root) {
        super(null, DEFAULT_PORT,
                Collections.singletonList(root),
                true, null);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();
        if(uri.contains(MEDIA_THUMBNAILS)){
            String docid = parms.get("docid");
            String authority = parms.get("authority");
            final Uri mediaUri = DocumentsContract.buildDocumentUri(authority, docid);

            if (mediaUri != null) {
                String mimeType = "image/jpg";
                InputStream inputStream = null;
                try {
                    Context context = DocumentsApplication.getInstance().getApplicationContext();
                    AssetFileDescriptor afd = DocumentsContract.getDocumentThumbnails(context.getContentResolver(), mediaUri);
                    if(null != afd) {
                        inputStream = afd.createInputStream();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return newChunkedResponse(Response.Status.OK, mimeType, inputStream);
            }
        }
        return super.serve(session);
    }
}
