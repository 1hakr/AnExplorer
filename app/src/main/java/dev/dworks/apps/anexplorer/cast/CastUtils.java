package dev.dworks.apps.anexplorer.cast;

import android.net.Uri;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;

import java.io.File;

import dev.dworks.apps.anexplorer.DocumentsApplication;
import dev.dworks.apps.anexplorer.misc.ConnectionUtils;
import dev.dworks.apps.anexplorer.model.DocumentInfo;
import dev.dworks.apps.anexplorer.model.RootInfo;

public class CastUtils {

    public static final String MEDIA_THUMBNAILS = "mediathumbnails";

    public static MediaInfo buildMediaInfo(DocumentInfo doc, RootInfo rootInfo) {

        int mediaType = getMediaType(doc.mimeType);

        String thumbnailUrl = getIpAddress() + "/" + MEDIA_THUMBNAILS
                + "?docid=" + doc.documentId + "&authority=" + doc.authority;
        MediaMetadata metadata = new MediaMetadata(mediaType);

        String folderName = new File(doc.path).getParentFile().getName();
        String url  = getFileAddress(doc.path, rootInfo);
        metadata.putString(MediaMetadata.KEY_TITLE, doc.displayName);
        metadata.putString(MediaMetadata.KEY_SUBTITLE, doc.path);
        metadata.putString(MediaMetadata.KEY_ALBUM_TITLE, folderName);
        metadata.addImage(new WebImage(Uri.parse(thumbnailUrl)));

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

    public static MediaQueueItem buildMediaQueueItem(MediaInfo mediaInfo){
        return new MediaQueueItem.Builder(mediaInfo)
                .setAutoplay(true)
                .setPreloadTime(20)
                .build();
    }

    private static String getFileAddress(String path, RootInfo rootInfo){
        return getIpAddress() + path.replace(rootInfo.path, "");
    }

    private static String getIpAddress(){
        return ConnectionUtils.getHTTPAccess(DocumentsApplication.getInstance().getApplicationContext());
    }

    public static void removeQueueItem(Casty casty, int itemId) {
        RemoteMediaClient client = casty.getRemoteMediaClient();
        if(null != client) {
            client.queueRemoveItem(itemId, null);
        }
    }

    public static void clearQueue(Casty casty) {
        RemoteMediaClient client = casty.getRemoteMediaClient();
        if(null != client) {
            casty.getMediaQueue().clear();
            client.stop();
        }
    }

    public static void playFromQueue(Casty casty, final int itemtId) {
        final RemoteMediaClient client = casty.getRemoteMediaClient();
        if(null != client) {
            client.queueJumpToItem(itemtId, null);
        }
    }

    public static void addToQueue(Casty casty, MediaInfo mediaInfo) {
        if(casty.getMediaQueue().getItemCount() == 0) {
            casty.getPlayer().loadMediaAndPlay(mediaInfo);
        } else {
            casty.getPlayer().loadMediaInQueueAndPlay(CastUtils.buildMediaQueueItem(mediaInfo));
        }
    }

    public static int getMediaType(String mimeType){
        int mediaType = MediaMetadata.MEDIA_TYPE_GENERIC;
        final String typeOnly = mimeType.split("/")[0];
        if ("audio".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_MUSIC_TRACK;
        } else if ("image".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_PHOTO;
        }else if ("video".equals(typeOnly)) {
            mediaType = MediaMetadata.MEDIA_TYPE_MOVIE;
        }
        return mediaType;
    }

    public static String getMimeType(int mediaType){
        String mimeType = "others/generic";
        if (mediaType == MediaMetadata.MEDIA_TYPE_MUSIC_TRACK) {
            mimeType = "audio/mp3";
        } else if (mediaType == MediaMetadata.MEDIA_TYPE_PHOTO) {
            mimeType = "image/jpg";
        }else if (mediaType == MediaMetadata.MEDIA_TYPE_MOVIE) {
            mimeType = "video/mp4";
        }
        return mimeType;
    }
}
