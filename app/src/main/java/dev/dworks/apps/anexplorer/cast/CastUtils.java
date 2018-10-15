package dev.dworks.apps.anexplorer.cast;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.common.images.WebImage;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;
import dev.dworks.apps.anexplorer.server.WebServer;

public class CastUtils {

    public static final String MEDIA_THUMBNAILS = "mediathumbnails";

    public static MediaInfo buildMediaInfo(DocumentInfo doc, RootInfo rootInfo) {

        int mediaType = MediaMetadata.MEDIA_TYPE_GENERIC;
        final String typeOnly = doc.mimeType.split("/")[0];
        if ("audio".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_MUSIC_TRACK;
        } else if ("image".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_PHOTO;
        }else if ("video".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_MOVIE;
        }

        String thumbnailUrl = getIpAddress() + "/" + MEDIA_THUMBNAILS
                + "?docid=" + doc.documentId + "&authority=" + doc.authority;
        MediaMetadata metadata = new MediaMetadata(mediaType);

        metadata.putString(MediaMetadata.KEY_TITLE, doc.displayName);
        metadata.addImage(new WebImage(Uri.parse(thumbnailUrl)));
        String url  = getFileAddress(doc.path, rootInfo);

        return new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(doc.mimeType)
                .setMetadata(metadata)
                .build();
    }

    public static MediaQueueItem buildMediaQueueItem(DocumentInfo doc, RootInfo rootInfo){
        return new MediaQueueItem.Builder(buildMediaInfo(doc, rootInfo))
                .setAutoplay(true)
                .setPreloadTime(20)
                .build();
    }

    private static String getFileAddress(String path, RootInfo rootInfo){
        return getIpAddress() + path.replace(rootInfo.path, "");
    }

    private static String getIpAddress(){
        return ConnectionUtils.getIpAccess(DocumentsApplication.getInstance().getApplicationContext()) + WebServer.DEFAULT_PORT;
    }
}
